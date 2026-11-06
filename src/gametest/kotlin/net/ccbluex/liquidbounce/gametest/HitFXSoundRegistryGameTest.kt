/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.gametest

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.features.module.modules.render.hitfx.HitFXRegistry
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * The HitFX sounds are registered into the client's sound registry, where a server can name them with `/playsound`
 * to tell a modified client from a vanilla one. They must be resolvable after the client loaded and removable by
 * [HitFXRegistry.unregisterSounds].
 */
class HitFXSoundRegistryGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()

        check(context.clientSounds().isNotEmpty()) {
            "HitFX sounds are missing from the client sound registry"
        }

        context.onClient { HitFXRegistry.unregisterSounds() }

        val remaining = context.clientSounds()
        check(remaining.isEmpty()) { "sounds survived unregisterSounds: $remaining" }
    }

    private fun ClientGameTestContext.clientSounds() = fromClient { mc ->
        mc.soundManager.registry.keys.filter { it.namespace == CLIENT_NAME.lowercase() }
    }

}
