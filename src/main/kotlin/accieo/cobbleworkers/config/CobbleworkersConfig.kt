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
import me.shedaniel.autoconfig.annotation.ConfigEntry

@Config(name = "cobbleworkers")
class CobbleworkersConfig : ConfigData {

    @ConfigEntry.Gui.CollapsibleObject
    var apricorn = ApricornGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var irrigation = IrrigationGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var amethyst = AmethystGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var tumblestone = TumblestoneGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var cropHarvest = CropHarvestGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var berries = BerriesGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var honey = HoneyGroup()

    class ApricornGroup {
        var apricornHarvestersEnabled = true
        var bugTypeHarvestsApricorns = true
        var apricornHarvesters: MutableList<String> = mutableListOf("pikachu")
    }

    class IrrigationGroup {
        var cropIrrigatorsEnabled = true
        var waterTypeIrrigatesCrops = true
        var cropIrrigators: MutableList<String> = mutableListOf("pikachu")
    }

    class AmethystGroup {
        var amethystHarvestersEnabled = true
        var rockTypeHarvestsAmethyst = true
        var amethystHarvesters: MutableList<String> = mutableListOf("pikachu")
    }

    class TumblestoneGroup {
        var tumblestoneHarvestersEnabled = true
        var steelTypeHarvestsTumblestone = true
        var tumblestoneHarvesters: MutableList<String> = mutableListOf("pikachu")
    }

    class CropHarvestGroup {
        var cropHarvestersEnabled = true
        var grassTypeHarvestsCrops = true
        var cropHarvesters: MutableList<String> = mutableListOf("pikachu")
    }

    class BerriesGroup {
        var berryHarvestersEnabled = true
        var grassTypeHarvestsBerries = true
        var berryHarvesters: MutableList<String> = mutableListOf("pikachu")
    }

    class HoneyGroup {
        var honeyCollectorsEnabled = true
        var combeeLineCollectsHoney = true
        var honeyCollectors: MutableList<String> = mutableListOf("pikachu")
    }
}