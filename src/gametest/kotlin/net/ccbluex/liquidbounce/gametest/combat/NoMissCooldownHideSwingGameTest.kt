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
package net.ccbluex.liquidbounce.gametest.combat

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleNoMissCooldown
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.world.phys.HitResult

/**
 * NoMissCooldown's SwingMode hides the swing of a click into the air from the client, the server or both, and
 * leaves the swing of a hit alone.
 */
class NoMissCooldownHideSwingGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) = context.combatTest(ModuleNoMissCooldown) { server, y ->
        context.onClient { ModuleNoMissCooldown.enabled = true }

        face(context, server, y, 0f, -90f)
        context.waitFor({ client -> client.hitResult?.type == HitResult.Type.MISS }, WAIT_TICKS)
        assertSwing(context, SwingMode.DO_NOT_HIDE, client = true, server = true)
        assertSwing(context, SwingMode.HIDE_CLIENT, client = false, server = true)
        assertSwing(context, SwingMode.HIDE_SERVER, client = true, server = false)
        assertSwing(context, SwingMode.HIDE_BOTH, client = false, server = false)

        summonHusk(context, server, y, 0.5, 2.5)
        face(context, server, y, 0f)
        context.waitFor({ client -> client.hitResult?.type == HitResult.Type.ENTITY }, WAIT_TICKS)
        assertSwing(context, SwingMode.HIDE_BOTH, client = true, server = true)
    }

    /**
     * Clicks the attack button with [mode] and checks whether the [client] and the [server] get the swing.
     */
    private fun assertSwing(context: ClientGameTestContext, mode: SwingMode, client: Boolean, server: Boolean) {
        context.waitFor({ it.player?.isSwinging == false }, WAIT_TICKS)
        context.onClient {
            ModuleNoMissCooldown.set("SwingMode", mode.tag)
            CombatEvents.reset()
        }
        context.input.holdKeyFor({ it.keyAttack }, 1)

        val target = context.fromClient { checkNotNull(it.hitResult).type }
        val swinging = context.fromClient { checkNotNull(it.player).isSwinging }
        check(swinging == client) { "${mode.tag} on $target: swinging $swinging" }
        val punches = CombatEvents.punches
        check((punches > 0) == server) { "${mode.tag} on $target: $punches punches" }
    }

}
