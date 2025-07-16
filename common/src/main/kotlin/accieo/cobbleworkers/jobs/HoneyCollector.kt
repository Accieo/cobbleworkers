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
    private val lastGenerationTime = mutableMapOf<UUID, Long>()
    private val cooldownTicks get() = config.honeyGenerationCooldownSeconds * 20L
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
        val pokemonId = pokemonEntity.pokemon.uuid
        val heldItems = heldItemsByPokemon[pokemonId]

        if (heldItems.isNullOrEmpty()) {
            failedDepositLocations.remove(pokemonId)
            if (!handleHarvesting(world, origin, pokemonEntity)) {
                handleGeneration(world, origin, pokemonEntity)
            }
        } else {
            handleDepositing(world, origin, pokemonEntity, heldItems)
        }
    }

    /**
     * Handles logic for finding and depositing items into an inventory when the Pokémon is holding items.
     * It will try multiple inventories nearby iteratively
     */
    private fun handleDepositing(world: World, origin: BlockPos, pokemonEntity: PokemonEntity, itemsToDeposit: List<ItemStack>) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val triedPositions = failedDepositLocations.getOrPut(pokemonId) { mutableSetOf() }
        val inventoryPos = CobbleworkersInventoryUtils.findClosestInventory(world, origin,
            searchRadius,
            searchHeight, triedPositions)

        if (inventoryPos == null) {
            // No (untried) inventories found, so we just drop the remaining items and reset.
            itemsToDeposit.forEach { stack -> Block.dropStack(world, pokemonEntity.blockPos, stack) }
            heldItemsByPokemon.remove(pokemonId)
            failedDepositLocations.remove(pokemonId)
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
                heldItemsByPokemon[pokemonId] = remainingDrops
            } else {
                heldItemsByPokemon.remove(pokemonId)
                failedDepositLocations.remove(pokemonId)
                pokemonEntity.navigation.stop()
            }
        } else {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, inventoryPos)
        }
    }

    /**
     * Handles logic for finding and collecting honeycomb when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity): Boolean {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestBeehive = findClosestReadyBeehive(world, origin) ?: return false
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestBeehive, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestBeehive, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestBeehive, world)
            }
            return true
        }

        if (currentTarget == closestBeehive) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestBeehive.down())
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            harvestHoneycomb(world, closestBeehive, pokemonEntity)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }

        return true
    }

    /**
     * Handles logic to increase honey level by one.
     */
    private fun generateHoney(world: World, beehivePos: BlockPos) {
        val state = world.getBlockState(beehivePos)
        val block = state.block
        if (block is BeehiveBlock) {
            val currentLevel = state.get(BeehiveBlock.HONEY_LEVEL)
            if (currentLevel < BeehiveBlock.FULL_HONEY_LEVEL) {
                world.setBlockState(beehivePos, state.with(BeehiveBlock.HONEY_LEVEL, currentLevel + 1), Block.NOTIFY_ALL)
            }
        }
    }

    /**
     * Handles generation of honey.
     */
    private fun handleGeneration(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        if (!config.combeeLineGeneratesHoney) return

        val pokemonId = pokemonEntity.pokemon.uuid
        val now = world.time
        val lastTime = lastGenerationTime[pokemonId] ?: 0L

        if (now - lastTime < cooldownTicks) {
            return
        }

        val closestBeehive = findClosestNonReadyBeehive(world, origin) ?: return

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, closestBeehive.down(), 2.0)) {
            generateHoney(world, closestBeehive)
            lastGenerationTime[pokemonId] = now
        } else {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestBeehive.down())
        }
    }

    /**
     * Scans the pasture's block surrounding area for the closest beehive.
     */
    private fun findClosestBeehive(world: World, origin: BlockPos, honeyLevelPredicate: (Int) -> Boolean): BlockPos? {
        return BlockPos.findClosest(origin, searchRadius, searchHeight) { pos ->
            val state = world.getBlockState(pos)
            state.block is BeehiveBlock &&
                    !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world) &&
                    honeyLevelPredicate(state.get(BeehiveBlock.HONEY_LEVEL))
        }.orElse(null)
    }

    /**
     * Finds closest ready beehive.
     */
    private fun findClosestReadyBeehive(world: World, origin: BlockPos): BlockPos? {
        return findClosestBeehive(world, origin) { it == BeehiveBlock.FULL_HONEY_LEVEL }
    }

    /**
     * Finds closest non-ready beehive.
     */
    private fun findClosestNonReadyBeehive(world: World, origin: BlockPos): BlockPos? {
        return findClosestBeehive(world, origin) { it < BeehiveBlock.FULL_HONEY_LEVEL }
    }

    /**
     * Executes the complete collecting process for a single beehive.
     */
    private fun harvestHoneycomb(world: World, beehivePos: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val beehiveState = world.getBlockState(beehivePos)
        heldItemsByPokemon[pokemonId] = listOf(ItemStack(Items.HONEYCOMB, 3))
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