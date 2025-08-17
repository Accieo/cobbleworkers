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

data class PastureCache(
    val targetsByJob: MutableMap<JobType, MutableSet<BlockPos>> =
        JobType.entries.associateWith { mutableSetOf<BlockPos>() }.toMutableMap()
)