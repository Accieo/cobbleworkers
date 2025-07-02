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