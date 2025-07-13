/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import accieo.cobbleworkers.config.CobbleworkersConfigHolder
import accieo.cobbleworkers.interfaces.Worker
import accieo.cobbleworkers.mixin.BrewingStandBlockEntityAccessor
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.block.BrewingStandBlock
import net.minecraft.block.entity.BrewingStandBlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

object BrewingStandFuelGenerator : Worker {
    private val config = CobbleworkersConfigHolder.config.brewingStandFuel
    private val cooldownTicks get() = config.fuelGenerationCooldownSeconds * 20L
    private val searchRadius get() = config.searchRadius
    private val searchHeight get() = config.searchHeight
    private val lastGenerationTime = mutableMapOf<UUID, Long>()

    /**
     * Determines if Pokémon is eligible to be a fuel generator.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.fuelGeneratorsEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeGeneratesFuel, pokemonEntity) || isDesignatedGenerator(pokemonEntity)
    }

    /**
     * Main logic loop for the fuel generator, executed each tick.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        handleFuelGeneration(world, origin, pokemonEntity)
    }

    /**
     * Finds closest brewing stand nearby.
     */
    private fun findClosestBrewingStand(world: World, origin: BlockPos, pokemonEntity: PokemonEntity): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        val searchArea = Box(origin).expand(searchRadius.toDouble(), searchHeight.toDouble(), searchRadius.toDouble())

        BlockPos.stream(searchArea).forEach { pos ->
            val state = world.getBlockState(pos)
            val blockEntity = world.getBlockEntity(pos)
            if (state.block is BrewingStandBlock && blockEntity is BrewingStandBlockEntity && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)) {
                val accessor = blockEntity as BrewingStandBlockEntityAccessor
                if (accessor.fuel < BrewingStandBlockEntity.MAX_FUEL_USES) {
                    val distanceSq = pos.getSquaredDistance(pokemonEntity.pos)
                    if (distanceSq < closestDistance) {
                        closestDistance = distanceSq
                        closestPos = pos.toImmutable()
                    }
                }
            }
        }

        return closestPos
    }

    /**
     * Handles logic for finding a brewing stand and adding fuel.
     */
    private fun handleFuelGeneration(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestFurnace = findClosestBrewingStand(world, origin, pokemonEntity) ?: return

        val now = world.time
        val lastTime = lastGenerationTime[pokemonId] ?: 0L

        if (now - lastTime < cooldownTicks) {
            return
        }

        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestFurnace, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestFurnace, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestFurnace, world)
            }
            return
        }

        if (currentTarget == closestFurnace) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestFurnace)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, closestFurnace)) {
            addBurnTime(world, closestFurnace)
            lastGenerationTime[pokemonId] = now
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Adds burn time to a brewing stand.
     */
    private fun addBurnTime(world: World, standPos: BlockPos) {
        val blockEntity = world.getBlockEntity(standPos)
        if (blockEntity !is BrewingStandBlockEntity) return
        val accessor = blockEntity as BrewingStandBlockEntityAccessor
        val addedFuel = (accessor.fuel + config.addedFuel).coerceAtMost(BrewingStandBlockEntity.MAX_FUEL_USES)
        accessor.setFuel(addedFuel)
        blockEntity.markDirty()
        world.updateListeners(standPos, world.getBlockState(standPos), world.getBlockState(standPos), Block.NOTIFY_ALL)
    }

    /**
     * Checks if the Pokémon qualifies as a fuel generator because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedGenerator(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.fuelGenerators.any { it.lowercase() == speciesName }
    }
}