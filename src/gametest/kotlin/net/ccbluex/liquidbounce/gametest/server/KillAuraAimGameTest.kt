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
import net.ccbluex.gametest.harness.ServerGameTest
import net.ccbluex.gametest.harness.action.summon
import net.ccbluex.gametest.harness.verdict.ServerReport
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.util.GSON_DEFAULT
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTarget
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.KillAuraRotationsValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.gametest.harness.disable
import net.ccbluex.liquidbounce.gametest.harness.enable
import net.ccbluex.liquidbounce.gametest.harness.setting
import net.ccbluex.liquidbounce.gametest.health
import net.ccbluex.liquidbounce.gametest.set
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.rotation
import java.util.Locale
import java.util.Random
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.tan

private const val FIGHT_TICKS = 1200
private const val DISENGAGES = 20
private const val HAND_BACK_TICKS = 100

/**
 * KillAura with the Human aim against an opponent that strafes and jumps around the player for a minute, then runs off
 * again and again while the player turns away: the server may not object once, enough hits have to land, and they
 * land where a person aims, on the upper body. The player jumps every second or two, as one going for critical hits
 * does. Where an anticheat checks aim (Intave, MX), the Constant aim then has to be caught, or a clean run would prove
 * nothing; elsewhere it has to pass as well.
 */
class KillAuraAimGameTest : ServerGameTest({
    val saved = client { ConfigSystem.serializeValueGroup(ModuleKillAura) to
        ConfigSystem.serializeValueGroup(GlobalSettingsTarget) }
    val attacks = client { Attacks() }

    try {
        client {
            GlobalSettingsTarget.combatChoices.deserializeFrom(GSON_DEFAULT, JsonArray().apply { add("Passive") })
            // Anticheats allow no more than the reach of the game
            ModuleKillAura.range.containedValues.first { it.name == "RangeIncrease" }.setByString("0")
            // The opponent spawns exactly in front of the camera. From yaw 0 the mouse grid would run through its
            // exact angle, a start no fight has and Intave reads as an aimbot
            val player = checkNotNull(it.player)
            player.yRot = 17.32f
            player.xRot = 6.87f
        }
        ticks(5)
        val opponent = Opponent(this)
        opponent.spawn()
        // One jump at a time and never to a beat, the way a player jumps
        val jumps = Random(31L)
        var jumpIn = 20
        fun fight(ticks: Int) = try {
            opponent.fight(ticks) {
                when (jumpIn--) {
                    0 -> input.holdKey { it.keyJump }
                    -2 -> {
                        input.releaseKey { it.keyJump }
                        jumpIn = 15 + jumps.nextInt(40)
                    }
                }
            }
        } finally {
            input.releaseKey { it.keyJump }
        }

        val human = server.console.size
        aim("Human")
        enable(ModuleKillAura)
        val (hits, low) = attacks.during("Human", opponent) { fight(FIGHT_TICKS) }
        // Fewer hits in a minute than a person who aims at all would land
        check(hits >= 60) { "the Human aim landed $hits hits in ${FIGHT_TICKS / 20} seconds" }
        // A view left where the player's own jumps push it has a quarter of the hits below 0.2, kept near level 0.4
        check(low >= 0.28) {
            "a quarter of the Human aim's hits land below %.2f of the opponent's height, on its legs".format(low)
        }

        // The opponent runs out of reach and the player turns away, so the view turns back to the camera each time
        val mouse = Random(7L)
        repeat(DISENGAGES) { round ->
            val side = if (round % 2 == 0) 1 else -1
            opponent.flee { tick ->
                if (tick in 8 until 14) {
                    // Mostly sideways and a little up or down, never twice the same: a hand, not a script
                    turnCamera(side * (16.0 + mouse.nextDouble() * 8.0), mouse.nextDouble() * 4.0 - 2.0)
                }
            }
            opponent.fight(60)
        }

        // Looking a quarter turn away when KillAura stops, the view has a way back to the camera: it walks it
        turnCamera(90.0, 0.0)
        ticks(1)
        disable(ModuleKillAura)
        var last = client { RotationManager.serverRotation }
        val way = last.rotationDeltaLengthTo(client { checkNotNull(it.player).rotation })
        var largest = 0f
        awaitClient("the view to be handed back", HAND_BACK_TICKS) {
            val now = RotationManager.serverRotation
            largest = max(largest, last.rotationDeltaLengthTo(now))
            last = now
            RotationManager.currentRotation == null
        }
        check(largest < way * 0.7f) { "the view jumped $largest of the $way degrees back to the camera in one tick" }
        expectNoObjection("the Human aim", human)

        aim("Constant")
        val constant = server.console.size
        if (anticheats.any { it == Anticheat.INTAVE || it == Anticheat.MX }) {
            enable(ModuleKillAura)
            attacks.during("Constant", opponent) { fight(600) }
            disable(ModuleKillAura)
            // Flags trail the turns they are about
            ticks(40)
            check(objections(constant).any { it.report == ServerReport.FLAG }) {
                "no aim check caught the Constant aim, see ${server.log}"
            }
        } else {
            // Without aim checks Constant has to pass as well: instant, then slowed down by Dynamic
            for (dynamic in listOf("false", "true")) {
                client { KillAuraRotationsValueGroup.set("Aim/Constant/Dynamic/Enabled", dynamic) }
                enable(ModuleKillAura)
                attacks.during("Constant, Dynamic $dynamic", opponent) { fight(600) }
                disable(ModuleKillAura)
            }
            expectNoObjection("the Constant aim", constant)
        }
    } finally {
        client {
            ModuleKillAura.enabled = false
            ConfigSystem.deserializeValueGroup(ModuleKillAura, saved.first)
            ConfigSystem.deserializeValueGroup(GlobalSettingsTarget, saved.second)
            attacks.unregister()
        }
    }
}) {
    // Judged by the test itself, see [objections]: the flags it leaves out would use up what the harness tolerates
    override val violationLevel get() = Int.MAX_VALUE

    // A second attempt would make one clean fight in two enough
    override val retries get() = 0
}

private fun GameTestScope.aim(mode: String) = client {
    (KillAuraRotationsValueGroup.setting<Any>("Aim") as ModeValueGroup<*>).setByString(mode)
}

/** Turns the camera as the mouse does: by whole counts, which the game scales by 0.15. */
private fun GameTestScope.turnCamera(yaw: Double, pitch: Double) = client {
    val step = RotationUtil.gcd
    checkNotNull(it.player).turn((yaw / step).roundToInt() * step / 0.15, (pitch / step).roundToInt() * step / 0.15)
}

/**
 * What the server objected to from console line [since] on. Intave's attackraytrace is left out: it misjudges hits on
 * an opponent moved by teleports, whatever the aim.
 */
private fun GameTestScope.objections(since: Int) = server.console.drop(since).filter { line ->
    when (line.report) {
        ServerReport.FLAG -> !(line.anticheat == "Intave" && line.message.startsWith("attackraytrace"))
        ServerReport.SETBACK, ServerReport.FAILMOVE -> true
        null -> line.level == "WARN" || line.level == "ERROR"
        else -> false
    }
}

private fun GameTestScope.expectNoObjection(what: String, since: Int) {
    // Flags trail the turns they are about
    ticks(40)
    val objections = objections(since)
    check(objections.isEmpty()) {
        "the server objected to $what ${objections.size} time(s), see ${server.log}\n" + objections.joinToString("\n")
    }
}

private class Attacks : EventListener {
    /** The player's tick count at each attack. */
    private val ticks = ArrayList<Int>()

    /** How high on the attacked entity's box the view met it at each attack: 0 at the feet, 1 at the top. */
    private val heights = ArrayList<Double>()

    @Suppress("unused")
    private val attackHandler = handler<AttackEntityEvent> { event ->
        ticks += player.tickCount
        val eyes = player.eyePosition
        val target = event.entity
        val view = RotationManager.currentRotation ?: player.rotation
        val drop = tan(Math.toRadians(view.pitch.toDouble())) * hypot(target.x - eyes.x, target.z - eyes.z)
        heights += (eyes.y - drop - target.boundingBox.minY) / target.bbHeight
    }

    /**
     * The attacks made during [fight] and the height on the opponent that three of four of them met it above, logged
     * with the damage [opponent] took on the server and the longest wait between two attacks, to compare aims and runs.
     */
    fun during(aim: String, opponent: Opponent, fight: () -> Unit): Pair<Int, Double> {
        val before = ticks.size
        val health = opponent.health()
        fight()
        val made = ticks.drop(before)
        val wait = made.zipWithNext { earlier, later -> later - earlier }.maxOrNull()
        val met = heights.drop(before).sorted()
        val low = met.getOrElse(met.size / 4) { 0.0 }
        logger.info(
            "[KillAuraAim] $aim attacked ${made.size} times for ${health - opponent.health()} damage, " +
                "longest wait $wait ticks, at height %.2f (quartiles %.2f to %.2f)".format(
                    met.getOrElse(met.size / 2) { 0.0 }, low, met.getOrElse(met.size * 3 / 4) { 0.0 },
                )
        )
        return made.size to low
    }
}

/**
 * A villager that strafes around the player 2.2 to 3 blocks away the way a player does in a fight: each strafe at its
 * own speed and length, now and then a short stop, a step in or out, a jump, or it runs off. It is moved by teleports,
 * so it does the same in every run.
 */
private class Opponent(private val scope: GameTestScope) {
    private val tag = scope.unique("opponent")
    private val random = Random(26L)
    private var angle = 0.0
    private var distance = 3.0
    private var velocity = 0.0
    private var strafeIn = 0
    private var approach = 0.0
    private var height = 0.0
    private var lift = 0.0
    private var jumpIn = 0

    // Health to lose all test long, so the server tells which attacks landed
    fun spawn() = scope.summon(
        "minecraft:villager",
        scope.origin.south(3),
        "{NoAI:1b,Silent:1b,PersistenceRequired:1b,Health:1024f," +
            "attributes:[{id:\"minecraft:max_health\",base:1024d}],Tags:[\"$tag\"]}",
    )

    fun health() = scope.health("@e[tag=$tag,limit=1]")

    /** Fights for [ticks], calling [each] with the tick it is at. */
    fun fight(ticks: Int, each: (Int) -> Unit = {}) = repeat(ticks) { tick ->
        step()
        moveTo(distance, scope.origin.y + height)
        each(tick)
        scope.ticks(1)
    }

    /**
     * Runs out of reach at sprinting speed, stays away a second and comes back, calling [each] with the tick it is at.
     */
    fun flee(each: (Int) -> Unit) {
        var tick = 0
        fun step(to: Double) {
            distance = to
            moveTo(distance, scope.origin.y.toDouble())
            each(tick++)
            scope.ticks(1)
        }
        while (distance < 9.0) step(distance + 0.28)
        repeat(20) { step(distance) }
        while (distance > 3.0) step(distance - 0.28)
    }

    private fun moveTo(distance: Double, y: Double) {
        val x = scope.origin.x + 0.5 + sin(angle) * distance
        val z = scope.origin.z + 0.5 + cos(angle) * distance
        scope.run(String.format(Locale.ROOT, "tp @e[tag=%s,limit=1] %.3f %.3f %.3f", tag, x, y, z))
    }

    private fun step() {
        if (--strafeIn <= 0) {
            val stop = random.nextInt(6) == 0
            val side = when {
                velocity > 0.0 -> -1.0
                velocity < 0.0 -> 1.0
                random.nextBoolean() -> 1.0
                else -> -1.0
            }
            velocity = if (stop) 0.0 else side * (0.15 + random.nextDouble() * (0.28 - 0.15))
            strafeIn = if (stop) 3 + random.nextInt(6) else 8 + random.nextInt(25)
            approach = (random.nextDouble() - 0.5) * 0.08
        }
        angle += velocity / distance
        distance = (distance + approach).coerceIn(2.2, 3.0)

        if (--jumpIn <= 0 && height == 0.0) {
            lift = 0.42
            jumpIn = 15 + random.nextInt(40)
        }
        if (height > 0.0 || lift > 0.0) {
            height += lift
            lift = (lift - 0.08) * 0.98
            if (height <= 0.0) {
                height = 0.0
                lift = 0.0
            }
        }
    }
}
