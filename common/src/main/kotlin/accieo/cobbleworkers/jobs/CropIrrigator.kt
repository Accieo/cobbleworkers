/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import accieo.cobbleworkers.config.CobbleworkersConfigHolder
import accieo.cobbleworkers.enums.JobType
import accieo.cobbleworkers.interfaces.Worker
import accieo.cobbleworkers.utilities.CobbleworkersCropUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.FarmlandBlock
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

/**
 * A worker job for a Pokémon to find, navigate to, and irrigate dry farmland.
 */
object CropIrrigator : Worker {
    private val config = CobbleworkersConfigHolder.config.irrigation

    override val jobType: JobType = JobType.CropIrrigator
    override val blockValidator: ((World, BlockPos) -> Boolean) = { world: World, pos: BlockPos ->
        val state = world.getBlockState(pos)
        state.block == Blocks.FARMLAND
    }

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
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestFarmland = CobbleworkersCropUtils.findClosestFarmland(world, origin) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null || currentTarget != closestFarmland) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestFarmland, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestFarmland, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestFarmland, world)
            }
            return
        }

        if (currentTarget == closestFarmland) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestFarmland)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget, 1.5)) {
            irrigateFarmland(world, currentTarget, config.irrigationRadius)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Handle irrigation processing, including finding neighboring farmland to
     * irrigate in a wider radius.
     */
    private fun irrigateFarmland(world: World, blockPos: BlockPos, radius: Int = 1) {
        val farmland = world.getBlockState(blockPos)
        val blocks = BlockPos.iterate(
            blockPos.add(-radius, 0, -radius),
            blockPos.add(radius, 0, radius)
        )

        blocks.forEach { it ->
            val blockState = world.getBlockState(it)
            if (blockState.block == Blocks.FARMLAND) {
                world.setBlockState(
                    it,
                    farmland.with(FarmlandBlock.MOISTURE, FarmlandBlock.MAX_MOISTURE),
                    Block.NOTIFY_LISTENERS
                )
            }
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