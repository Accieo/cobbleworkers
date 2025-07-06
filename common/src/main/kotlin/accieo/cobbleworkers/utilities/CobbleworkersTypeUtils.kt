/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.utilities

import accieo.cobbleworkers.config.CobbleworkersConfigPokemonType
import accieo.cobbleworkers.extensions.toElementalType
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity

object CobbleworkersTypeUtils {
    /**
     * Checks if the Pokémon qualifies as a worker because its type
     * is enabled via the config.
     */
    fun isAllowedByType(configPokemonType: CobbleworkersConfigPokemonType, pokemonEntity: PokemonEntity): Boolean {
        val allowedType = configPokemonType.toElementalType()
        if (allowedType == null) return false
        return pokemonEntity.pokemon.types.any { it == allowedType }
    }
}