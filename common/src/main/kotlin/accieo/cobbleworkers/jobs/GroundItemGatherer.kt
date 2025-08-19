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
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.entity.ItemEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

object GroundItemGatherer : Worker {
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val config = CobbleworkersConfigHolder.config.groundItemGathering
    private val generalConfig = CobbleworkersConfigHolder.config.general
    private val searchRadius get() = generalConfig.searchRadius
    private val searchHeight get() = generalConfig.searchHeight

    override val jobType: JobType = JobType.GroundItemGatherer
    override val blockValidator: ((World, BlockPos) -> Boolean)? = null

    /**
     * Determines if Pokémon is eligible to be a worker.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.groundItemGatheringEnabled) return false

        return  CobbleworkersTypeUtils.isAllowedByType(config.typeGathersGroundItems, pokemonEntity) || isDesignatedGatherer(pokemonEntity)
    }

    /**
     * Main logic loop for the worker, executed each tick.
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val heldItems = heldItemsByPokemon[pokemonId]

        if (heldItems.isNullOrEmpty()) {
            failedDepositLocations.remove(pokemonId)
            handleGathering(world, origin, pokemonEntity)
        } else {
            CobbleworkersInventoryUtils.handleDepositing(world, origin, pokemonEntity, heldItems, failedDepositLocations, heldItemsByPokemon)
        }
    }

    /**
     * Handles logic for finding and gathering an item on the ground.
     */
    private fun handleGathering(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val (closestItemPos, closestItem) = findClosestItem(world, origin) ?: return

        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestItemPos, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestItemPos, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestItemPos, world)
            }
            return
        }

        if (currentTarget == closestItemPos) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestItemPos)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            val stack = closestItem.stack.copy()
            closestItem.discard()
            heldItemsByPokemon[pokemonId] = listOf(stack)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Finds the closest item on the ground and returns its position and entity.
     */
    private fun findClosestItem(world: World, origin: BlockPos): Pair<BlockPos, ItemEntity>? {
        val searchArea = Box(origin).expand(searchRadius.toDouble(), searchHeight.toDouble(), searchRadius.toDouble())
        val items = world.getEntitiesByClass(ItemEntity::class.java, searchArea) { true }
        return items.find { item -> item.isOnGround }?.let { it.blockPos to it }
    }

    /**
     * Checks if the Pokémon qualifies as a gatherer because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedGatherer(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.groundItemGatherers.any { it.lowercase() == speciesName }
    }
}