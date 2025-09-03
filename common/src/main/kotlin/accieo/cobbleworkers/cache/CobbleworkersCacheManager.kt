/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.cache

import accieo.cobbleworkers.enums.JobType
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos

object CobbleworkersCacheManager {
    private val pastureCaches: MutableMap<BlockPos, PastureCache> = mutableMapOf()
    private var structuresCache: Set<Identifier>? = null

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

    /**
     * Gets or builds the server world structures cache.
     */
    fun getStructures(world: ServerWorld, useAll: Boolean, tags: List<String>): Set<Identifier> {
        structuresCache?.let { return it }

        val registryManager = world.server.registryManager
        val structureRegistry = registryManager.get(RegistryKeys.STRUCTURE)

        val structures = if (useAll) {
            structureRegistry.keys.map { it.value }.toSet()
        } else {
            tags.mapNotNull { Identifier.tryParse(it) }.toSet()
        }

        structuresCache = structures
        return structures
    }
}