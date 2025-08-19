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
import com.cobblemon.mod.common.block.ApricornBlock
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.collections.any
import kotlin.collections.isNotEmpty
import kotlin.collections.isNullOrEmpty
import kotlin.collections.set
import kotlin.text.lowercase

/**
 * A worker job for a Pokémon to find, navigate to, and harvest mature apricorns.
 * Harvested items are deposited into the nearest available inventory.
 */
object ApricornHarvester : Worker {
    private val APRICORNS_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of("cobblemon", "apricorns"))
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val config = CobbleworkersConfigHolder.config.apricorn

    override val jobType: JobType = JobType.ApricornHarvester
    override val blockValidator: ((World, BlockPos) -> Boolean) = { world: World, pos: BlockPos ->
        val state = world.getBlockState(pos)
        state.isIn(APRICORNS_TAG)
    }

    /**
     * Determines if Pokémon is eligible to be an apricorn harvester.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.apricornHarvestersEnabled) return false

        return  CobbleworkersTypeUtils.isAllowedByType(config.typeHarvestsApricorns, pokemonEntity) || isDesignatedHarvester(pokemonEntity)
    }

    /**
     * Main logic loop for the apricorn harvester, executed each tick.
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
     * Handles logic for finding and harvesting an apricorn when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestApricorn = findClosestReadyApricorn(world, origin) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestApricorn, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestApricorn, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestApricorn, world)
            }
            return
        }

        if (currentTarget == closestApricorn) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestApricorn)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            harvestApricorn(world, closestApricorn, pokemonEntity)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Scans the pasture's block surrounding area for the closest mature apricorn.
     */
    private fun findClosestReadyApricorn(world: World, origin: BlockPos): BlockPos? {
        val possibleTargets = CobbleworkersCacheManager.getTargets(origin, jobType)
        if (possibleTargets.isEmpty()) return null

        return possibleTargets
            .filter { pos ->
                val state = world.getBlockState(pos)
                blockValidator(world, pos) && state.get(ApricornBlock.AGE) == ApricornBlock.MAX_AGE && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
            }
            .minByOrNull { it.getSquaredDistance(origin) }
    }

    /**
     * Executes the complete harvesting process for a single apricorn block
     */
    private fun harvestApricorn(world: World, apricornPos: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val apricornState = world.getBlockState(apricornPos)

        if (!apricornState.isIn(APRICORNS_TAG)) return

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, apricornPos.toCenterPos())
            .add(LootContextParameters.BLOCK_STATE, apricornState)
            .add(LootContextParameters.TOOL, ItemStack.EMPTY)
            .addOptional(LootContextParameters.THIS_ENTITY, pokemonEntity)

        val drops = apricornState.getDroppedStacks(lootParams)

        if (drops.isNotEmpty()) {
            heldItemsByPokemon[pokemonId] = drops
        }

        world.setBlockState(apricornPos, apricornState.with(ApricornBlock.AGE, 0), Block.NOTIFY_ALL)
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedHarvester(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.apricornHarvesters.any { it.lowercase() == speciesName }
    }
}