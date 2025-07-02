/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.extensions

import accieo.cobbleworkers.config.CobbleworkersConfigPokemonType
import com.cobblemon.mod.common.api.types.ElementalType
import com.cobblemon.mod.common.api.types.ElementalTypes

fun CobbleworkersConfigPokemonType.toElementalType(): ElementalType? {
    if (this == CobbleworkersConfigPokemonType.NONE) {
        return null
    }

    return ElementalTypes.get(this.name)
}