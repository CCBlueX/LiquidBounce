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

import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.world.level.Level
import net.minecraft.world.phys.Vec3

private const val RISE_TICKS = 4
private const val LAND_TICKS = 200

/**
 * Velocity PauseOnExplosion: after explosion knockback, all knockback is left alone until we land.
 */
fun CombatScene.checkVelocityPauseOnExplosion() {
    reset()
    world.server.runCommand("effect give @a minecraft:resistance 60 4 true")
    context.onClient {
        ModuleVelocity.modes.setByString("Modify")
        ModuleVelocity.set("PauseOnExplosion", "true")
        ModuleVelocity.enabled = true
    }

    // Plain knockback is still blocked
    check(rise { push() } < 0.05) { "Velocity: knockback not blocked before any explosion" }

    val explosionRise = rise {
        val center = context.fromClient { checkNotNull(it.player).position() }.add(0.0, 0.1, 0.6)
        onServer { server ->
            server.overworld().explode(null, center.x, center.y, center.z, 2f, Level.ExplosionInteraction.NONE)
        }
    }
    check(explosionRise > 0.1) { "Velocity: explosion knockback was modified, rose $explosionRise" }
    val airRise = rise(waitForGround = false) { push() }
    check(airRise > 0.1) { "Velocity: knockback in the air after the explosion was modified, rose $airRise" }

    context.waitFor({ client -> checkNotNull(client.player).onGround() }, LAND_TICKS)
    context.waitTicks(RISE_TICKS)
    check(rise { push() } < 0.05) { "Velocity: knockback not blocked again after landing" }

    context.onClient { ModuleVelocity.enabled = false }
}

private fun CombatScene.push() = onServer { server ->
    val player = serverPlayer(server)
    player.connection.send(ClientboundSetEntityMotionPacket(player.id, Vec3(0.0, 0.6, 0.0)))
}

/**
 * Runs [action] and returns how high the client rose over the next ticks.
 */
private fun CombatScene.rise(waitForGround: Boolean = true, action: () -> Unit): Double {
    if (waitForGround) {
        context.waitFor({ client -> checkNotNull(client.player).onGround() }, LAND_TICKS)
    }
    val start = context.fromClient { checkNotNull(it.player).y }
    action()
    var peak = 0.0
    repeat(RISE_TICKS) {
        context.waitTick()
        peak = maxOf(peak, context.fromClient { checkNotNull(it.player).y } - start)
    }
    return peak
}
