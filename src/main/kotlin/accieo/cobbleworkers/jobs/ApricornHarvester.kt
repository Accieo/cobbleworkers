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
import com.cobblemon.mod.common.api.types.ElementalTypes
import com.cobblemon.mod.common.block.ApricornBlock
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.block.BlockState
import net.minecraft.registry.RegistryKeys
import net.minecraft.registry.tag.TagKey
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

object ApricornHarvester : Worker {
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!CobbleworkersConfigHolder.config.apricornHarvestersEnabled) return false

        val bugTypeEnabled = CobbleworkersConfigHolder.config.bugTypeHarvestsApricorns
        val isBugType = pokemonEntity.pokemon.types.any { it == ElementalTypes.BUG }
        val isApricornHarvester = CobbleworkersConfigHolder.config.apricornHarvesters
            .map { it.lowercase() }
            .contains(pokemonEntity.pokemon.species.translatedName.string.lowercase())

        return (bugTypeEnabled && isBugType) || isApricornHarvester
    }

    override fun tick(
        world: World,
        origin: BlockPos,
        pokemonEntity: PokemonEntity
    ) {
        val radius = 8
        var closestPos: BlockPos? = null
        var closestDistance = Double.Companion.MAX_VALUE
        val apricornsTag = TagKey.of(RegistryKeys.BLOCK, Identifier.of("cobblemon", "apricorns"))

        for (pos in Iterable {
            BlockPos.stream(origin.add(-radius, -5, -radius), origin.add(radius, 5, radius)).iterator()
        }) {
            val state = world.getBlockState(pos)

            if (state.isIn(apricornsTag)) {
                val age = state.get(ApricornBlock.AGE)
                if (age == ApricornBlock.MAX_AGE) {
                    val distance = pokemonEntity.squaredDistanceTo(
                        pos.x.toDouble(),
                        pos.y.toDouble(),
                        pos.z.toDouble()
                    )
                    if (distance < closestDistance) {
                        closestDistance = distance
                        closestPos = pos.toImmutable()
                    }
                }
            }
        }

        if (closestPos != null) {
            val buffer = 1.0
            val apricornHitbox = Box(closestPos).expand(buffer)

            if (pokemonEntity.boundingBox.intersects(apricornHitbox)) {
                val apricorn = world.getBlockState(closestPos)
                if (apricorn.isIn(apricornsTag)) {
                    val apricornBlock = apricorn.block as ApricornBlock
                    val newState = apricornBlock.harvest(world, apricorn, closestPos)
                    world.setBlockState(closestPos, newState, 3)
                }
                return
            }

            // TODO: Move apricorns to inventories

            val targetX = closestPos.x + 0.5
            val targetY = closestPos.y.toDouble()
            val targetZ = closestPos.z + 0.5

            pokemonEntity.getNavigation().startMovingTo(targetX, targetY, targetZ, 1.0)
        }
    }
}