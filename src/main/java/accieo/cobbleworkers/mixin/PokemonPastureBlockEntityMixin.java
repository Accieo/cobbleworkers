/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.mixin;

import accieo.cobbleworkers.Cobbleworkers;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PokemonPastureBlockEntity.class)
public class PokemonPastureBlockEntityMixin {
	@Inject(at = @At("TAIL"), method = "TICKER$lambda$14")
	private static void init(World world, BlockPos blockPos, BlockState blockState, PokemonPastureBlockEntity pastureBlock, CallbackInfo ci) {
		if (world.isClient) return;

		pastureBlock.getTetheredPokemon().forEach(tethering -> {
			Pokemon pokemon = tethering.getPokemon();
			if (pokemon != null && !pokemon.isFainted()) {
				PokemonEntity pokemonEntity = pokemon.getEntity();

				if (pokemonEntity == null) return;

				try {
					// TODO: Switch case with jobs and checking from config suitable pokémon
					// TODO: Ensure that this also works in multiple-languages
					pokemonEntity.getPokemon().getDisplayName();
				} catch (Exception e) {
					Cobbleworkers.logger.error("[Cobbleworkers]: Error while processing pasture mixin");
				}
			}
		});
	}
}