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
package net.ccbluex.liquidbounce.gametest.server

import com.google.gson.JsonArray
import net.ccbluex.gametest.harness.Anticheat
import net.ccbluex.gametest.harness.GameTestScope
import net.ccbluex.gametest.harness.action.summon
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.gametest.harness.LiquidBounceGameTest
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.gametest.harness.setting
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.entity.rotation
import java.util.Locale
import java.util.Random
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private const val FIGHT_TICKS = 1200
private const val CONTROL_TICKS = 600
private const val HAND_BACK_TICKS = 100
private const val CAMERA_YAW = 17.32f
private const val CAMERA_PITCH = 6.87f

/** Hits in a minute against the opponent below a person who aims at all would land. */
private const val MIN_HITS = 60

/**
 * KillAura with the Human aim against an opponent that strafes and jumps around the player for a minute: nothing that
 * watches the player may object, and enough hits have to land. Where an anticheat checks aim (Intave, MX), the
 * Constant aim has to be caught first, or a clean run would prove nothing.
 */
class KillAuraAimGameTest : LiquidBounceGameTest({
    val saved = client { ConfigSystem.serializeValueGroup(ModuleKillAura) to
        ConfigSystem.serializeValueGroup(GlobalSettingsTarget) }
    val attacks = client { Attacks() }

    try {
        client {
            GlobalSettingsTarget.combatChoices.deserializeFrom(GSON_DEFAULT, JsonArray().apply { add("Passive") })
            // Anticheats allow no more than the reach of the game
            ModuleKillAura.range.containedValues.first { it.name == "RangeIncrease" }.setByString("0")
            // Someone who has looked around does not hold the camera at round angles. From exactly 0 the mouse grid
            // runs through the exact angle of a level target, which Intave reads as an aimbot when a turn ends on it
            val player = checkNotNull(it.player)
            player.yRot = CAMERA_YAW
            player.xRot = CAMERA_PITCH
        }
        ticks(5)
        val opponent = Opponent(this)
        opponent.spawn()

        aim("Human")
        enable(ModuleKillAura)
        opponent.fight(FIGHT_TICKS)
        disable(ModuleKillAura)
        val hits = attacks.count
        check(hits >= MIN_HITS) { "the Human aim landed $hits hits in ${FIGHT_TICKS / 20} seconds" }

        // The view turns back to the camera and is handed over where the camera looks, without a jump
        awaitClient("the view to be handed back", HAND_BACK_TICKS) { RotationManager.currentRotation == null }
        val (server, camera) = client { RotationManager.serverRotation to checkNotNull(it.player).rotation }
        check(abs(angleDifference(server.yaw, camera.yaw)) < 1e-3f && abs(server.pitch - camera.pitch) < 1e-3f) {
            "the server looks at $server, the camera at $camera"
        }

        if (anticheats.any { it == Anticheat.INTAVE || it == Anticheat.MX }) {
            aim("Constant")
            expectViolation("the aim checks to catch the Constant aim") {
                enable(ModuleKillAura)
                opponent.fight(CONTROL_TICKS)
                disable(ModuleKillAura)
            }
        }
    } finally {
        client {
            ModuleKillAura.enabled = false
            ConfigSystem.deserializeValueGroup(ModuleKillAura, saved.first)
            ConfigSystem.deserializeValueGroup(GlobalSettingsTarget, saved.second)
            attacks.unregister()
        }
    }
})

private fun GameTestScope.aim(mode: String) = client {
    (KillAuraRotationsValueGroup.setting<Any>("Aim") as ModeValueGroup<*>).setByString(mode)
}

private class Attacks : EventListener {
    var count = 0

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { count++ }
}

/**
 * A villager that strafes around the player 2.2 to 3 blocks away the way a player does in a fight: each strafe at its
 * own speed and length, now and then a short stop, a step in or out, a jump. It is moved by teleports, so it does the
 * same in every run.
 */
private class Opponent(private val scope: GameTestScope) {
    private val tag = scope.unique("opponent")
    private val random = Random(SEED)
    private var angle = 0.0
    private var distance = 3.0
    private var velocity = 0.0
    private var strafeIn = 0
    private var approach = 0.0
    private var height = 0.0
    private var lift = 0.0
    private var jumpIn = 0

    fun spawn() = scope.summon(
        "minecraft:villager",
        scope.origin.south(3),
        "{NoAI:1b,Invulnerable:1b,Silent:1b,PersistenceRequired:1b,Tags:[\"$tag\"]}",
    )

    fun fight(ticks: Int) = repeat(ticks) {
        step()
        val x = scope.origin.x + 0.5 + sin(angle) * distance
        val z = scope.origin.z + 0.5 + cos(angle) * distance
        val y = scope.origin.y + height
        scope.run(String.format(Locale.ROOT, "tp @e[tag=%s,limit=1] %.3f %.3f %.3f", tag, x, y, z))
        scope.ticks(1)
    }

    private fun step() {
        if (--strafeIn <= 0) {
            val stop = random.nextInt(STOP_CHANCE) == 0
            val side = when {
                velocity > 0.0 -> -1.0
                velocity < 0.0 -> 1.0
                random.nextBoolean() -> 1.0
                else -> -1.0
            }
            velocity = if (stop) 0.0 else side * (MIN_SPEED + random.nextDouble() * (MAX_SPEED - MIN_SPEED))
            strafeIn = if (stop) 3 + random.nextInt(6) else 8 + random.nextInt(25)
            approach = (random.nextDouble() - 0.5) * 0.08
        }
        angle += velocity / distance
        distance = (distance + approach).coerceIn(2.2, 3.0)

        if (--jumpIn <= 0 && height == 0.0) {
            lift = JUMP
            jumpIn = 15 + random.nextInt(40)
        }
        if (height > 0.0 || lift > 0.0) {
            height += lift
            lift = (lift - GRAVITY) * DRAG
            if (height <= 0.0) {
                height = 0.0
                lift = 0.0
            }
        }
    }

    private companion object {
        const val SEED = 26L
        const val MIN_SPEED = 0.15
        const val MAX_SPEED = 0.28
        const val STOP_CHANCE = 6
        const val JUMP = 0.42
        const val GRAVITY = 0.08
        const val DRAG = 0.98
    }
}
