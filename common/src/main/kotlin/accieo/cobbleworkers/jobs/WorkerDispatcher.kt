/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import accieo.cobbleworkers.enums.JobType
import accieo.cobbleworkers.interfaces.Worker
import accieo.cobbleworkers.utilities.DeferredBlockScanner
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import kotlin.collections.filter
import kotlin.collections.forEach

object WorkerDispatcher {
    /**
     * Worker registry.
     */
    private val workers: List<Worker> = listOf(
        ApricornHarvester,
        AmethystHarvester,
        Archeologist,
        BerryHarvester,
        BrewingStandFuelGenerator,
        CropHarvester,
        CropIrrigator,
        DiveLooter,
        FireExtinguisher,
        FishingLootGenerator,
        FuelGenerator,
        GroundItemGatherer,
        Healer,
        HoneyCollector,
        LavaGenerator,
        MintHarvester,
        NetherwartHarvester,
        PickUpLooter,
        Scout,
        SnowGenerator,
        TumblestoneHarvester,
        WaterGenerator,
    )

    /**
     * Gathers all block validators from registered workers.
     */
    private val jobValidators: Map<JobType, (World, BlockPos) -> Boolean> = workers
        .mapNotNull { worker -> worker.blockValidator?.let { worker.jobType to it } }
        .toMap()

    /**
     * Ticks the deferred block scanning for a single pasture.
     * Called ONCE per pasture per tick.
     */
    fun tickAreaScan(world: World, pastureOrigin: BlockPos) {
        DeferredBlockScanner.tickPastureAreaScan(
            world,
            pastureOrigin,
            jobValidators
        )
    }

    /**
     * Ticks the action logic for a specific Pokémon.
     * Called ONCE per Pokémon in the pasture per tick.
     */
    fun tickPokemon(world: World, pastureOrigin: BlockPos, pokemonEntity: PokemonEntity) {
        workers
            .filter { it.shouldRun(pokemonEntity) }
            .forEach { it.tick(world, pastureOrigin, pokemonEntity) }
    }
}