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
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.entity.ItemEntity
import net.minecraft.inventory.Inventory
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
    private val searchRadius get() = config.searchRadius
    private val searchHeight get() = config.searchHeight

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
        val heldItems = heldItemsByPokemon[pokemonEntity.uuid]

        if (heldItems.isNullOrEmpty()) {
            failedDepositLocations.remove(pokemonEntity.uuid)
            handleGathering(world, origin, pokemonEntity)
        } else {
            handleDepositing(world, origin, pokemonEntity, heldItems)
        }
    }

    /**
     * Handles logic for finding and depositing items into an inventory when the Pokémon is holding items.
     * It will try multiple inventories nearby iteratively
     */
    private fun handleDepositing(world: World, origin: BlockPos, pokemonEntity: PokemonEntity, itemsToDeposit: List<ItemStack>) {
        val triedPositions = failedDepositLocations.getOrPut(pokemonEntity.uuid) { mutableSetOf() }
        val inventoryPos = CobbleworkersInventoryUtils.findClosestInventory(world, origin,
            searchRadius,
            searchHeight, triedPositions)

        if (inventoryPos == null) {
            // No (untried) inventories found, so we just drop the remaining items and reset.
            itemsToDeposit.forEach { stack -> Block.dropStack(world, pokemonEntity.blockPos, stack) }
            heldItemsByPokemon.remove(pokemonEntity.uuid)
            failedDepositLocations.remove(pokemonEntity.uuid)
            return
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, inventoryPos, 2.0)) {
            val inventory = world.getBlockEntity(inventoryPos) as? Inventory
            if (inventory == null) {
                // Block not an inventory, mark it as failed
                triedPositions.add(inventoryPos)
                return
            }

            val remainingDrops = CobbleworkersInventoryUtils.insertStacks(inventory, itemsToDeposit)

            if (remainingDrops.size == itemsToDeposit.size) {
                //  No change in stack size, so mark as failed
                triedPositions.add(inventoryPos)
            }

            if (remainingDrops.isNotEmpty()) {
                heldItemsByPokemon[pokemonEntity.uuid] = remainingDrops
            } else {
                heldItemsByPokemon.remove(pokemonEntity.uuid)
                failedDepositLocations.remove(pokemonEntity.uuid)
                pokemonEntity.navigation.stop()
            }
        } else {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, inventoryPos)
        }
    }

    /**
     * Handles logic for finding and gathering an item on the ground.
     */
    private fun handleGathering(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.uuid
        val (closestItemPos, closestItem) = findClosestItem(world, origin, pokemonEntity) ?: return

        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestItemPos, world)) {
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
            CobbleworkersNavigationUtils.releaseTarget(pokemonId)
        }
    }

    /**
     * Finds the closest item on the ground and returns its position and entity.
     */
    private fun findClosestItem(world: World, origin: BlockPos, pokemonEntity: PokemonEntity): Pair<BlockPos, ItemEntity>? {
        var closestItem: ItemEntity? = null
        var closestDistance = Double.MAX_VALUE

        val searchArea = Box(origin).expand(searchRadius.toDouble(), searchHeight.toDouble(), searchRadius.toDouble())
        val items = world.getEntitiesByClass(ItemEntity::class.java, searchArea) { true }

        for (item in items) {
            if (item.isOnGround) {
                val distanceSq = item.squaredDistanceTo(pokemonEntity.pos)
                if (distanceSq < closestDistance) {
                    closestDistance = distanceSq
                    closestItem = item
                }
            }
        }

        return closestItem?.let { it.blockPos to it }
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