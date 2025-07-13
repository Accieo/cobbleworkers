/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World
import java.util.UUID

/**
 * Pokémon navigation management.
 */
object CobbleworkersNavigationUtils {
    private data class Claim(val pokemonId: UUID, val claimTick: Long)
    private data class ExpiredTarget(val pos: BlockPos, val expiryTick: Long)
    private val pokemonToTarget = mutableMapOf<UUID, BlockPos>()
    private val targetedBlocks = mutableMapOf<BlockPos, Claim>()
    private val pokemonToPlayerTarget = mutableMapOf<UUID, UUID>()
    private val targetedPlayers = mutableMapOf<UUID, Claim>()
    private val recentlyExpiredTargets = mutableMapOf<BlockPos, ExpiredTarget>()
    private const val CLAIM_TIMEOUT_TICKS = 140L
    private const val EXPIRED_TARGET_TIMEOUT_TICKS = 300L

    /**
     * Checks if the Pokémon's bounding box intersects with the target block area.
     */
    fun isPokemonAtPosition(pokemonEntity: PokemonEntity, targetPos: BlockPos, offset: Double = 1.0): Boolean {
        val interactionHitbox = Box(targetPos).expand(offset)
        return pokemonEntity.boundingBox.intersects(interactionHitbox)
    }

    /**
     * Checks if the Pokémon is near enough to the player.
     */
    fun isPokemonNearPlayer(pokemonEntity: PokemonEntity, player: PlayerEntity, offset: Double = 1.0): Boolean {
        val interactionHitbox = player.boundingBox.expand(offset)
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
     * Commands the Pokémon entity to move towards the player's current position.
     */
    fun navigateToPlayer(pokemonEntity: PokemonEntity, player: PlayerEntity, speed: Double = 1.0) {
        pokemonEntity.navigation.startMovingTo(
            player.x,
            player.y,
            player.z,
            speed
        )
    }

    /**
     * Assigns a target block to a Pokémon and records the time.
     */
    fun claimTarget(pokemonId: UUID, target: BlockPos, world: World) {
        releaseExpiredClaims(world)
        releaseTarget(pokemonId, world)

        val immutableTarget = target.toImmutable()
        pokemonToTarget[pokemonId] = immutableTarget
        targetedBlocks[immutableTarget] = Claim(pokemonId, world.time)
    }

    /**
     * Overloaded claim target method to handle player claiming
     */
    fun claimTarget(pokemonId: UUID, player: PlayerEntity, world: World) {
        releaseExpiredClaims(world)
        releasePlayerTarget(pokemonId)

        pokemonToPlayerTarget[pokemonId] = player.uuid
        targetedPlayers[player.uuid] = Claim(pokemonId, world.time)
    }

    /**
     * Releases the target for a given Pokémon, making it available for others.
     */
    fun releaseTarget(pokemonId: UUID, world: World) {
        val releasedTarget = pokemonToTarget.remove(pokemonId)
        if (releasedTarget != null) {
            targetedBlocks.remove(releasedTarget)
            recentlyExpiredTargets[releasedTarget] = ExpiredTarget(releasedTarget, world.time)
        }
    }

    /**
     * Releases the player target for a given Pokémon.
     */
    fun releasePlayerTarget(pokemonId: UUID) {
        val playerId = pokemonToPlayerTarget.remove(pokemonId)
        if (playerId != null) {
            targetedPlayers.remove(playerId)
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
     * Gets the current player target UUID for a Pokémon.
     */
    fun getPlayerTarget(pokemonId: UUID, world: World): UUID? {
        releaseExpiredClaims(world)
        return pokemonToPlayerTarget[pokemonId]
    }

    /**
     * Checks if a specific block is targeted by any other Pokémon.
     */
    fun isTargeted(pos: BlockPos, world: World): Boolean {
        releaseExpiredClaims(world)
        return targetedBlocks.contains(pos)
    }

    /**
     * Checks if a player is targeted by any Pokémon.
     */
    fun isPlayerTargeted(player: PlayerEntity, world: World): Boolean {
        releaseExpiredClaims(world)
        return targetedPlayers.containsKey(player.uuid)
    }

    /**
     * Releases expired targets.
     */
    private fun releaseExpiredClaims(world: World) {
        val now = world.time
        val expiredPokemon = mutableListOf<UUID>()

        // Check block claims
        targetedBlocks.values.forEach { claim ->
            if (now - claim.claimTick > CLAIM_TIMEOUT_TICKS) expiredPokemon.add(claim.pokemonId)
        }

        // Check player claims
        targetedPlayers.values.forEach { claim ->
            if (now - claim.claimTick > CLAIM_TIMEOUT_TICKS) expiredPokemon.add(claim.pokemonId)
        }

        expiredPokemon.forEach {
            releaseTarget(it, world)
            releasePlayerTarget(it)
        }

        recentlyExpiredTargets.entries.removeIf { now - it.value.expiryTick > EXPIRED_TARGET_TIMEOUT_TICKS }
    }

    /**
     * Checks if a block is in the recently expired targets.
     */
    fun isRecentlyExpired(pos: BlockPos, world: World): Boolean {
        releaseExpiredClaims(world)
        return recentlyExpiredTargets.containsKey(pos)
    }
}