/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import accieo.cobbleworkers.cache.CobbleworkersCacheManager
import accieo.cobbleworkers.config.CobbleworkersConfigHolder
import accieo.cobbleworkers.enums.JobType
import accieo.cobbleworkers.interfaces.Worker
import accieo.cobbleworkers.mixin.BrewingStandBlockEntityAccessor
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.block.BrewingStandBlock
import net.minecraft.block.entity.BrewingStandBlockEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

object BrewingStandFuelGenerator : Worker {
    private val config = CobbleworkersConfigHolder.config.brewingStandFuel
    private val cooldownTicks get() = config.fuelGenerationCooldownSeconds * 20L
    private val lastGenerationTime = mutableMapOf<UUID, Long>()

    override val jobType: JobType = JobType.BrewingStandFuelGenerator
    override val blockValidator: ((World, BlockPos) -> Boolean) = { world: World, pos: BlockPos ->
        val state = world.getBlockState(pos)
        val blockEntity = world.getBlockEntity(pos)
        state.block is BrewingStandBlock && blockEntity is BrewingStandBlockEntity
    }

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
    private fun findClosestBrewingStand(world: World, origin: BlockPos): BlockPos? {
        val possibleTargets = CobbleworkersCacheManager.getTargets(origin, jobType)
        if (possibleTargets.isEmpty()) return null

        return possibleTargets
            .filter { pos ->
                val blockEntity = world.getBlockEntity(pos)
                blockValidator(world, pos)
                        && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
                        && (blockEntity as BrewingStandBlockEntityAccessor).fuel < BrewingStandBlockEntity.MAX_FUEL_USES
            }
            .minByOrNull { it.getSquaredDistance(origin) }
    }

    /**
     * Handles logic for finding a brewing stand and adding fuel.
     */
    private fun handleFuelGeneration(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestBrewingStand = findClosestBrewingStand(world, origin) ?: return

        val now = world.time
        val lastTime = lastGenerationTime[pokemonId] ?: 0L

        if (now - lastTime < cooldownTicks) {
            return
        }

        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestBrewingStand, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestBrewingStand, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestBrewingStand, world)
            }
            return
        }

        if (currentTarget == closestBrewingStand) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestBrewingStand)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, closestBrewingStand)) {
            addBurnTime(world, closestBrewingStand)
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