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
import com.cobblemon.mod.common.block.BerryBlock
import com.cobblemon.mod.common.block.entity.BerryBlockEntity
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import com.cobblemon.mod.common.util.toBlockPos
import net.minecraft.block.Block
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID
import kotlin.text.lowercase

/**
 * A worker job for a Pokémon to find, navigate to, and harvest fully grown berries.
 * Harvested items are deposited into the nearest available inventory.
 */
object BerryHarvester : Worker {
    private val BERRIES_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of("cobblemon", "berries"))
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val config = CobbleworkersConfigHolder.config.berries

    override val jobType: JobType = JobType.BerryHarvester
    override val blockValidator: ((World, BlockPos) -> Boolean) = { world: World, pos: BlockPos ->
        val state = world.getBlockState(pos)
        state.isIn(BERRIES_TAG)
    }

    /**
     * Determines if Pokémon is eligible to be a berry harvester.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.berryHarvestersEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeHarvestsBerries, pokemonEntity) || isDesignatedHarvester(pokemonEntity)
    }

    /**
     * Main logic loop for the berry harvester, executed each tick.
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
     * Handles logic for finding and harvesting a berry when the Pokémon is not holding items.
     */
    private fun handleHarvesting(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val closestBerry = findClosestReadyBerry(world, origin) ?: return
        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestBerry, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestBerry, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestBerry, world)
            }
            return
        }

        if (currentTarget == closestBerry) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestBerry)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            harvestBerry(world, closestBerry, pokemonEntity)
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Scans the pasture's block surrounding area for the closest mature berry.
     */
    private fun findClosestReadyBerry(world: World, origin: BlockPos): BlockPos? {
        val possibleTargets = CobbleworkersCacheManager.getTargets(origin, jobType)
        if (possibleTargets.isEmpty()) return null

        return possibleTargets
            .filter { pos ->
                val state = world.getBlockState(pos)
                blockValidator(world, pos) && state.get(BerryBlock.AGE) == BerryBlock.FRUIT_AGE && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
            }
            .minByOrNull { it.getSquaredDistance(origin) }
    }

    /**
     * Executes the complete harvesting process for a single berry block
     */
    private fun harvestBerry(world: World, berryPos: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val berryState = world.getBlockState(berryPos)
        val berryBlockEntity = world.getBlockEntity(berryPos) as? BerryBlockEntity ?: return

        if (!berryState.isIn(BERRIES_TAG)) return

        val drops = berryBlockEntity.harvest(world, berryState, berryPos, null)

        if (drops.isNotEmpty()) {
            heldItemsByPokemon[pokemonId] = drops as List<ItemStack>
        }

        world.setBlockState(berryPos, berryState.with(BerryBlock.AGE, BerryBlock.MATURE_AGE), Block.NOTIFY_ALL)
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedHarvester(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.berryHarvesters.any { it.lowercase() == speciesName }
    }
}