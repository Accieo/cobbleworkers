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
import accieo.cobbleworkers.utilities.CobbleworkersCauldronUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

object WaterGenerator : Worker {
    private val config = CobbleworkersConfigHolder.config.water
    private val searchRadius get() = config.searchRadius
    private val searchHeight get() = config.searchHeight
    private val cooldownTicks get() = config.waterGenerationCooldownSeconds * 20L
    private val lastGenerationTime = mutableMapOf<UUID, Long>()

    /**
     * Determines if Pokémon is eligible to be a worker.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.waterGeneratorsEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeGeneratesWater, pokemonEntity) || isDesignatedGenerator(pokemonEntity)
    }

    /**
     * Main logic loop for the worker, executed each tick.
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        handleGeneration(world, origin, pokemonEntity)
    }

    /**
     * Handles logic for finding a cauldron and generating water.
     */
    fun handleGeneration(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val now = world.time
        val lastTime = lastGenerationTime[pokemonId] ?: 0L

        if (now - lastTime < cooldownTicks) return

        val closestCauldron = CobbleworkersCauldronUtils.findClosestCauldron(world, origin, searchRadius, searchHeight) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestCauldron, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestCauldron, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestCauldron, world)
            }
            return
        }

        if (currentTarget == closestCauldron) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestCauldron.down())
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            CobbleworkersCauldronUtils.addFluid(world, closestCauldron, CobbleworkersCauldronUtils.CauldronFluid.WATER)
            lastGenerationTime[pokemonId] = now
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Checks if the Pokémon qualifies as a generator because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedGenerator(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.waterGenerators.any { it.lowercase() == speciesName }
    }
}