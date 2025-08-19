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
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

/**
 * A worker job for a Pokémon to find, navigate to, and harvest fully grown crops.
 * Harvested items are deposited into the nearest available inventory.
 */
object CropHarvester : Worker {
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val config = CobbleworkersConfigHolder.config.cropHarvest

    override val jobType: JobType = JobType.CropHarvester
    override val blockValidator: ((World, BlockPos) -> Boolean) = { world: World, pos: BlockPos ->
        val state = world.getBlockState(pos)
        state.block in CobbleworkersCropUtils.validCropBlocks
    }

    /**
     * Determines if Pokémon is eligible to be a crop harvester.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.cropHarvestersEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeHarvestsCrops, pokemonEntity) || isDesignatedHarvester(pokemonEntity)
    }

    /**
     * Main logic loop for the crop harvester, executed each tick.
     * Delegates to state handlers handleHarvesting and handleDepositing
     * to manage the current task of the Pokémon.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val heldItems = heldItemsByPokemon[pokemonId]

        if (heldItems.isNullOrEmpty()) {
            failedDepositLocations.remove(pokemonId)
            handleHarvesting(world, origin, pokemonEntity)
        } else {
            CobbleworkersInventoryUtils.handleDepositing(world, origin, pokemonEntity, heldItems, failedDepositLocations, heldItemsByPokemon)
        }
    }

    /**
     * Handles logic for finding and harvesting an apricorn when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestCrop = CobbleworkersCropUtils.findClosestCrop(world, origin) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestCrop, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestCrop, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestCrop, world)
            }
            return
        }

        if (currentTarget == closestCrop) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestCrop)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            CobbleworkersCropUtils.harvestCrop(world, closestCrop, pokemonEntity, heldItemsByPokemon, config)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedHarvester(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.cropHarvesters.any { it.lowercase() == speciesName }
    }
}