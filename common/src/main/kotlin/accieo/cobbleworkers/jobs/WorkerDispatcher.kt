/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.collections.filter
import kotlin.collections.forEach

object WorkerDispatcher {
    private val workers = listOf(
        ApricornHarvester,
        AmethystHarvester,
        TumblestoneHarvester,
        CropIrrigator,
        CropHarvester,
        BerryHarvester,
        HoneyCollector,
        MintHarvester,
        LavaGenerator,
        WaterGenerator,
        SnowGenerator,
        FishingLootGenerator,
        PickUpLooter,
        DiveLooter,
        GroundItemGatherer,
        NetherwartHarvester
    )

    fun tickAll(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        workers
            .filter { it.shouldRun(pokemonEntity) }
            .forEach { it.tick(world, origin, pokemonEntity) }
    }
}