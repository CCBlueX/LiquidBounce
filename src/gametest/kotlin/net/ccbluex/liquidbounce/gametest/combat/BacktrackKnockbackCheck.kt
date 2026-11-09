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

import net.ccbluex.liquidbounce.features.module.modules.combat.backtrack.ModuleBacktrack
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.world.phys.Vec3

private const val PROMPT_TICKS = 3

/**
 * Backtrack: our own knockback waits in the queue with the rest.
 */
fun CombatScene.checkBacktrackHoldsOwnKnockback() {
    val lifted = liftAfterKnockback(flushOnKnockback = false)
    check(lifted < 0.01) { "Backtrack: our knockback passed without FlushOnKnockback, lifted $lifted" }
}

/**
 * Backtrack with FlushOnKnockback: our own knockback releases the queue at once.
 */
fun CombatScene.checkBacktrackFlushesOnOwnKnockback() {
    val lifted = liftAfterKnockback(flushOnKnockback = true)
    check(lifted > 0.1) { "Backtrack: our knockback was held with FlushOnKnockback, lifted $lifted" }
}

/**
 * Hits a husk so Backtrack starts holding packets, knocks us up and returns how far we rose shortly after.
 */
private fun CombatScene.liftAfterKnockback(flushOnKnockback: Boolean): Double {
    reset()
    val husk = spawnHusk(at(0.0, 0.0, 2.0))
    context.onClient {
        ModuleBacktrack.set("Delay", "1000..1000")
        ModuleBacktrack.set("NextBacktrackDelay", "0..0")
        ModuleBacktrack.set("Chance", "100")
        ModuleBacktrack.set("FlushOnKnockback", flushOnKnockback.toString())
        ModuleBacktrack.enabled = true
    }
    context.waitTicks(ATTACK_TICKS)

    attack(husk)
    check(context.fromClient { ModuleBacktrack.shouldCancelPackets() }) { "Backtrack: not lagging after the hit" }
    onServer { server ->
        val player = serverPlayer(server)
        player.connection.send(ClientboundSetEntityMotionPacket(player.id, Vec3(0.0, 0.5, 0.0)))
    }
    context.waitTicks(PROMPT_TICKS)
    val lifted = context.fromClient { checkNotNull(it.player).y } - origin.y

    context.onClient { ModuleBacktrack.enabled = false }
    context.waitTicks(PROMPT_TICKS)
    context.waitFor({ client -> checkNotNull(client.player).onGround() }, CombatScene.SETTLE_TIMEOUT_TICKS)
    return lifted
}
