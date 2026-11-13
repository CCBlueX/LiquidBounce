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
package net.ccbluex.liquidbounce.features.module.modules.player.gamebot

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerSafeWalkEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleClutch
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.SAFETY_FEATURE
import net.ccbluex.liquidbounce.utils.math.allEmpty
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.inputFrameYaw
import net.ccbluex.liquidbounce.utils.navigation.Navigator
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.max

/**
 * Keeps the player out of the void while a round is played. Movement that would walk or jump off into the void is
 * held back at the edge, unless the walk is building a bridge. [ModuleClutch] stays on to catch falls; a fall it
 * cannot catch is flown back from with [flyBack].
 *
 * Logs `edge` lines within a block of a void edge and a `fall` line when a fall starts.
 */
internal class VoidSafety(
    private val owner: EventListener,
    private val navigator: Navigator,
    /** `state=... action=...` while the player should be kept safe, else null. */
    private val situation: () -> String?,
    /** Flies back to the given ground; whether a flight started. */
    private val flyBack: (Vec3) -> Boolean,
) : EventListener, MinecraftShortcuts {

    enum class Hold(val tag: String) {
        NONE("none"),
        SAFE_WALK("safewalk"),
        NO_JUMP("nojump"),
        STOP("stop"),
    }

    private var ticks = 0
    private var holding = false

    /** How the movement was held back last tick. */
    var hold = Hold.NONE
        private set

    private var safeGround: Vec3? = null
    var isFalling = false
        private set
    private var fallTicks = 0
    private var flewBack = false

    private var ownsClutch = false

    /** Whether the player stood within a block of a void edge, or was held back, on the last tick on the ground. */
    var isNearEdge = false
        private set

    private var edgeLine: String? = null
    private var edgeLoggedAt = 0

    override fun parent() = owner

    /** For when the round ends or the player stops being kept safe. */
    fun stop() {
        holding = false
        hold = Hold.NONE
        isNearEdge = false
        endFall()
        if (ownsClutch) {
            ownsClutch = false
            ModuleClutch.enabled = false
        }
    }

    @Suppress("unused")
    private val safeWalkHandler = handler<PlayerSafeWalkEvent> { event ->
        if (holding) {
            event.isSafeWalk = true
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent>(priority = FIRST_PRIORITY) {
        ticks++
        val situation = situation()
        if (situation == null || player.abilities.flying) {
            stop()
            return@handler
        }

        if (!ModuleClutch.enabled) {
            ModuleClutch.enabled = true
            ownsClutch = true
            report("clutch on")
        }

        val position = player.position()
        if (player.onGround()) {
            if (!isOverVoid(position)) {
                safeGround = position
            }
            if (isFalling) {
                report("fall end landed=${format(position)}")
                MapRecorder.event("fall_end", "at" to position, "rescue" to flewBack)
                endFall()
            }
            return@handler
        }

        if (!isFalling && !ModuleFly.running && player.deltaMovement.y < 0.0 && isOverVoid(position)) {
            isFalling = true
            report("fall from=${safeGround?.let(::format) ?: "unknown"} at=${format(position)} " +
                "clutch=${ModuleClutch.guarding} $situation")
            MapRecorder.event(
                "fall", "from" to safeGround, "at" to position, "clutch" to ModuleClutch.guarding,
                "building" to navigator.isBuilding,
            )
            Clips.trigger(Clips.Trigger.FALL)
        }
        if (isFalling) {
            rescue()
        }
    }

    @Suppress("unused")
    private val inputHandler = handler<MovementInputEvent>(priority = SAFETY_FEATURE) { event ->
        holding = false
        hold = Hold.NONE
        val situation = situation() ?: return@handler
        if (ModuleFly.running || player.abilities.flying || isFalling || !player.onGround()) {
            return@handler
        }

        // A bridge runs or leans out over the void on purpose; Scaffold or the walk's safe walk keeps the player up.
        val building = navigator.isBuilding
        if (!building) {
            hold = hold(event)
        }
        val nearEdge = hold != Hold.NONE || isNearVoidEdge()
        isNearEdge = nearEdge
        if (nearEdge) {
            logEdge(situation)
        }
    }

    /** Holds back the movement of this tick as little as keeps the player out of the void. */
    private fun hold(event: MovementInputEvent): Hold {
        val input = event.directionalInput
        if (!fallsIntoVoid(input, event.jump, event.sneak, safeWalk = false)) {
            return Hold.NONE
        }

        holding = true
        return when {
            !fallsIntoVoid(input, event.jump, event.sneak, safeWalk = true) -> Hold.SAFE_WALK
            !fallsIntoVoid(input, jump = false, event.sneak, safeWalk = true) -> {
                event.jump = false
                Hold.NO_JUMP
            }

            else -> {
                event.directionalInput = DirectionalInput.NONE
                event.jump = false
                Hold.STOP
            }
        }
    }

    /** Whether moving on with this input drops the player more than a block into the void within [SIM_TICKS]. */
    private fun fallsIntoVoid(input: DirectionalInput, jump: Boolean, sneak: Boolean, safeWalk: Boolean): Boolean {
        // Sprint is decided after the input; assuming it overestimates the way, which is the safe side.
        val sprint = player.isSprinting || !sneak && input.forwards
        val simulated = SimulatedPlayerInput(input, jump, sprint, sneak)
        simulated.forceSafeWalk = safeWalk
        val body = SimulatedPlayer.fromClientPlayer(simulated)
        body.yRot = inputFrameYaw()

        val drop = player.y - DROP
        repeat(SIM_TICKS) {
            body.tick()
            if (body.y < drop && !body.onGround() && isOverVoid(body.position())) {
                return true
            }
        }
        return false
    }

    private fun isNearVoidEdge(): Boolean {
        val position = player.position()
        return EDGE_DIRECTIONS.any { (x, z) -> isOverVoid(position.add(x * EDGE_DISTANCE, 0.0, z * EDGE_DISTANCE)) }
    }

    /** No block to land on within [VOID_DEPTH] below a player at [pos]. */
    private fun isOverVoid(pos: Vec3): Boolean {
        val bottom = max(world.minY.toDouble(), pos.y - VOID_DEPTH)
        val box = player.dimensions.makeBoundingBox(pos).setMinY(bottom)
        return world.getBlockCollisions(player, box).allEmpty()
    }

    /** Clutch builds under the fall; flying back is for the falls it cannot catch or does not catch in time. */
    private fun rescue() {
        fallTicks++
        val clutchFails = !ModuleClutch.guarding && fallTicks >= FLY_AFTER_TICKS || fallTicks >= CLUTCH_TICKS
        val ground = safeGround
        if (flewBack || !clutchFails || ground == null) {
            return
        }

        flewBack = true
        val flies = flyBack(ground)
        report("fall rescue ${if (flies) "fly to=${format(ground)}" else "none"} clutch=${ModuleClutch.guarding}")
        MapRecorder.event("rescue", "at" to player.position(), "to" to ground, "flies" to flies)
        Clips.trigger(Clips.Trigger.RESCUE)
    }

    private fun endFall() {
        isFalling = false
        fallTicks = 0
        flewBack = false
    }

    private fun logEdge(situation: String) {
        val placement = navigator.placement?.let { "${it.verdict.tag}@${it.pos.x},${it.pos.y},${it.pos.z}" } ?: "none"
        val line = "guard=${hold.tag} placement=$placement"
        if (line == edgeLine && ticks - edgeLoggedAt < EDGE_LOG_TICKS) {
            return
        }

        edgeLine = line
        edgeLoggedAt = ticks
        report("edge pos=${format(player.position())} $line onGround=${player.onGround()} $situation")
        MapRecorder.event(
            "edge", "pos" to player.position(), "hold" to hold.tag, "onGround" to player.onGround(),
            "placement" to navigator.placement?.let { "${it.verdict.tag}:${it.pos.x}:${it.pos.y}:${it.pos.z}" },
        )
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        stop()
        safeGround = null
        edgeLine = null
    }

    private fun format(pos: Vec3) = "%.2f,%.2f,%.2f".format(Locale.ROOT, pos.x, pos.y, pos.z)

    private companion object {
        const val SIM_TICKS = 16
        const val DROP = 1.0
        const val VOID_DEPTH = 40.0
        const val EDGE_DISTANCE = 1.0
        val EDGE_DIRECTIONS = arrayOf(
            1.0 to 0.0, -1.0 to 0.0, 0.0 to 1.0, 0.0 to -1.0,
            0.7 to 0.7, 0.7 to -0.7, -0.7 to 0.7, -0.7 to -0.7,
        )

        const val FLY_AFTER_TICKS = 3
        const val CLUTCH_TICKS = 20
        const val EDGE_LOG_TICKS = 20
    }

}
