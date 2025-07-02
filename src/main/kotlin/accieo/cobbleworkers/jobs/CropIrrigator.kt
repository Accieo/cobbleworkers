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
import accieo.cobbleworkers.utilities.CobbleworkersCropUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.block.FarmlandBlock
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * A worker job for a Pokémon to find, navigate to, and irrigate dry farmland.
 */
object CropIrrigator : Worker {
    private const val SEARCH_RADIUS = 8
    private val config = CobbleworkersConfigHolder.config.irrigation

    /**
     * Determines if Pokémon is eligible to be a crop irrigator.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.cropIrrigatorsEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeIrrigatesCrops, pokemonEntity) || isDesignatedIrrigator(pokemonEntity)
    }

    /**
     * Main logic loop for the crop irrigator, executed each tick.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.uuid
        val closestFarmland = CobbleworkersCropUtils.findClosestFarmland(world, origin, SEARCH_RADIUS) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId)

        if (currentTarget == null || currentTarget != closestFarmland) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestFarmland)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestFarmland)
            }
            return
        }

        if (currentTarget == closestFarmland) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestFarmland)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget, 1.5)) {
            val farmland = world.getBlockState(currentTarget)
            world.setBlockState(
               currentTarget,
               farmland.with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE),
               Block.NOTIFY_LISTENERS
            )
            CobbleworkersNavigationUtils.releaseTarget(pokemonId)
        }
    }

    /**
     * Checks if the Pokémon qualifies as an irrigator because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedIrrigator(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.cropIrrigators.any { it.lowercase() == speciesName }
    }
}