/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import com.cobblemon.mod.common.CobblemonBlocks
import net.minecraft.block.Blocks
import net.minecraft.block.entity.BlockEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World

object CobbleworkersInventoryUtils {
    fun findClosestInventory(world: World, origin: BlockPos, radius: Int): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE
        val validBlocks = arrayOf(
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

        // TODO: Update to check closest AVAILABLE inventory, better gameplay
        BlockPos.stream(origin.add(-radius, -5, -radius), origin.add(radius, 5, radius)).forEach { pos ->
            val block = world.getBlockState(pos).block
            if (validBlocks.contains(block)) {
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

    fun insertStack(inventory: Inventory, stack: ItemStack): ItemStack {
        if (stack.isEmpty) {
            return ItemStack.EMPTY
        }
        val newStack = stack.copy()
        for (i in 0 until inventory.size()) {
            val inventoryStack = inventory.getStack(i)
            if (canInsertIntoSlot(inventoryStack, newStack)) {
                val space = inventoryStack.maxCount - inventoryStack.count
                val toTransfer = minOf(newStack.count, space)
                if (toTransfer > 0) {
                    if (inventoryStack.isEmpty) {
                        inventory.setStack(i, newStack.split(toTransfer))
                    } else {
                        inventoryStack.increment(toTransfer)
                        newStack.decrement(toTransfer)
                    }
                }
            }
            if (newStack.isEmpty) {
                break
            }
        }
        return newStack
    }

    fun canInsertIntoSlot(slotStack: ItemStack, insertStack: ItemStack): Boolean {
        return slotStack.isEmpty || (ItemStack.areItemsAndComponentsEqual(slotStack, insertStack) && slotStack.count < slotStack.maxCount)
    }
}