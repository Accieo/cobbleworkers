/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.config

import me.shedaniel.autoconfig.AutoConfig
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer

object CobbleworkersConfigInitializer {
    fun init() {
        val configHolder = AutoConfig.register(CobbleworkersConfig::class.java, ::GsonConfigSerializer)

        CobbleworkersConfigHolder.config = configHolder.get()
    }
}