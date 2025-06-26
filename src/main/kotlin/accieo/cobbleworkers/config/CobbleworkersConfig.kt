/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.config

import me.shedaniel.autoconfig.ConfigData
import me.shedaniel.autoconfig.annotation.Config

@Config(name = "cobbleworkers")
class CobbleworkersConfig : ConfigData {
    // Apricorn harvesting
    var apricornHarvestersEnabled = true
    var bugTypeHarvestsApricorns = true
    var apricornHarvesters: MutableList<String> = mutableListOf("dragonite")
    // Crop irrigation
    var cropIrrigatorsEnabled = true
    var waterTypeIrrigateCrops = true
    var cropIrrigators: MutableList<String> = mutableListOf("dragonite")
}