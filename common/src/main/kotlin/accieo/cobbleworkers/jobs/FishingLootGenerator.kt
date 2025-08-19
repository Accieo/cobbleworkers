/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import accieo.cobbleworkers.config.CobbleworkersConfigHolder
import accieo.cobbleworkers.enums.JobType
import accieo.cobbleworkers.interfaces.Worker
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.loot.LootTables
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.loot.context.LootContextTypes
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.collections.set
import kotlin.text.lowercase

object FishingLootGenerator : Worker {
    private val config = CobbleworkersConfigHolder.config.fishing
    private val cooldownTicks get() = config.fishingLootGenerationCooldownSeconds * 20L
    private val treasureChance get() = config.fishingLootTreasureChance
    private val lastGenerationTime = mutableMapOf<UUID, Long>()
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()

    override val jobType: JobType = JobType.FishingLootGenerator
    override val blockValidator: ((World, BlockPos) -> Boolean)? = null

    /**
     * Determines if Pokémon is eligible to be a worker.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.fishingLootGeneratorsEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeGeneratesFishingLoot, pokemonEntity) || isDesignatedGenerator(pokemonEntity)
    }

    /**
     * Main logic loop for the worker, executed each tick.
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        if (!pokemonEntity.isTouchingWater) return

        val heldItems = heldItemsByPokemon[pokemonId]

        if (heldItems.isNullOrEmpty()) {
            failedDepositLocations.remove(pokemonId)
            handleGeneration(world, origin, pokemonEntity)
        } else {
            CobbleworkersInventoryUtils.handleDepositing(world, origin, pokemonEntity, heldItems, failedDepositLocations, heldItemsByPokemon)
        }
    }

    /**
     * Handles logic for generating loot from fishing loot table.
     */
    fun handleGeneration(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val now = world.time
        val lastTime = lastGenerationTime[pokemonId] ?: 0L

        if (now - lastTime < cooldownTicks) {
            return
        }

        val treasureChancePercentage = treasureChance.toDouble() / 100
        val useTreasureTable = world.random.nextFloat() < treasureChancePercentage

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, origin.toCenterPos())
            .add(LootContextParameters.TOOL, ItemStack(Items.FISHING_ROD))
            .addOptional(LootContextParameters.THIS_ENTITY, pokemonEntity)
            .build(LootContextTypes.FISHING)

        val lootTable = if (useTreasureTable) {
            world.server.reloadableRegistries.getLootTable(LootTables.FISHING_TREASURE_GAMEPLAY)
        } else {
            world.server.reloadableRegistries.getLootTable(LootTables.FISHING_GAMEPLAY)
        }

        val drops = lootTable.generateLoot(lootParams)

        if (drops.isNotEmpty()) {
            lastGenerationTime[pokemonId] = now
            heldItemsByPokemon[pokemonId] = drops
        }
    }

    /**
     * Checks if the Pokémon qualifies as a generator because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedGenerator(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.fishingLootGenerators.any { it.lowercase() == speciesName }
    }
}