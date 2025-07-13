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
import accieo.cobbleworkers.utilities.CobbleworkersNavigationUtils
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity
import net.minecraft.entity.effect.StatusEffectInstance
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.world.World

object Healer : Worker {
    private val VALID_SPECIES: Set<String> = setOf("happiny", "chansey", "blissey")
    private val config = CobbleworkersConfigHolder.config.healing
    private val searchRadius get() = config.searchRadius
    private val searchHeight get() = config.searchHeight

    /**
     * Determines if Pokémon is eligible to be a healer.
     * NOTE: This is used to prevent running the tick method unnecessarily.
     */
    override fun shouldRun(pokemonEntity: PokemonEntity): Boolean {
        if (!config.healersEnabled) return false

        return isDesignatedHealer(pokemonEntity) || isAllowedBySpecies(pokemonEntity) || doesPokemonKnowHealingMove(pokemonEntity)
    }

    /**
     * Main logic loop for the healer, executed each tick.
     *
     * NOTE: Origin refers to the pasture's block position.
     */
    override fun tick(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        handleHealing(world, origin, pokemonEntity)
    }

    /**
     * Finds players nearby.
     */
    private fun findNearbyPlayers(world: World, origin: BlockPos): List<PlayerEntity> {
        val searchBox = Box(origin).expand(searchRadius.toDouble(), searchHeight.toDouble(), searchRadius.toDouble())
        return world.getEntitiesByClass(PlayerEntity::class.java, searchBox) { true }
    }

    /**
     * Handles logic for finding a player and healing them.
     */
    private fun handleHealing(world: World, origin: BlockPos, pokemonEntity: PokemonEntity) {
        val pokemonId = pokemonEntity.pokemon.uuid
        val nearbyPlayers = findNearbyPlayers(world, origin)
        if (nearbyPlayers.isEmpty()) {
            CobbleworkersNavigationUtils.releaseTarget(pokemonId, world)
            return
        }

        val closestPlayer = nearbyPlayers.minByOrNull { it.squaredDistanceTo(pokemonEntity.pos) } ?: return
        if (doesPlayerHaveRegen(closestPlayer) || closestPlayer.health == closestPlayer.maxHealth) return

        val currentTarget = CobbleworkersNavigationUtils.getPlayerTarget(pokemonId, world)

        if (currentTarget == null) {
            if (!CobbleworkersNavigationUtils.isPlayerTargeted(closestPlayer, world)) {
                CobbleworkersNavigationUtils.claimTarget(pokemonId, closestPlayer, world)
            }
            return
        }

        if (currentTarget == closestPlayer.uuid) {
            CobbleworkersNavigationUtils.navigateToPlayer(pokemonEntity, closestPlayer)
        }

        if (CobbleworkersNavigationUtils.isPokemonNearPlayer(pokemonEntity, closestPlayer)) {
            if (!doesPlayerHaveRegen(closestPlayer)) {
                closestPlayer.addStatusEffect(
                    StatusEffectInstance(
                        StatusEffects.REGENERATION,
                        config.regenDurationSeconds * 20,
                        config.regenAmplifier
                    )
                )
            }
            CobbleworkersNavigationUtils.releasePlayerTarget(pokemonId)
        }
    }

    private fun doesPlayerHaveRegen(player: PlayerEntity): Boolean {
        return player.hasStatusEffect(StatusEffects.REGENERATION)
    }

    /**
     * Checks if the Pokémon qualifies as a healer because of its species,
     * and it is allowed in the config.
     */
    private fun isAllowedBySpecies(pokemonEntity: PokemonEntity): Boolean {
        if (!config.chanseyLineHealsPlayers) return false
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return speciesName in VALID_SPECIES
    }

    /**
     * Checks if the Pokémon qualifies as a healer because its species is
     * explicitly listed in the config.
     */
    private fun isDesignatedHealer(pokemonEntity: PokemonEntity): Boolean {
        val speciesName = pokemonEntity.pokemon.species.translatedName.string.lowercase()
        return config.healers.any { it.lowercase() == speciesName }
    }

    /**
     * Checks if the Pokémon qualifies as a healer because of its moves.
     */
    private fun doesPokemonKnowHealingMove(pokemonEntity: PokemonEntity): Boolean {
        return pokemonEntity.pokemon.moveSet.getMoves().any { it.name in config.healingMoves }
    }
}