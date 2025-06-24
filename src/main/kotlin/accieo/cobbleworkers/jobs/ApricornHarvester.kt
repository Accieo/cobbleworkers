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
import com.cobblemon.mod.common.api.types.ElementalTypes
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
import net.minecraft.util.math.Box
import net.minecraft.world.World

/**
 * A worker job for a Pokémon to find, navigate to, and harvest mature apricorns.
 * Harvested items are deposited into the nearest available inventory.
 */
object ApricornHarvester : Worker {
    private const val SEARCH_RADIUS = 8
    private const val VERTICAL_SEARCH_RANGE = 5
    private const val NAVIGATION_SPEED = 1.0
    private const val INTERACTION_BOX_OFFSET = 1.0
    private val APRICORNS_TAG = TagKey.of(RegistryKeys.BLOCK, Identifier.of("cobblemon", "apricorns"))

    /**
     * Determines if Pokémon is eligible to be an apricorn harvester.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     *
     * @return True if Pokémon is eligible, false otherwise.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        if (!config.apricornHarvestersEnabled) return false

        return isAllowedByBugType(pokemonEntity) || isDesignatedHarvester(pokemonEntity)
    }

    /**
     * Main logic loop for the apricorn harvester, executed each tick.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val targetPos = findClosestReadyApricorn(world, origin, pokemonEntity) ?: return

        if (isPokemonAtPosition(pokemonEntity, targetPos)) {
            harvestApricorn(world, targetPos, origin, pokemonEntity)
        } else {
            navigateTo(pokemonEntity, targetPos)
        }
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because it's a bug type
     * and bug type harvesting is enabled via config.
     *
     * @return True if bug type and bug type allowed, false otherwise.
     */
    private fun isAllowedByBugType(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        return config.bugTypeHarvestsApricorns && pokemonEntity.pokemon.types.any { it == ElementalTypes.BUG }
    }

    /**
     * Checks if the Pokémon qualifies as a harvester because its species is
     * explicitly listed in the config.
     *
     * @return True if is qualified harvester, false otherwise.
     */
    private fun isDesignatedHarvester(pokemonEntity: PokemonEntity): Boolean {
        val config = CobbleworkersConfigHolder.config
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.apricornHarvesters.any { it.lowercase() == speciesName }
    }

    /**
     * Scans the pasture's block surrounding area for the closest mature apricorn.
     *
     * @return BlockPos of the closest harvestable apricorn, or null if none are found.
     */
    private fun findClosestReadyApricorn(world: World, origin: BlockPos, pokemonEntity: PokemonEntity): BlockPos? {
        var closestPos: BlockPos? = null
        var closestDistance = Double.MAX_VALUE

        val searchArea = Box(origin).expand(SEARCH_RADIUS.toDouble(), VERTICAL_SEARCH_RANGE.toDouble(), SEARCH_RADIUS.toDouble())

        BlockPos.stream(searchArea).forEach { pos ->
           val state = world.getBlockState(pos)
            if (state.isIn(APRICORNS_TAG) && state.get(ApricornBlock.AGE) == ApricornBlock.MAX_AGE) {
                val distanceSq = pos.getSquaredDistance(pokemonEntity.pos)
                if (distanceSq < closestDistance) {
                    closestDistance = distanceSq
                    closestPos = pos.toImmutable()
                }
            }
        }

        return closestPos
    }

    /**
     * Checks if the Pokémon's bounding box intersects with the target block area.
     *
     * @return True if Pokémon's bounding box intersects, false otherwise.
     */
    private fun isPokemonAtPosition(pokemonEntity: PokemonEntity, targetPos: BlockPos): Boolean {
        val interactionHitbox = Box(targetPos).expand(INTERACTION_BOX_OFFSET)
        return pokemonEntity.boundingBox.intersects(interactionHitbox)
    }

    /**
     * Commands the Pokémon entity to move towards the target destination.
     */
    private fun navigateTo(pokemonEntity: PokemonEntity, targetPos: BlockPos) {
        pokemonEntity.navigation.startMovingTo(
            targetPos.x + 0.5,
            targetPos.y.toDouble(),
            targetPos.z + 0.5,
            NAVIGATION_SPEED
        )
    }

    /**
     * Executes the complete harvesting process for a single apricorn block
     */
    private fun harvestApricorn(world: World, apricornPos: BlockPos, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val apricornState = world.getBlockState(apricornPos)

        if (!apricornState.isIn(APRICORNS_TAG)) return

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, apricornPos.toCenterPos())
            .add(LootContextParameters.BLOCK_STATE, apricornState)
            .add(LootContextParameters.TOOL, ItemStack.EMPTY)
            .addOptional(LootContextParameters.THIS_ENTITY, pokemonEntity)

        val drops = apricornState.getDroppedStacks(lootParams)

        storeOrDropStacks(world, origin, apricornPos, drops)

        world.setBlockState(apricornPos, apricornState.with(ApricornBlock.AGE, 0), Block.NOTIFY_ALL)
    }

    /**
     * Attempts to store a list of ItemStacks in the nearest inventory.
     * Any stacks that cannot be fully stored are dropped in the world.
     */
    private fun storeOrDropStacks(world: World, origin: BlockPos, dropPos: BlockPos, drops: List<ItemStack>) {
        val inventoryPos = CobbleworkersInventoryUtils.findClosestInventory(world, origin, SEARCH_RADIUS)
        val inventory = inventoryPos?.let { world.getBlockEntity(it) as? Inventory }

        if (inventory != null) {
            // TODO: Make the Pokémon navigate towards the inventory before inserting
            val remainingDrops = drops.mapNotNull { stack ->
                val remainingStack = CobbleworkersInventoryUtils.insertStack(inventory, stack)
                if (!remainingStack.isEmpty) remainingStack else null
            }

            remainingDrops.forEach { stack -> Block.dropStack(world, dropPos, stack) }
        } else {
            // No inventory was found, drop all items at the given location
            drops.forEach { stack -> Block.dropStack(world, dropPos, stack) }
        }
    }
}