/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import accieo.cobbleworkers.enums.JobType
import net.minecraft.util.math.BlockPos

// TODO: Implement this
object CobbleworkersTargetCacheManager {
    val targetsByJob: MutableMap<JobType, MutableSet<BlockPos>> =
        JobType.entries.associateWith { mutableSetOf<BlockPos>() }.toMutableMap()

    /**
     * Adds a target block for the given job type.
     */
    fun addTarget(jobType: JobType, pos: BlockPos) {
        targetsByJob[jobType]?.add(pos)
    }

    /**
     * Adds a target block for the given job type.
     */
    fun removeTarget(jobType: JobType, pos: BlockPos) {
        targetsByJob[jobType]?.remove(pos)
    }
}