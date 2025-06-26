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
import com.cobblemon.mod.common.api.types.ElementalTypes
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

    /**
     * Determines if Pokémon is eligible to be a crop irrigator.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        if (!config.cropIrrigatorsEnabled) return false

        return isAllowedByWaterType(pokemonEntity) || isDesignatedIrrigator(pokemonEntity)
    }

    /**
     * Main logic loop for the crop irrigator, executed each tick.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val closestFarmland = CobbleworkersCropUtils.findClosestFarmland(world, origin, SEARCH_RADIUS)
        if (closestFarmland == null) return

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, closestFarmland)) {
            val farmland = world.getBlockState(closestFarmland)
            world.setBlockState(closestFarmland, farmland.with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE), Block.NOTIFY_ALL)
        } else {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestFarmland)
        }
    }

    /**
     * Checks if the Pokémon qualifies as an irrigator because it's a water type
     * and water type irrigator is enabled via config.
     */
    private fun isAllowedByWaterType(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        return config.waterTypeIrrigateCrops && pokemonEntity.pokemon.types.any { it == ElementalTypes.WATER }
    }

    /**
     * Checks if the Pokémon qualifies as an irrigator because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedIrrigator(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.cropIrrigators.any { it.lowercase() == speciesName }
    }
}