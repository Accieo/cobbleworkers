/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import accieo.cobbleworkers.cache.CobbleworkersCacheManager
import accieo.cobbleworkers.config.CobbleworkersConfigHolder
import accieo.cobbleworkers.enums.JobType
import accieo.cobbleworkers.interfaces.Worker
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.loot.context.LootContextTypes
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

object Archeologist : Worker {
    private val config = CobbleworkersConfigHolder.config.archeology
    private val cooldownTicks get() = config.archeologyLootingCooldownSeconds * 20L
    private val lastGenerationTime = mutableMapOf<UUID, Long>()
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val validBlocks: Set<Block> = setOf(
        Blocks.DIRT,
        Blocks.GRAVEL,
        Blocks.MUD,
        Blocks.COARSE_DIRT,
        Blocks.ROOTED_DIRT,
    )

    override val jobType: JobType = JobType.Archeologist
    override val blockValidator: ((World, BlockPos) -> Boolean) = { world: World, pos: BlockPos ->
        val state = world.getBlockState(pos)
        val aboveState = world.getBlockState(pos.up())
        state.block in validBlocks && aboveState.isAir
    }

    /**
     * Determines if Pokémon is eligible to be an archeologist.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.archeologistsEnabled) return false

        return  CobbleworkersTypeUtils.isAllowedByType(config.typeDoesArcheology, pokemonEntity) || isDesignatedHarvester(pokemonEntity)
    }

    /**
     * Main logic loop for the archeologist, executed each tick.
     * Delegates to state handlers handleHarvesting and handleDepositing
     * to manage the current task of the Pokémon.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val heldItems = heldItemsByPokemon[pokemonId]

        if (heldItems.isNullOrEmpty()) {
            failedDepositLocations.remove(pokemonId)
            handleHarvesting(world, origin, pokemonEntity)
        } else {
            CobbleworkersInventoryUtils.handleDepositing(world, origin, pokemonEntity, heldItems,
                failedDepositLocations,
                heldItemsByPokemon
            )
        }
    }

    /**
     * Handles logic for archeologist when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestBlock = findClosestArcheologySpot(world, origin) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestBlock, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestBlock, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestBlock, world)
            }
            return
        }

        if (currentTarget == closestBlock) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestBlock)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            handleGeneration(world, closestBlock, pokemonEntity)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Scans the pasture's block surrounding area for the closest archeology spot.
     */
    private fun findClosestArcheologySpot(world: World, origin: BlockPos): BlockPos? {
        val possibleTargets = CobbleworkersCacheManager.getTargets(origin, jobType)
        if (possibleTargets.isEmpty()) return null

        return possibleTargets
            .filter { pos ->
                blockValidator(world, pos) && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
            }
            .minByOrNull { it.getSquaredDistance(origin) }
    }

    /**
     * Handles logic for generating loot from cobblemon loot table.
     */
    fun handleGeneration(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val now = world.time
        val lastTime = lastGenerationTime[pokemonId] ?: 0L

        if (now - lastTime < cooldownTicks) {
            return
        }

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, origin.toCenterPos())
            .add(LootContextParameters.THIS_ENTITY, pokemonEntity)
            .build(LootContextTypes.CHEST)

        val lootTables = config.lootTables.mapNotNull { Identifier.tryParse(it) }

        if (lootTables.isEmpty()) return

        val selectedId = lootTables.random()

        val lootTableKey = RegistryKey.of(RegistryKeys.LOOT_TABLE, selectedId)
        val lootTable = world.server.reloadableRegistries.getLootTable(lootTableKey)
        val drops = lootTable.generateLoot(lootParams)

        if (drops.isNotEmpty()) {
            lastGenerationTime[pokemonId] = now
            heldItemsByPokemon[pokemonId] = drops
        }
    }

    /**
     * Checks if the Pokémon qualifies as an archeologist because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedHarvester(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.archeologists.any { it.lowercase() == speciesName }
    }
}