/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import net.minecraft.block.Blocks
import net.minecraft.block.FarmlandBlock
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

/**
 * Utility functions for crop related stuff
 */
object CobbleworkersCropUtils {

    /**
     * Finds the closest dry farmland
     */
    fun findClosestFarmland(world: World, origin: BlockPos, searchRadius: Int): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        val searchArea = Box(origin).expand(searchRadius.toDouble(), 1.5, searchRadius.toDouble())
        BlockPos.stream(searchArea).forEach { pos ->
            val state = world.getBlockState(pos)
            val block = state.block
            if (block == Blocks.FARMLAND && state.get(FarmlandBlock.MOISTURE) <= 3) {
                val distanceSq = origin.getSquaredDistance(pos)
                if (distanceSq < closestDistance) {
                    closestDistance = distanceSq
                    closestPos = pos.toImmutable()
                }
            }
        }

        return closestPos
    }
}