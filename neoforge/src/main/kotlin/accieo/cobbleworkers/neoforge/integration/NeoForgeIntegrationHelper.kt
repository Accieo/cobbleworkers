/*
 * Copyright (C) 2025 Accieo
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 */

package accieo.cobbleworkers.neoforge.integration

import accieo.cobbleworkers.interfaces.ModIntegrationHelper
import net.neoforged.fml.ModList

object NeoForgeIntegrationHelper : ModIntegrationHelper {
    override fun isModLoaded(modId: String): Boolean {
        return ModList.get().isLoaded(modId)
    }
}