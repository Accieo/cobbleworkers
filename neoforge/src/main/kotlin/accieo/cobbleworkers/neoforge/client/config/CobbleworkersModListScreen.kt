/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.neoforge.client.config

import accieo.cobbleworkers.config.CobbleworkersConfig
import me.shedaniel.autoconfig.AutoConfig
import net.minecraft.client.gui.screen.Screen
import net.neoforged.fml.ModContainer
import net.neoforged.fml.ModLoadingContext
import net.neoforged.neoforge.client.gui.IConfigScreenFactory

object CobbleworkersModListScreen {
    fun registerModScreen() {
        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory::class.java) {
            IConfigScreenFactory { _: ModContainer, parent: Screen ->
                AutoConfig.getConfigScreen(CobbleworkersConfig::class.java, parent).get()
            }
        }
    }
}