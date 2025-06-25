/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import com.cobblemon.mod.common.CobblemonBlocks
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

object CobbleworkersInventoryUtils {
    private val VALID_INVENTORY_BLOCKS: Set<Block> = setOf(
        Blocks.CHEST,
        Blocks.BARREL,
        CobblemonBlocks.GILDED_CHEST,
        CobblemonBlocks.BLUE_GILDED_CHEST,
        CobblemonBlocks.PINK_GILDED_CHEST,
        CobblemonBlocks.BLACK_GILDED_CHEST,
        CobblemonBlocks.WHITE_GILDED_CHEST,
        CobblemonBlocks.GREEN_GILDED_CHEST,
        CobblemonBlocks.YELLOW_GILDED_CHEST,
    )

    @Deprecated("Old ugly version, should use findClosestAvailableInventory after fixing it")
    fun findClosestInventory(world: World, origin: BlockPos, radius: Int): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        BlockPos.stream(origin.add(-radius, -5, -radius), origin.add(radius, 5, radius)).forEach { pos ->
            val block = world.getBlockState(pos).block
            if (block in VALID_INVENTORY_BLOCKS) {
                val blockEntity: BlockEntity? = world.getBlockEntity(pos)
                if (blockEntity is Inventory) {
                    val distance = origin.getSquaredDistance(pos)
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestPos = pos.toImmutable()
                    }
                }
            }
        }

        return closestPos
    }

    /**
     * Finds closest inventory nearby with enough space to insert items
     */
    // TODO: Fix so it can handle stacks correctly.
    fun findClosestAvailableInventory(world: World, origin: BlockPos, stacks: List<ItemStack>, searchRadius: Int, verticalRange: Int): BlockPos? {
        if (stacks.isEmpty()) {
            return null
        }

        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        val searchArea = Box(origin).expand(searchRadius.toDouble(), verticalRange.toDouble(), searchRadius.toDouble())
        BlockPos.stream(searchArea).forEach { pos ->
            val blockState = world.getBlockState(pos)
            val blockEntity = world.getBlockEntity(pos)

            if (blockState.block in VALID_INVENTORY_BLOCKS && blockEntity is Inventory) {
                if (hasAvailableSlot(blockEntity, stacks)) {
                    val distanceSq = origin.getSquaredDistance(pos)
                    if (distanceSq < closestDistance) {
                        closestDistance = distanceSq
                        closestPos = pos.toImmutable()
                    }
                }
            }
        }

        return closestPos
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
     * Checks if an inventory has any space for a given item.
     */
    // TODO: Fix this to handle stacks correctly
    private fun hasAvailableSlot(inventory: Inventory, stacks: List<ItemStack>): Boolean {
        for (stack in stacks) {
            for (i in 0 until inventory.size()) {
                val slotStack = inventory.getStack(i)

                if (slotStack.isEmpty) {
                    return true
                }

                val isSameItem = ItemStack.areItemsAndComponentsEqual(slotStack, stack)
                val hasSpace = slotStack.count < slotStack.maxCount

                if (isSameItem && hasSpace) {
                    return true
                }
            }
        }

        return false
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
            }

            if (stack.isEmpty) {
                return ItemStack.EMPTY
            }
        }

        return stack
    }
}