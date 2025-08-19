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
import net.minecraft.block.Blocks
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

/**
 * A worker job for a Pokémon to find, navigate to, and harvest fully grown amethysts.
 * Harvested items are deposited into the nearest available inventory.
 */
object AmethystHarvester : Worker {
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val config = CobbleworkersConfigHolder.config.amethyst

    override val jobType: JobType = JobType.AmethystHarvester
    override val blockValidator: ((World, BlockPos) -> Boolean) = { world: World, pos: BlockPos ->
        val state = world.getBlockState(pos)
        state.block == Blocks.AMETHYST_CLUSTER
    }

    /**
     * Determines if Pokémon is eligible to be an amethyst harvester.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.amethystHarvestersEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeHarvestsAmethyst, pokemonEntity) || isDesignatedHarvester(pokemonEntity)
    }

    /**
     * Main logic loop for the amethyst harvester, executed each tick.
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
            CobbleworkersInventoryUtils.handleDepositing(world, origin, pokemonEntity, heldItems, failedDepositLocations, heldItemsByPokemon)
        }
    }

    /**
     * Handles logic for finding and harvesting an amethyst cluster when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestAmethyst = findClosestAmethystCluster(world, origin) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestAmethyst, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestAmethyst, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestAmethyst, world)
            }
            return
        }

        if (currentTarget == closestAmethyst) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestAmethyst)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget, 1.5)) {
            harvestAmethystCluster(world, closestAmethyst, pokemonEntity)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Scans the pasture's block surrounding area for the closest amethyst cluster.
     */
    private fun findClosestAmethystCluster(world: World, origin: BlockPos): BlockPos? {
        val possibleTargets = CobbleworkersCacheManager.getTargets(origin, jobType)
        if (possibleTargets.isEmpty()) return null

        return possibleTargets
            .filter { pos ->
                blockValidator(world, pos) && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
            }
            .minByOrNull { it.getSquaredDistance(origin) }
    }

    /**
     * Executes the complete harvesting process for a single amethyst cluster
     */
    private fun harvestAmethystCluster(world: World, amethystPos: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val amethystState = world.getBlockState(amethystPos)
        if (amethystState.block != Blocks.AMETHYST_CLUSTER) return

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, amethystPos.toCenterPos())
            .add(LootContextParameters.BLOCK_STATE, amethystState)
            .add(LootContextParameters.TOOL, ItemStack.EMPTY)
            .addOptional(LootContextParameters.THIS_ENTITY, pokemonEntity)

        val drops = amethystState.getDroppedStacks(lootParams)

        if (drops.isNotEmpty()) {
            heldItemsByPokemon[pokemonId] = drops
        }

        world.setBlockState(amethystPos, Blocks.AIR.defaultState)
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedHarvester(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.amethystHarvesters.any { it.lowercase() == speciesName }
    }
}