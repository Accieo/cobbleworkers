/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.fabric

import accieo.cobbleworkers.Cobbleworkers
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import net.fabricmc.api.ModInitializer
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier

/**
 * Fabric entrypoint.
 */
object CobbleworkersFabric : ModInitializer {
    override fun onInitialize() {
        Cobbleworkers.init()

        // TODO: This is just test boilerplate, abstract out and update inv. utils
        ServerLifecycleEvents.SERVER_STARTING.register { _ ->
            if (FabricLoader.getInstance().isModLoaded("sophisticatedstorage")) {
                try {
                    val sophisticatedBlocks = setOfNotNull(
                        Registries.BLOCK.get(Identifier.of("sophisticatedstorage", "limited_barrel_1")),
                        Registries.BLOCK.get(Identifier.of("sophisticatedstorage", "limited_barrel_2")),
                        Registries.BLOCK.get(Identifier.of("sophisticatedstorage", "limited_barrel_3")),
                        Registries.BLOCK.get(Identifier.of("sophisticatedstorage", "limited_barrel_4")),
                    )
                    CobbleworkersInventoryUtils.addCompatibility(sophisticatedBlocks)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}