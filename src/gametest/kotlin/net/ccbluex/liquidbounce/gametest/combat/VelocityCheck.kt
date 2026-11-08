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

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.world.phys.Vec3

private const val ROLLS = 20
private val FACTORS = 0.399..0.601
private const val HOP_TICKS = 20

/**
 * Velocity: Modify rolls the horizontal and vertical factors per hit.
 */
fun CombatScene.checkVelocity() {
    reset()
    context.onClient {
        ModuleVelocity.modes.setByString("Modify")
        val modify = ModuleVelocity.modes.modes.first { it.name == "Modify" }
        modify.set("Random/Horizontal", "0.4..0.6")
        modify.set("Random/Vertical", "0.4..0.6")
        modify.set("Random/Enabled", "true")
        ModuleVelocity.enabled = true
    }

    val factors = context.fromClient { client ->
        val id = checkNotNull(client.player).id
        List(ROLLS) {
            val packet = ClientboundSetEntityMotionPacket(id, Vec3(1.0, 1.0, 1.0))
            EventManager.callEvent(PacketEvent(TransferOrigin.INCOMING, packet))
            packet.movement
        }
    }
    check(factors.all { it.x in FACTORS && it.y in FACTORS && it.x == it.z } && factors.distinct().size > 1) {
        "Velocity: rolled factors $factors"
    }

    val reduced = hopPeak()
    context.onClient { ModuleVelocity.enabled = false }
    val vanilla = hopPeak()
    check(reduced in 0.05..0.5 * vanilla) { "Velocity: hop of $reduced blocks, vanilla $vanilla" }
}

/**
 * Sends upwards knockback and returns how high the server saw the player go.
 */
private fun CombatScene.hopPeak(): Double {
    context.waitFor({ client -> checkNotNull(client.player).onGround() }, HOP_TICKS * 2)
    onServer { server ->
        val player = serverPlayer(server)
        player.connection.send(ClientboundSetEntityMotionPacket(player.id, Vec3(0.0, 0.6, 0.0)))
    }
    var peak = 0.0
    repeat(HOP_TICKS) {
        context.waitTick()
        peak = maxOf(peak, onServer { serverPlayer(it).y } - origin.y)
    }
    return peak
}
