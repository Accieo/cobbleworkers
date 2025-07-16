/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import accieo.cobbleworkers.config.CobbleworkersConfig
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.MedicinalLeekBlock
import com.cobblemon.mod.common.block.RevivalHerbBlock
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.BeetrootsBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.CarrotsBlock
import net.minecraft.block.CropBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.PotatoesBlock
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.server.world.ServerWorld
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import java.util.UUID

/**
 * Utility functions for crop related stuff
 */
object CobbleworkersCropUtils {
    private val VALID_CROP_BLOCKS: Set<Block> = setOf(
        Blocks.POTATOES,
        Blocks.BEETROOTS,
        Blocks.CARROTS,
        Blocks.WHEAT,
        CobblemonBlocks.REVIVAL_HERB,
        CobblemonBlocks.MEDICINAL_LEEK,
        CobblemonBlocks.VIVICHOKE_SEEDS
    )

    /**
     * Finds the closest dry farmland
     */
    fun findClosestFarmland(world: World, origin: BlockPos, searchRadius: Int, searchHeight: Int): BlockPos? {
        return BlockPos.findClosest(origin, searchRadius, searchHeight) { pos ->
            val state = world.getBlockState(pos)
            state.block == Blocks.FARMLAND && state.get(FarmlandBlock.MOISTURE) <= 2 && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
        }.orElse(null)
    }

    /**
     * Finds the closest crop
     */
    fun findClosestCrop(world: World, origin: BlockPos, searchRadius: Int, searchHeight: Int): BlockPos? {
        return BlockPos.findClosest(origin, searchRadius, searchHeight) { pos ->
            val state = world.getBlockState(pos)
            state.block in VALID_CROP_BLOCKS && isMatureCrop(world, pos) && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
        }.orElse(null)
    }

    /**
     * Executes the complete harvesting processes for a single crop
     */
    fun harvestCrop(world: World, blockPos: BlockPos, pokemonEntity: PokemonEntity, pokemonHeldItems:  MutableMap<UUID, List<ItemStack>>, config: CobbleworkersConfig.CropHarvestGroup) {
        val blockState = world.getBlockState(blockPos)
        if (blockState.block !in VALID_CROP_BLOCKS) return

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, blockPos.toCenterPos())
            .add(LootContextParameters.BLOCK_STATE, blockState)
            .add(LootContextParameters.TOOL, ItemStack.EMPTY)
            .addOptional(LootContextParameters.THIS_ENTITY, pokemonEntity)

        val drops = blockState.getDroppedStacks(lootParams)

        if (drops.isNotEmpty()) {
            pokemonHeldItems[pokemonEntity.pokemon.uuid] = drops
        }

        val newState = if (config.shouldReplantCrops) {
            when (blockState.block) {
                Blocks.POTATOES -> blockState.with(PotatoesBlock.AGE, 0)
                Blocks.BEETROOTS -> blockState.with(BeetrootsBlock.AGE, 0)
                Blocks.CARROTS -> blockState.with(CarrotsBlock.AGE, 0)
                Blocks.WHEAT -> blockState.with(CropBlock.AGE, 0)
                CobblemonBlocks.REVIVAL_HERB -> blockState.with(RevivalHerbBlock.AGE, RevivalHerbBlock.MIN_AGE)
                CobblemonBlocks.MEDICINAL_LEEK -> blockState.with(MedicinalLeekBlock.AGE, 0)
                CobblemonBlocks.VIVICHOKE_SEEDS -> Blocks.AIR.defaultState
                else -> return
            }
        } else {
            Blocks.AIR.defaultState
        }

        world.setBlockState(blockPos, newState)
    }

    /**
     * Checks if the crop is its mature state
     */
    private fun isMatureCrop(world: World, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        val block = state.block

        return when (block) {
            is CropBlock -> block.getAge(state) == block.maxAge
            else -> false
        }
    }
}