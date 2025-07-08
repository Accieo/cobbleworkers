/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import java.util.UUID

/**
 * Pokémon navigation management.
 */
object CobbleworkersNavigationUtils {
    private data class Claim(val pokemonId: UUID, val claimTick: Long)
    private val pokemonToTarget = mutableMapOf<UUID, BlockPos>()
    private val targetedBlocks = mutableMapOf<BlockPos, Claim>()
    private const val CLAIM_TIMEOUT_TICKS = 300L

    /**
     * Checks if the Pokémon's bounding box intersects with the target block area.
     */
    fun isPokemonAtPosition(pokemonEntity: PokemonEntity, targetPos: BlockPos, offset: Double = 1.0): Boolean {
        val interactionHitbox = Box(targetPos).expand(offset)
        return pokemonEntity.boundingBox.intersects(interactionHitbox)
    }

    /**
     * Commands the Pokémon entity to move towards the target destination.
     */
    fun navigateTo(pokemonEntity: PokemonEntity, targetPos: BlockPos, speed: Double = 1.0) {
        pokemonEntity.navigation.startMovingTo(
            targetPos.x + 0.5,
            targetPos.y.toDouble(),
            targetPos.z + 0.5,
            speed
        )
    }

    /**
     * Assigns a target block to a Pokémon and records the time.
     */
    fun claimTarget(pokemonId: UUID, target: BlockPos, world: World) {
        releaseExpiredClaims(world)
        releaseTarget(pokemonId)

        val immutableTarget = target.toImmutable()
        pokemonToTarget[pokemonId] = immutableTarget
        targetedBlocks[immutableTarget] = Claim(pokemonId, world.time)
    }

    /**
     * Releases the target for a given Pokémon, making it available for others.
     */
    fun releaseTarget(pokemonId: UUID) {
        val releasedTarget = pokemonToTarget.remove(pokemonId)
        if (releasedTarget != null) {
            targetedBlocks.remove(releasedTarget)
        }
    }

    /**
     * Gets the current target for a specific Pokémon.
     */
    fun getTarget(pokemonId: UUID, world: World): BlockPos? {
        releaseExpiredClaims(world)
        return pokemonToTarget[pokemonId]
    }

    /**
     * Checks if a specific block is targeted by any other Pokémon.
     */
    fun isTargeted(pos: BlockPos, world: World): Boolean {
        releaseExpiredClaims(world)
        return targetedBlocks.contains(pos)
    }

    /**
     * Releases expired targets.
     */
    private fun releaseExpiredClaims(world: World) {
        val now = world.time
        val expiredPokemon = mutableListOf<UUID>()

        targetedBlocks.values.forEach { claim ->
            if (now - claim.claimTick > CLAIM_TIMEOUT_TICKS) {
                expiredPokemon.add(claim.pokemonId)
            }
        }

        expiredPokemon.forEach { releaseTarget(it) }
    }
}