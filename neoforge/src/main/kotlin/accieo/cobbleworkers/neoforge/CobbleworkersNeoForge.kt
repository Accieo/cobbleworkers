/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.neoforge

import accieo.cobbleworkers.Cobbleworkers
import accieo.cobbleworkers.neoforge.client.config.CobbleworkersModListScreen
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import net.minecraft.client.MinecraftClient
import net.minecraft.registry.Registries
import net.minecraft.util.Identifier
import net.neoforged.fml.common.EventBusSubscriber
import net.neoforged.fml.common.Mod
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS
import thedarkcolour.kotlinforforge.neoforge.forge.runForDist
import net.neoforged.bus.api.SubscribeEvent
import net.neoforged.fml.ModList
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent

/**
 * NeoForge entrypoint.
 */
@Mod(Cobbleworkers.MODID)
@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
object CobbleworkersNeoForge {
    init {
        Cobbleworkers.init()

        val obj = runForDist(
            clientTarget = {
                MOD_BUS.addListener(::onClientSetup)
                MinecraftClient.getInstance()
            },
            serverTarget = {
                MOD_BUS.addListener(::onServerSetup)
            }
        )
    }

    private fun onClientSetup(event: FMLClientSetupEvent) {
        CobbleworkersModListScreen.registerModScreen()
    }

    private fun onServerSetup(event: FMLDedicatedServerSetupEvent) {
        //
    }

    @SubscribeEvent
    fun onCommonSetup(event: FMLCommonSetupEvent) {
        // TODO: This is just test boilerplate, abstract out and update inv. utils
        if (ModList.get().isLoaded("sophisticatedstorage")) {
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