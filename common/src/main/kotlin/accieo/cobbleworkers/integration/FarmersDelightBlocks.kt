/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.integration

object FarmersDelightBlocks {
    const val TOMATOES = "tomatoes"
    const val RICE_PANICLES = "rice_panicles"
    const val BROWN_MUSHROOM_COLONY = "brown_mushroom_colony"
    const val RED_MUSHROOM_COLONY = "red_mushroom_colony"
    const val ONIONS = "onions"
    const val CABBAGES = "cabbages"
    val ALL = setOf(
        ONIONS,
        CABBAGES,
        TOMATOES,
        RICE_PANICLES,
        BROWN_MUSHROOM_COLONY,
        RED_MUSHROOM_COLONY
    )
    val MUSHROOMS = setOf(
        BROWN_MUSHROOM_COLONY,
        RED_MUSHROOM_COLONY
    )
}
