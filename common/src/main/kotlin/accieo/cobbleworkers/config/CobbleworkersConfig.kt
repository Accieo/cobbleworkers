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

    @ConfigEntry.Gui.CollapsibleObject
    var mints = MintsGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var lava = LavaGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var water = WaterGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var snow = SnowGroup()

    @ConfigEntry.Gui.CollapsibleObject
    var fishing = FishingGroup()

    class ApricornGroup {
        var apricornHarvestersEnabled = true
        var apricornHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsApricorns: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.BUG

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class IrrigationGroup {
        var cropIrrigatorsEnabled = true
        var cropIrrigators: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeIrrigatesCrops: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.WATER

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 2
    }

    class AmethystGroup {
        var amethystHarvestersEnabled = true
        var amethystHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsAmethyst: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.ROCK

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class TumblestoneGroup {
        var tumblestoneHarvestersEnabled = true
        var tumblestoneHarvesters: MutableList<String> = mutableListOf("pikachu")
        var shouldReplantTumblestone = true

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsTumblestone: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.STEEL

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class CropHarvestGroup {
        var cropHarvestersEnabled = true
        var cropHarvesters: MutableList<String> = mutableListOf("pikachu")
        var shouldReplantCrops = true

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsCrops: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.GRASS

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 3
    }

    class BerriesGroup {
        var berryHarvestersEnabled = true
        var berryHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsBerries: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.GRASS

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class HoneyGroup {
        var honeyCollectorsEnabled = true
        var combeeLineCollectsHoney = true
        var honeyCollectors: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsHoney: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.NONE

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class MintsGroup {
        var mintHarvestersEnabled = true
        var mintHarvesters: MutableList<String> = mutableListOf("pikachu")

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeHarvestsMints: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.FAIRY

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class LavaGroup {
        var lavaGeneratorsEnabled = true
        var lavaGenerators: MutableList<String> = mutableListOf("pikachu")
        var lavaGenerationCooldownSeconds: Long = 90

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeGeneratesLava: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.FIRE

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class WaterGroup {
        var waterGeneratorsEnabled = true
        var waterGenerators: MutableList<String> = mutableListOf("pikachu")
        var waterGenerationCooldownSeconds: Long = 90

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeGeneratesWater: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.WATER

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class SnowGroup {
        var snowGeneratorsEnabled = true
        var snowGenerators: MutableList<String> = mutableListOf("pikachu")
        var snowGenerationCooldownSeconds: Long = 90

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeGeneratesSnow: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.ICE

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }

    class FishingGroup {
        var fishingLootGeneratorsEnabled = true
        var fishingLootGenerators: MutableList<String> = mutableListOf("pikachu")
        var fishingLootGenerationCooldownSeconds: Long = 60
        var fishingLootTreasureChance = 1

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.DROPDOWN)
        var typeGeneratesFishingLoot: CobbleworkersConfigPokemonType = CobbleworkersConfigPokemonType.WATER

        /* Dangerous settings: It can highly impact server performance! */
        @ConfigEntry.BoundedDiscrete(min = 1, max = 10)
        var searchRadius = 8
        @ConfigEntry.BoundedDiscrete(min = 1, max = 8)
        var searchHeight = 5
    }
}