/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.jobs

import accieo.cobbleworkers.Cobbleworkers
import accieo.cobbleworkers.cache.CobbleworkersCacheManager
import accieo.cobbleworkers.config.CobbleworkersConfigHolder
import accieo.cobbleworkers.enums.JobType
import accieo.cobbleworkers.interfaces.Worker
import accieo.cobbleworkers.utilities.CobbleworkersInventoryUtils
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import accieo.cobbleworkers.utilities.CobbleworkersTypeUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.MapColorComponent
import net.minecraft.entity.ItemEntity
import net.minecraft.item.FilledMapItem
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.item.map.MapDecorationTypes
import net.minecraft.item.map.MapState
import net.minecraft.registry.RegistryKey
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.entry.RegistryEntry
import net.minecraft.registry.entry.RegistryEntryList
import net.minecraft.server.world.ServerWorld
import net.minecraft.text.Text
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import net.minecraft.world.gen.structure.Structure
import java.util.UUID
import kotlin.text.lowercase

object Scout : Worker {
    private val heldItemsByPokemon = mutableMapOf<UUID, List<ItemStack>>()
    private val failedDepositLocations = mutableMapOf<UUID, MutableSet<BlockPos>>()
    private val config get() = CobbleworkersConfigHolder.config.scouts
    private val generalConfig = CobbleworkersConfigHolder.config.general
    private val searchRadius get() = generalConfig.searchRadius
    private val searchHeight get() = generalConfig.searchHeight

    override val jobType: JobType = JobType.Scout
    override val blockValidator: ((World, BlockPos) -> Boolean)? = null

    /**
     * Determines if Pokémon is eligible to be a healer.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.scoutsEnabled) return false

        return CobbleworkersTypeUtils.isAllowedByType(config.typeScouts, pokemonEntity) || isDesignatedScout(pokemonEntity) || doesPokemonKnowFly(pokemonEntity)
    }

    /**
     * Main logic loop for the scout, executed each tick.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val heldItems = heldItemsByPokemon[pokemonId]

        // TODO: Add generation cooldown

        if (heldItems.isNullOrEmpty()) {
            failedDepositLocations.remove(pokemonId)
            handleGathering(world, origin, pokemonEntity)
        } else {
            CobbleworkersInventoryUtils.handleDepositing(world, origin, pokemonEntity, heldItems, failedDepositLocations, heldItemsByPokemon)
        }
    }

    /**
     * Finds the closest item on the ground and returns its position and entity.
     */
    private fun findClosestItem(world: World, origin: BlockPos): Pair<BlockPos, ItemEntity>? {
        val searchArea = Box(origin).expand(searchRadius.toDouble(), searchHeight.toDouble(), searchRadius.toDouble())
        val items = world.getEntitiesByClass(ItemEntity::class.java, searchArea) { true }
        return items
            .filter { item -> item.isOnGround && item.stack.item == Items.MAP }
            .minByOrNull { item -> item.squaredDistanceTo(origin.x + 0.5, origin.y + 0.5, origin.z + 0.5) }
            ?.let { it.blockPos to it }
    }

    /**
     * Locate random structure in already generated chunks and create a map to it.
     */
    private fun createStructureMap(world: ServerWorld, origin: BlockPos): ItemStack? {
        val structures = CobbleworkersCacheManager.getStructures(world, config.useAllStructures, config.structureTags)
        if (structures.isEmpty()) return null

        val selectedId = structures.random()
        val searchResult = locateStructure(world, selectedId, origin) ?: return null
        val structurePos = searchResult.first
        val structureEntry = searchResult.second

        val map = FilledMapItem.createMap(
            world,
            structurePos.x,
            structurePos.z,
            2.toByte(),
            true,
            true
        )

        MapState.addDecorationsNbt(
            map,
            structurePos,
            "target",
            MapDecorationTypes.RED_X
        )

        val mapKey = if (config.mapNameIsHidden) {
            Text.of("Scout's map")
        } else {
            Text.of(cleanMapName(structureEntry.idAsString))
        }

        map.set(DataComponentTypes.CUSTOM_NAME, mapKey)
        map.set(DataComponentTypes.MAP_COLOR, MapColorComponent(0xCC84ED))

        return map
    }

    private fun locateStructure(world: ServerWorld, structure: Identifier, pos: BlockPos): com.mojang.datafixers.util.Pair<BlockPos, RegistryEntry<Structure>>? {
        val structureRegistry = world.server.registryManager.get(RegistryKeys.STRUCTURE)

        val entry = structureRegistry
            .getEntry(RegistryKey.of(RegistryKeys.STRUCTURE, structure))
            .orElse(null) ?: return null

        val entryList = RegistryEntryList.of(entry)

        val chunkGenerator = world.chunkManager.chunkGenerator
        return chunkGenerator.locateStructure(world, entryList, pos, 100, false)
    }

    /**
     * Handles logic for finding and gathering an item on the ground.
     */
    private fun handleGathering(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val (closestItemPos, closestItem) = findClosestItem(world, origin) ?: return

        val currentTarget = CobbleworkersNavigationUtils.getTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isTargeted(closestItemPos, world) && !CobbleworkersNavigationUtils.isRecentlyExpired(closestItemPos, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestItemPos, world)
            }
            return
        }

        if (currentTarget == closestItemPos) {
            CobbleworkersNavigationUtils.navigateTo(pokemonEntity, closestItemPos)
        }

        if (CobbleworkersNavigationUtils.isPokemonAtPosition(pokemonEntity, currentTarget)) {
            if (closestItem.stack.item == Items.MAP) {
                val singleItem = closestItem.stack.split(1)

                if (closestItem.stack.isEmpty) {
                    closestItem.discard()
                }

                val map = createStructureMap(world as ServerWorld, origin)
                if (map != null) {
                    heldItemsByPokemon[pokemonId] = listOf(map)
                } else {
                    heldItemsByPokemon[pokemonId] = listOf(singleItem)
                }
            }
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
        }
    }

    /**
     * Checks if the Pokémon qualifies as a scout because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedScout(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.scouts.any { it.lowercase() == speciesName }
    }

    /**
     * Checks if the Pokémon qualifies as a scout because of its moves.
     */
    private fun doesPokemonKnowFly(pokemonEntity: PokemonEntity): Boolean {
        return pokemonEntity.pokemon.moveSet.getMoves().any { it.name == "fly" }
    }

    /**
     * Cleans up registry structure names.
     */
    private fun cleanMapName(rawName: String): String {
        return rawName
            .substringAfterLast(":")
            .substringAfterLast("/")
            .replace("_", " ")
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }
}