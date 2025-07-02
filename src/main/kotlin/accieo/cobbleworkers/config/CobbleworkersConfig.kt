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
        var apricornHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsApricorns: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.BUG
    }

    class IrrigationGroup {
        var cropIrrigatorsEnabled = true
        var cropIrrigators: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeIrrigatesCrops: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.WATER
    }

    class AmethystGroup {
        var amethystHarvestersEnabled = true
        var amethystHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsAmethyst: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.ROCK
    }

    class TumblestoneGroup {
        var tumblestoneHarvestersEnabled = true
        var tumblestoneHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsTumblestone: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.STEEL
    }

    class CropHarvestGroup {
        var cropHarvestersEnabled = true
        var cropHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsCrops: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.GRASS
    }

    class BerriesGroup {
        var berryHarvestersEnabled = true
        var berryHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsBerries: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.GRASS
    }

    class HoneyGroup {
        var honeyCollectorsEnabled = true
        var combeeLineCollectsHoney = true
        var honeyCollectors: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsHoney: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.NONE
    }
}