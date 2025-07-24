/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.fabric.integration

import accieo.cobbleworkers.Cobbleworkers
import accieo.cobbleworkers.utilities.CobbleworkersCropUtils
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.block.Block
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

object CobbleworkersIntegrationHandler {
    private const val FARMERS_DELIGHT = "farmersdelight"
    private const val SOPHISTICATED_STORAGE = "sophisticatedstorage"

    /**
     * Runs all relevant functions to add integrations
     */
    fun addIntegrations() {
        addFarmersDelight()
        addSophisticatedStorage()
    }

    /**
     * Checks whether a mod is loaded
     */
    private fun isModLoaded(modId: String): Boolean {
        return FabricLoader.getInstance().isModLoaded(modId)
    }

    /**
     * Fetches mod blocks from block registry
     */
    private fun getModBlocks(modId: String, names: List<String>): Set<Block> {
        return names.mapNotNull { name ->
            Registries.BLOCK.getOrEmpty(Identifier.of(modId, name)).orElse(null)
        }.toSet()
    }

    /**
     * Adds integration for farmer's delight
     */
    private fun addFarmersDelight() {
        if (!isModLoaded(FARMERS_DELIGHT)) return

        val farmersDelightCrops = getModBlocks(
            FARMERS_DELIGHT,
            CobbleworkersCropUtils.farmersDelightCrops
        )

        CobbleworkersCropUtils.addCompatibility(farmersDelightCrops)
        Cobbleworkers.LOGGER.info("Added integration for farmer's delight!")
    }

    /**
     * Adds integration for sophisticated storage
     */
    private fun addSophisticatedStorage() {
        // TODO: This is a work in progress, not yet implemented.
        return

        if (!isModLoaded(SOPHISTICATED_STORAGE)) return

        val sophisticatedStorageBlocks = getModBlocks(
            SOPHISTICATED_STORAGE,
            listOf("limited_barrel_1", "limited_barrel_2", "limited_barrel_3", "limited_barrel_4")
        )

        CobbleworkersInventoryUtils.addCompatibility(sophisticatedStorageBlocks)
    }
}