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
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

object DeferredBlockScanner {
    // TODO: Make this value configurable
    private const val BLOCKS_PER_TICK = 15

    private data class ScanJob(
        val iterator: Iterator<BlockPos>,
        var lastTickProcessed: Long
    )

    private val activeScans = mutableMapOf<BlockPos, ScanJob>()

    /**
     * Initiates or continues a deferred area scan for a pasture for one tick.
     */
    fun tickPastureAreaScan(
        world: World,
        pastureOrigin: BlockPos,
        searchRadius: Int,
        searchHeight: Int,
        jobValidators: Map<JobType, (World, BlockPos) -> Boolean>
    ) {
        val currentTick = world.time
        val scanJob = activeScans.getOrPut(pastureOrigin) {
            CobbleworkersCacheManager.removeTargets(pastureOrigin)

            val radius = searchRadius.toDouble()
            val height = searchHeight.toDouble()
            val searchArea = Box(pastureOrigin).expand(radius, height, radius)
            ScanJob(BlockPos.stream(searchArea).iterator(), currentTick - 1)
        }

        if (scanJob.lastTickProcessed == currentTick) return
        scanJob.lastTickProcessed = currentTick

        repeat(BLOCKS_PER_TICK) {
            if (!scanJob.iterator.hasNext()) {
                activeScans.remove(pastureOrigin)
                return
            }

            val pos = scanJob.iterator.next()

            for ((jobType, validator) in jobValidators) {
                if (validator(world, pos)) {
                    CobbleworkersCacheManager.addTarget(pastureOrigin, jobType, pos.toImmutable())
                }
            }
        }
    }
}