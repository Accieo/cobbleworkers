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
import net.minecraft.block.Blocks
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.text.lowercase

object FireExtinguisher : Worker {
    private val config = CobbleworkersConfigHolder.config.extinguisher

    override val jobType: JobType = JobType.FireExtinguisher
    override val blockValidator: ((World, BlockPos) -> Boolean) = {  world: World, pos: BlockPos ->
        val state = world.getBlockState(pos)
        state.block == Blocks.FIRE
    }

    /**
     * Determines if Pokémon is eligible to be a worker.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.extinguishersEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeExtinguishesFire, pokemonEntity) || isDesignatedExtinguisher(pokemonEntity)
    }

    /**
     * Main logic loop for the worker, executed each tick.
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        handleFireExtinguishing(world, origin, pokemonEntity)
    }

    /**
     * Handles the logic of finding and extinguishing fire.
     */
    private fun handleFireExtinguishing(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestFire = findClosestFire(world, origin) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestFire, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestFire, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestFire, world)
            }
            return
        }

        if (currentTarget == closestFire) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestFire)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            world.setBlockState(closestFire, Blocks.AIR.defaultState)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Finds the closest fire block.
     */
    private fun findClosestFire(world: World, origin: BlockPos): BlockPos? {
        val possibleTargets = CobbleworkersCacheManager.getTargets(origin, jobType)
        if (possibleTargets.isEmpty()) return null

        return possibleTargets
            .filter { pos ->
                blockValidator(world, pos) && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
            }
            .minByOrNull { it.getSquaredDistance(origin) }
    }

    /**
     * Checks if the Pokémon qualifies as an extinguisher because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedExtinguisher(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.extinguishers.any { it.lowercase() == speciesName }
    }
}