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
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

/**
 * A worker job for a Pokémon to find, navigate to, and harvest fully grown amethysts.
 * Harvested items are deposited into the nearest available inventory.
 */
object AmethystHarvester : Worker {
    private const val SEARCH_RADIUS = 8
    private const val VERTICAL_SEARCH_RANGE = 5
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()

    /**
     * Determines if Pokémon is eligible to be an amethyst harvester.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        if (!config.amethystHarvestersEnabled) return false

        return isAllowedByRockType(pokemonEntity) || isDesignatedHarvester(pokemonEntity)
    }

    /**
     * Main logic loop for the amethyst harvester, executed each tick.
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
           SEARCH_RADIUS,
           VERTICAL_SEARCH_RANGE, triedPositions)

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
     * Handles logic for finding and harvesting an amethyst cluster when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.uuid
        val closestAmethyst = findClosestAmethystCluster(world, origin, pokemonEntity) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestAmethyst)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestAmethyst)
            }
            return
        }

        CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestAmethyst)

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget, 1.5)) {
            harvestAmethystCluster(world, closestAmethyst, pokemonEntity)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId)
        }
    }

    /**
     * Scans the pasture's block surrounding area for the closest amethyst cluster.
     */
    private fun findClosestAmethystCluster(world: World, origin: BlockPos, pokemonEntity: PokemonEntity): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        val searchArea = Box(origin).expand(SEARCH_RADIUS.toDouble(), VERTICAL_SEARCH_RANGE.toDouble(), SEARCH_RADIUS.toDouble())

        BlockPos.stream(searchArea).forEach { pos ->
            val state = world.getBlockState(pos)
            if (state.block == Blocks.AMETHYST_CLUSTER) {
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
     * Executes the complete harvesting process for a single amethyst cluster
     */
    private fun harvestAmethystCluster(world: World, amethystPos: BlockPos, pokemonEntity: PokemonEntity) {
        val amethystState = world.getBlockState(amethystPos)
        if (amethystState.block != Blocks.AMETHYST_CLUSTER) return

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, amethystPos.toCenterPos())
            .add(LootContextParameters.BLOCK_STATE, amethystState)
            .add(LootContextParameters.TOOL, ItemStack.EMPTY)
            .addOptional(LootContextParameters.THIS_ENTITY, pokemonEntity)

        val drops = amethystState.getDroppedStacks(lootParams)

        if (drops.isNotEmpty()) {
            heldItemsByPokemon[pokemonEntity.uuid] = drops
        }

        world.setBlockState(amethystPos, Blocks.AIR.defaultState)
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because it's a rock type
     * and rock type harvesting is enabled via config.
     */
    private fun isAllowedByRockType(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        return config.rockTypeHarvestsAmethyst && pokemonEntity.pokemon.types.any { it == ElementalTypes.ROCK }
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedHarvester(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.amethystHarvesters.any { it.lowercase() == speciesName }
    }
}