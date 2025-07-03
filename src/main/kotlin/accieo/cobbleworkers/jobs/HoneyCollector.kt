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
import net.minecraft.block.BeehiveBlock
import net.minecraft.block.Block
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import java.util.UUID

/**
 * A worker job for a Pokémon to find, navigate to, and collect honey.
 * Collected items are deposited into the nearest available inventory.
 */
object HoneyCollector : Worker {
    private val VALID_SPECIES: Set<String> = setOf("combee", "vespiquen")
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val config = CobbleworkersConfigHolder.config.honey
    private val searchRadius get() = config.searchRadius
    private val searchHeight get() = config.searchHeight

    /**
     * Determines if Pokémon is eligible to be a honey collector.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.honeyCollectorsEnabled) return false

        return isAllowedBySpecies(pokemonEntity) || isDesignatedCollector(pokemonEntity) || CobbleworkersTypeUtils.isAllowedByType(config.typeHarvestsHoney, pokemonEntity)
    }

    /**
     * Main logic loop for the honey collector, executed each tick.
     * Delegates to state handlers handleHarvesting and handleDepositing
     * to manage the current task of the Pokémon.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val heldItems = heldItemsByPokemon[pokemonEntity.uuid]

        if (heldItems.isNullOrEmpty()) {
            failedDepositLocations.remove(pokemonEntity.uuid)
            handleHarvesting(world, origin, pokemonEntity)
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
            }
        } else {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, inventoryPos)
        }
    }

    /**
     * Handles logic for finding and collecting honeycomb when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.uuid
        val closestBeehive = findClosestReadyBeehive(world, origin, pokemonEntity) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestBeehive)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestBeehive)
            }
            return
        }

        if (currentTarget == closestBeehive) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestBeehive.down())
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            harvestHoneycomb(world, closestBeehive, pokemonEntity)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId)
        }
    }

    /**
     * Scans the pasture's block surrounding area for the closest beehive.
     */
    private fun findClosestReadyBeehive(world: World, origin: BlockPos, pokemonEntity: PokemonEntity): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        val searchArea = Box(origin).expand(searchRadius.toDouble(), searchHeight.toDouble(), searchRadius.toDouble())

        BlockPos.stream(searchArea).forEach { pos ->
            val state = world.getBlockState(pos)
            if (state.block is BeehiveBlock && state.get(BeehiveBlock.HONEY_LEVEL) == BeehiveBlock.FULL_HONEY_LEVEL) {
                val distanceSq = pos.getSquaredDistance(pokemonEntity.pos)
                if (distanceSq < closestDistance) {
                    closestDistance = distanceSq
                    closestPos = pos.toImmutable()
                }
            }
        }

        return closestPos
    }

    /**
     * Executes the complete collecting process for a single beehive.
     */
    private fun harvestHoneycomb(world: World, beehivePos: BlockPos, pokemonEntity: PokemonEntity) {
        val beehiveState = world.getBlockState(beehivePos)
        heldItemsByPokemon[pokemonEntity.uuid] = listOf(ItemStack(Items.HONEYCOMB, 3))
        world.setBlockState(beehivePos, beehiveState.with(BeehiveBlock.HONEY_LEVEL, 0), Block.NOTIFY_ALL)
    }

    /**
     * Checks if the Pokémon qualifies as a collector because of its species,
     * and it is allowed in the config.
     */
    private fun isAllowedBySpecies(pokemonEntity: PokemonEntity): Boolean {
        if (!config.combeeLineCollectsHoney) return false
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return speciesName in VALID_SPECIES
    }

    /**
     * Checks if the Pokémon qualifies as a collector because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedCollector(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.honeyCollectors.any { it.lowercase() == speciesName }
    }
}