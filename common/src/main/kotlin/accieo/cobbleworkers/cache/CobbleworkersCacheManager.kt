/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.cache

import accieo.cobbleworkers.enums.JobType
import net.minecraft.util.math.BlockPos

object CobbleworkersCacheManager {
    private val pastureCaches: MutableMap<BlockPos, PastureCache> = mutableMapOf()

    /**
     * Adds a target block for the given job type.
     */
    fun addTarget(pastureOrigin: BlockPos, jobType: JobType, pos: BlockPos) {
        val cache = pastureCaches.getOrPut(pastureOrigin) { PastureCache() }
        cache.targetsByJob[jobType]?.add(pos)
    }

    /**
     * Removes a target block for the given job type.
     */
    fun removeTargets(pastureOrigin: BlockPos) {
        pastureCaches[pastureOrigin]?.targetsByJob?.values?.forEach { it.clear() }
    }

    /**
     * Gets targets for a given job type.
     */
    fun getTargets(pastureOrigin: BlockPos, jobType: JobType): Set<BlockPos> {
        return pastureCaches[pastureOrigin]?.targetsByJob?.get(jobType) ?: emptySet()
    }

    /**
     * Clear a pasture block entity from the cache.
     */
    fun removePasture(pastureOrigin: BlockPos) {
        pastureCaches.remove(pastureOrigin)
    }
}