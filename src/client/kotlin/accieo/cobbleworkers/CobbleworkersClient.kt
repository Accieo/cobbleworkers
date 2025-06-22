/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers

import accieo.cobbleworkers.config.CobbleworkersConfig
import accieo.cobbleworkers.config.CobbleworkersConfigHolder
import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer
import net.fabricmc.api.ClientModInitializer

object CobbleworkersClient : ClientModInitializer {
	override fun onInitializeClient() {
		val configHolder = AutoConfig.register(CobbleworkersConfig::class.java, ::GsonConfigSerializer)

		CobbleworkersConfigHolder.config = configHolder.get()
	}
}