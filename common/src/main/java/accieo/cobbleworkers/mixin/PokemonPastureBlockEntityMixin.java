/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.mixin;

import accieo.cobbleworkers.Cobbleworkers;
import accieo.cobbleworkers.cache.CobbleworkersCacheManager;
import accieo.cobbleworkers.jobs.WorkerDispatcher;
import com.cobblemon.mod.common.block.entity.PokemonPastureBlockEntity;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(PokemonPastureBlockEntity.class)
public class PokemonPastureBlockEntityMixin {
	@Inject(at = @At("TAIL"), method = "TICKER$lambda$0")
	private static void init(World world, BlockPos blockPos, BlockState blockState, PokemonPastureBlockEntity pastureBlock, CallbackInfo ci) {
		if (world.isClient) return;

		try {
			WorkerDispatcher.INSTANCE.tickAreaScan(world, blockPos);
		} catch (Exception e) {
			Cobbleworkers.LOGGER.error("[Cobbleworkers] - Error processing WorkerDispatcher tickAreaScan", e);
		}

		pastureBlock.getTetheredPokemon().stream()
				.filter(Objects::nonNull)
				.forEach(tethering -> {
					Pokemon pokemon;
					try {
						pokemon = tethering.getPokemon();
					} catch (Exception e)  {
						Cobbleworkers.LOGGER.error("[Cobbleworkers] - Failed to get Pokémon from tethering: {}", e.getMessage());
						return;
					}

					if (pokemon == null || pokemon.isFainted()) return;

					PokemonEntity pokemonEntity = pokemon.getEntity();
					if (pokemonEntity == null) return;

					PoseType poseType = pokemonEntity.getDataTracker().get(PokemonEntity.getPOSE_TYPE());
					if (poseType == PoseType.SLEEP) return;

					try {
						WorkerDispatcher.INSTANCE.tickPokemon(world, blockPos, pokemonEntity);
					} catch (Exception e) {
						Cobbleworkers.LOGGER.error("[Cobbleworkers] - Error processing WorkerDispatcher.tickPokemon {}", e.getMessage());
					}
				});
	}

	@Inject(method = "onBroken()V", at = @At("TAIL"), remap = false)
	private void onPastureBroken(CallbackInfo ci) {
		PokemonPastureBlockEntity self = (PokemonPastureBlockEntity)(Object)this;
		World world = self.getWorld();
		if (world != null && !world.isClient) {
			CobbleworkersCacheManager.INSTANCE.removePasture(self.getPos());
		}
	}
}
