/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import accieo.cobbleworkers.config.CobbleworkersConfig
import accieo.cobbleworkers.integration.FarmersDelightBlocks
import com.cobblemon.mod.common.CobblemonBlocks
import com.cobblemon.mod.common.block.MedicinalLeekBlock
import com.cobblemon.mod.common.block.RevivalHerbBlock
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.BeetrootsBlock
import net.minecraft.block.Block
import net.minecraft.block.Blocks
import net.minecraft.block.CarrotsBlock
import net.minecraft.block.CaveVines
import net.minecraft.block.CaveVinesBodyBlock
import net.minecraft.block.CropBlock
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.PotatoesBlock
import net.minecraft.block.SweetBerryBushBlock
import net.minecraft.item.ItemStack
import net.minecraft.loot.context.LootContextParameterSet
import net.minecraft.loot.context.LootContextParameters
import net.minecraft.registry.Registries
import net.minecraft.server.world.ServerWorld
import net.minecraft.state.property.Properties.AGE_3
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import java.util.UUID

/**
 * Utility functions for crop related stuff
 */
object CobbleworkersCropUtils {
    private val validCropBlocks: MutableSet<Block> = mutableSetOf(
        Blocks.POTATOES,
        Blocks.BEETROOTS,
        Blocks.CARROTS,
        Blocks.WHEAT,
        Blocks.SWEET_BERRY_BUSH,
        Blocks.CAVE_VINES,
        Blocks.CAVE_VINES_PLANT,
        CobblemonBlocks.REVIVAL_HERB,
        CobblemonBlocks.MEDICINAL_LEEK,
        CobblemonBlocks.VIVICHOKE_SEEDS
    )

    /**
     * Add crop integrations dynamically at runtime
     */
    fun addCompatibility(externalBlocks: Set<Block>) {
        validCropBlocks.addAll(externalBlocks)
    }

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
            state.block in validCropBlocks && isMatureCrop(world, pos) && !CobbleworkersNavigationUtils.isRecentlyExpired(pos, world)
        }.orElse(null)
    }

    /**
     * Executes the complete harvesting processes for a single crop
     */
    fun harvestCrop(world: World, blockPos: BlockPos, pokemonEntity: PokemonEntity, pokemonHeldItems:  MutableMap<UUID, List<ItemStack>>, config: CobbleworkersConfig.CropHarvestGroup) {
        val blockState = world.getBlockState(blockPos)
        if (blockState.block !in validCropBlocks) return

        val lootParams = LootContextParameterSet.Builder(world as ServerWorld)
            .add(LootContextParameters.ORIGIN, blockPos.toCenterPos())
            .add(LootContextParameters.BLOCK_STATE, blockState)
            .add(LootContextParameters.TOOL, ItemStack.EMPTY)
            .addOptional(LootContextParameters.THIS_ENTITY, pokemonEntity)

        val drops = blockState.getDroppedStacks(lootParams)

        if (drops.isNotEmpty()) {
            pokemonHeldItems[pokemonEntity.pokemon.uuid] = drops
        }

        val block = blockState.block
        val blockId = Registries.BLOCK.getId(block).path

        /** Integration stuff **/
        val isFarmersDelightCrop = blockId in FarmersDelightBlocks.ALL
        val isTomato = blockId == FarmersDelightBlocks.TOMATOES
        val isRice = blockId == FarmersDelightBlocks.RICE_PANICLES
        val isMushroomColony = blockId in FarmersDelightBlocks.MUSHROOMS

        val newState = when {
            config.shouldReplantCrops ->
                when {
                    /** Farmer's delight **/
                    isRice && blockState.contains(AGE_3) -> Blocks.AIR.defaultState
                    isTomato && blockState.contains(AGE_3) -> blockState.with(AGE_3, 0)
                    isMushroomColony && blockState.contains(AGE_3) -> blockState.with(AGE_3, 0)
                    isFarmersDelightCrop && blockState.contains(CropBlock.AGE) -> blockState.with(CropBlock.AGE, 0)
                    /** Vanilla **/
                    block == Blocks.POTATOES -> blockState.with(PotatoesBlock.AGE, 0)
                    block == Blocks.BEETROOTS -> blockState.with(BeetrootsBlock.AGE, 0)
                    block == Blocks.CARROTS -> blockState.with(CarrotsBlock.AGE, 0)
                    block == Blocks.WHEAT -> blockState.with(CropBlock.AGE, 0)
                    block == Blocks.SWEET_BERRY_BUSH -> blockState.with(SweetBerryBushBlock.AGE, 1)
                    block == Blocks.CAVE_VINES -> blockState.with(CaveVinesBodyBlock.BERRIES, false)
                    block == Blocks.CAVE_VINES_PLANT -> blockState.with(CaveVinesBodyBlock.BERRIES, false)
                    /** Cobblemon **/
                    block == CobblemonBlocks.REVIVAL_HERB -> blockState.with(RevivalHerbBlock.AGE, RevivalHerbBlock.MIN_AGE)
                    block == CobblemonBlocks.MEDICINAL_LEEK -> blockState.with(MedicinalLeekBlock.AGE, 0)
                    block == CobblemonBlocks.VIVICHOKE_SEEDS -> Blocks.AIR.defaultState
                    else -> return
                }
            block == Blocks.SWEET_BERRY_BUSH -> blockState.with(SweetBerryBushBlock.AGE, 1)
            block == Blocks.CAVE_VINES -> blockState.with(CaveVinesBodyBlock.BERRIES, false)
            block == Blocks.CAVE_VINES_PLANT -> blockState.with(CaveVinesBodyBlock.BERRIES, false)
            else -> Blocks.AIR.defaultState
        }

        world.setBlockState(blockPos, newState)
    }

    /**
     * Checks if the crop is its mature state
     */
    private fun isMatureCrop(world: World, pos: BlockPos): Boolean {
        val state = world.getBlockState(pos)
        val block = state.block
        val blockId = Registries.BLOCK.getId(block).path

        return when {
            block is CropBlock -> block.getAge(state) == block.maxAge
            block is CaveVines -> state.get(CaveVinesBodyBlock.BERRIES)
            block is SweetBerryBushBlock -> state.get(SweetBerryBushBlock.AGE) == SweetBerryBushBlock.MAX_AGE
            blockId in FarmersDelightBlocks.MUSHROOMS && state.contains(AGE_3) -> state.get(AGE_3) == 3
            else -> false
        }
    }
}