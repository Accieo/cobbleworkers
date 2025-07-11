/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import net.minecraft.block.Blocks
import net.minecraft.block.LeveledCauldronBlock
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

object CobbleworkersCauldronUtils {
    enum class CauldronFluid {
        WATER, LAVA, POWDER_SNOW
    }

    /**
     * Finds the closest empty cauldron
     */
    fun findClosestCauldron(world: World, origin: BlockPos, searchRadius: Int, searchHeight: Int): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        val searchArea = Box(origin).expand(searchRadius.toDouble(), searchHeight.toDouble(), searchRadius.toDouble())
        BlockPos.stream(searchArea).forEach { pos ->
            val state = world.getBlockState(pos)
            if (state.isOf(Blocks.CAULDRON)) {
                val distanceSq = origin.getSquaredDistance(pos)
                if (distanceSq < closestDistance) {
                    closestDistance = distanceSq
                    closestPos = pos.toImmutable()
                }
            }
        }

        return closestPos
    }

    /**
     * Adds fluid to cauldron
     */
    fun addFluid(world: World, cauldronPos: BlockPos, fluid: CauldronFluid) {
        val newState = when (fluid) {
            CauldronFluid.WATER -> Blocks.WATER_CAULDRON.defaultState.with(LeveledCauldronBlock.LEVEL, LeveledCauldronBlock.MAX_LEVEL)
            CauldronFluid.LAVA -> Blocks.LAVA_CAULDRON.defaultState
            CauldronFluid.POWDER_SNOW -> Blocks.POWDER_SNOW_CAULDRON.defaultState.with(LeveledCauldronBlock.LEVEL, LeveledCauldronBlock.MAX_LEVEL)
        }

        world.setBlockState(cauldronPos, newState)
    }
}