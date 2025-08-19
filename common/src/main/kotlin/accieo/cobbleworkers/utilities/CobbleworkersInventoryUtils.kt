/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import accieo.cobbleworkers.cache.CobbleworkersCacheManager
import accieo.cobbleworkers.enums.JobType
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.ChestBlock
import net.minecraft.block.entity.ChestBlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.collections.mutableSetOf
import kotlin.collections.set

object CobbleworkersInventoryUtils {
    private val validInventoryBlocks: MutableSet<Block> = mutableSetOf(
        Blocks.CHEST,
        Blocks.TRAPPED_CHEST,
        Blocks.BARREL,
        CobblemonBlocks.GILDED_CHEST,
        CobblemonBlocks.BLUE_GILDED_CHEST,
        CobblemonBlocks.PINK_GILDED_CHEST,
        CobblemonBlocks.BLACK_GILDED_CHEST,
        CobblemonBlocks.WHITE_GILDED_CHEST,
        CobblemonBlocks.GREEN_GILDED_CHEST,
        CobblemonBlocks.YELLOW_GILDED_CHEST,
    )

    /**
     * Add inventory integrations dynamically at runtime
     */
    fun addCompatibility(externalBlocks: Set<Block>) {
        validInventoryBlocks.addAll(externalBlocks)
    }

    /**
     * Validates whether the block is a valid inventory block.
     */
    fun blockValidator(world: World, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        return state.block in validInventoryBlocks
    }

    /**
     * Finds closest inventory
     */
    fun findClosestInventory(world: World, origin: BlockPos, ignorePos: Set<BlockPos> = emptySet()): BlockPos? {
        val possibleTargets = CobbleworkersCacheManager.getTargets(origin, JobType.Generic)
        if (possibleTargets.isEmpty()) return null

        return possibleTargets
            .filter { pos ->
                blockValidator(world, pos) && pos !in ignorePos
            }
            .minByOrNull { it.getSquaredDistance(origin) }
    }

    /**
     * Attempt to get actual inventory (wrapper to handle double chests)
     */
    fun getActualInventory(inventory: Inventory): Inventory {
        if (inventory !is ChestBlockEntity) return inventory
        val world = inventory.world ?: return inventory
        val pos = inventory.pos
        val state = world.getBlockState(pos)
        val block = state.block

        if (block is ChestBlock) {
            return ChestBlock.getInventory(block, state, world, pos, true) ?: inventory
        }

        return inventory
    }

    /**
     * Inserts a list of ItemStack into an inventory, returning the remainder
     */
    fun insertStacks(inventory: Inventory, stacks: List<ItemStack>): List<ItemStack> {
        val actualInventory = getActualInventory(inventory)

        val remainingDrops = mutableListOf<ItemStack>()
        stacks.forEach { stack ->
            val remaining = insertStack(actualInventory, stack.copy())
            if (!remaining.isEmpty) {
                remainingDrops.add(remaining)
            }
        }

        return remainingDrops
    }

    /**
     * Inserts an ItemStack into an inventory, returning the remainder.
     * Prioritizes existing slots first, then empty slots.
     */
    fun insertStack(inventory: Inventory, stack: ItemStack): ItemStack {
        if (stack.isEmpty) {
            return ItemStack.EMPTY
        }

        var remainingStack = stack.copy()
        remainingStack = fillExistingStacks(inventory, remainingStack)
        if (!remainingStack.isEmpty) {
            remainingStack = fillEmptySlots(inventory, remainingStack)
        }

        return remainingStack
    }

    /**
     * Iterates through the inventory to find empty slots to place remaining items in.
     */
    private fun fillEmptySlots(inventory: Inventory, stack: ItemStack): ItemStack {
        for (i in 0 until inventory.size()) {
            if (inventory.getStack(i).isEmpty) {
                // If a slot is empty, item's stack size limits the amount we can place.
                val toTransfer = minOf(stack.count, stack.maxCount)
                inventory.setStack(i, stack.split(toTransfer))
                inventory.markDirty()
            }

            if (stack.isEmpty) {
                return ItemStack.EMPTY
            }
        }

        return stack
    }

    /**
     * Iterates through the inventory to find non-null stacks of the same item and adds them.
     */
    private fun fillExistingStacks(inventory: Inventory, stack: ItemStack): ItemStack {
        for (i in 0 until inventory.size()) {
            val inventoryStack = inventory.getStack(i)

            if (inventoryStack.isEmpty || !ItemStack.areItemsAndComponentsEqual(inventoryStack, stack)) {
                continue
            }

            val availableSpace = inventoryStack.maxCount - inventoryStack.count
            if (availableSpace > 0) {
                val toTransfer = minOf(stack.count, availableSpace)

                inventoryStack.increment(toTransfer)
                stack.decrement(toTransfer)
                inventory.markDirty()
            }

            if (stack.isEmpty) {
                return ItemStack.EMPTY
            }
        }

        return stack
    }

    /**
     * Handles depositing items into an inventory.
     */
    /**
     * Handles logic for finding and depositing items into an inventory when the Pokémon is holding items.
     * It will try multiple inventories nearby iteratively
     */
    fun handleDepositing(
        world: World,
        origin: BlockPos,
        pokemonEntity: PokemonEntity,
        itemsToDeposit: List<ItemStack>,
        failedDepositLocations: MutableMap<UUID, MutableSet<BlockPos>>,
        heldItemsByPokemon: MutableMap<UUID, List<ItemStack>>
    ) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val triedPositions = failedDepositLocations.getOrPut(pokemonId) { mutableSetOf() }
        val inventoryPos = findClosestInventory(world, origin, triedPositions)

        if (inventoryPos == null) {
            // Don't drop items yet if scan is running as inventory might be found within the next ticks.
            if (DeferredBlockScanner.isScanActive(origin)) {
                heldItemsByPokemon[pokemonId] = itemsToDeposit
                return
            }

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

            val remainingDrops = insertStacks(inventory, itemsToDeposit)

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
}