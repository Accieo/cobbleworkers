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
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

object TumblestoneHarvester : Worker {
    private val VALID_TUMBLESTONE_BLOCKS: Set<Block> = setOf(
        CobblemonBlocks.TUMBLESTONE_CLUSTER,
        CobblemonBlocks.BLACK_TUMBLESTONE_CLUSTER,
        CobblemonBlocks.SKY_TUMBLESTONE_CLUSTER,
    )
    private val REPLACEMENT_BLOCKS: Map<Block, Block> = mapOf(
        CobblemonBlocks.TUMBLESTONE_CLUSTER to CobblemonBlocks.SMALL_BUDDING_TUMBLESTONE,
        CobblemonBlocks.BLACK_TUMBLESTONE_CLUSTER to CobblemonBlocks.SMALL_BUDDING_BLACK_TUMBLESTONE,
        CobblemonBlocks.SKY_TUMBLESTONE_CLUSTER to CobblemonBlocks.SMALL_BUDDING_SKY_TUMBLESTONE
    )
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val config = CobbleworkersConfigHolder.config.tumblestone
    private val searchRadius get() = config.searchRadius
    private val searchHeight get() = config.searchHeight

    /**
     * Determines if Pokémon is eligible to be a tumblestone harvester.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.tumblestoneHarvestersEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeHarvestsTumblestone, pokemonEntity) || isDesignatedHarvester(pokemonEntity)
    }

    /**
     * Main logic loop for the tumblestone harvester, executed each tick.
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
     * Handles logic for finding and harvesting a tumblestone cluster when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestTumblestone = findClosestTumblestone(world, origin) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestTumblestone, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestTumblestone, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestTumblestone, world)
            }
            return
        }

        if (currentTarget == closestTumblestone) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestTumblestone)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget, 1.5)) {
            harvestTumblestone(world, closestTumblestone, pokemonEntity)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Scans the pasture's block surrounding area for the closest tumblestone cluster.
     */
    private fun findClosestTumblestone(world: World, origin: BlockPos): BlockPos? {
        return BlockPos.findClosest(origin, searchRadius, searchHeight) { pos ->
            val state = world.getBlockState(pos)
            state.block in VALID_TUMBLESTONE_BLOCKS && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
        }.orElse(null)
    }

    /**
     * Executes the complete harvesting process for a single tumblestone cluster
     */
    private fun harvestTumblestone(world: World, tumblestonePos: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val tumblestoneState = world.getBlockState(tumblestonePos)
        if (tumblestoneState.block !in VALID_TUMBLESTONE_BLOCKS) return

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, tumblestonePos.toCenterPos())
            .add(LootContextParameters.BLOCK_STATE, tumblestoneState)
            .add(LootContextParameters.TOOL, ItemStack.EMPTY)
            .addOptional(LootContextParameters.THIS_ENTITY, pokemonEntity)

        val drops = tumblestoneState.getDroppedStacks(lootParams)

        if (drops.isNotEmpty()) {
            heldItemsByPokemon[pokemonId] = drops
        }

        if (config.shouldReplantTumblestone) {
            val originalBlock = tumblestoneState.block
            val replacementBlock = REPLACEMENT_BLOCKS[originalBlock] ?: return

            var replacementState = replacementBlock.defaultState

            val facingProperty = Properties.FACING
            if (replacementState.properties.contains(facingProperty) && tumblestoneState.contains(facingProperty)) {
                replacementState = replacementState.with(facingProperty, tumblestoneState.get(facingProperty))
            }

            world.setBlockState(tumblestonePos, replacementState)
        } else {
            world.setBlockState(tumblestonePos, Blocks.AIR.defaultState)
        }
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedHarvester(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.tumblestoneHarvesters.any { it.lowercase() == speciesName }
    }
}