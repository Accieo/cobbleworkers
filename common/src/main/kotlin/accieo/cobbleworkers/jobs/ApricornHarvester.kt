/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import accieo.cobbleworkers.config.CobbleworkersConfigHolder
import accieo.cobbleworkers.interfaces.Worker
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.block.ApricornBlock
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.Block
import net.minecraft.inventory.Inventory
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
import kotlin.collections.forEach
import kotlin.collections.getOrPut
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
    private val searchRadius get() = config.searchRadius
    private val searchHeight get() = config.searchHeight

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
            handleDepositing(world, origin, pokemonEntity, heldItems)
        }
    }

    /**
     * Handles logic for finding and depositing items into an inventory when the Pokémon is holding items.
     * It will try multiple inventories nearby iteratively
     */
    private fun handleDepositing(world: World, origin: BlockPos, pokemonEntity: PokemonEntity, itemsToDeposit: List<ItemStack>) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val triedPositions = failedDepositLocations.getOrPut(pokemonId) { mutableSetOf() }
        val inventoryPos = CobbleworkersInventoryUtils.findClosestInventory(world, origin, searchRadius, searchHeight, triedPositions)

        if (inventoryPos == null) {
            // No (untried) inventories found, so we just drop the remaining items and reset.
            itemsToDeposit.forEach { stack -> Block.dropStack(world, pokemonEntity.blockPos, stack) }
            heldItemsByPokemon.remove(pokemonId)
            failedDepositLocations.remove(pokemonId)
            return
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, inventoryPos, 2.0)) {
            val inventory = world.getBlockEntity(inventoryPos) as? Inventory
            if (inventory == null) {
                // Block not an inventory, mark it as failed
                triedPositions.add(inventoryPos)
                return
            }

            val remainingDrops = CobbleworkersInventoryUtils.insertStacks(inventory, itemsToDeposit)

            if (remainingDrops.size == itemsToDeposit.size) {
                //  No change in stack size, so mark as failed
                triedPositions.add(inventoryPos)
            }

            if (remainingDrops.isNotEmpty()) {
                heldItemsByPokemon[pokemonId] = remainingDrops
            } else {
                heldItemsByPokemon.remove(pokemonId)
                failedDepositLocations.remove(pokemonId)
                pokemonEntity.navigation.stop()
            }
        } else {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, inventoryPos)
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
        return BlockPos.findClosest(origin, searchRadius, searchHeight) { pos ->
            val state = world.getBlockState(pos)
            state.isIn(APRICORNS_TAG) && state.get(ApricornBlock.AGE) == ApricornBlock.MAX_AGE && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
        }.orElse(null)
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