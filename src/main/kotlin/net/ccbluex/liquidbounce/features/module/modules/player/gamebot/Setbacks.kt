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
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerSneakMultiplier
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.features.module.modules.player.gamebot.ModuleGameBot.report
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleClutch
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.SAFETY_FEATURE
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.inputFrameYaw
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.util.Mth
import net.minecraft.world.entity.Relative
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.phys.Vec3
import java.util.Locale
import kotlin.math.abs

/**
 * Logs a `setback` line for every position correction of the server, and keeps the player from moving in ways a
 * 1.8 server corrects while a round is played: sprinting while sneaking or away from where the server sees it look,
 * and sneaking faster than sneak speed.
 */
internal object Setbacks : EventListener, MinecraftShortcuts {

    override fun parent() = ModuleGameBot

    private val game: Game
        get() = ModuleGameBot.games.activeMode

    /** Setbacks of the round so far. */
    var count = 0
        private set

    private var ticks = 0
    private var holdUntil = 0
    private var stillFrom: Vec3? = null
    private var stillAt = 0L
    private var stillCount = 0
    private var lastAt = Int.MIN_VALUE / 2

    /** Ticks since the server last set the player back. */
    val ticksSinceLast: Int
        get() = ticks - lastAt

    /**
     * Whether the bot holds still to get out of a setback loop: every movement module stays off and the walk
     * stands for a moment, so the server takes the clean position instead of resetting the next move again.
     */
    val isHolding: Boolean
        get() = ticks < holdUntil

    /** Movement modules the hold turns off, plus the block placing the walk and the fall catcher use. */
    private val paused: List<ClientModule> by lazy {
        ModuleManager.filter { it.category == ModuleCategories.MOVEMENT } + listOf(ModuleScaffold, ModuleClutch)
    }

    /** Counts the setbacks of a new round from one. */
    fun startRound() {
        count = 0
        stillFrom = null
        stillCount = 0
        holdUntil = 0
    }

    @Suppress("unused")
    private val tickHandler = handler<PlayerTickEvent>(priority = OBJECTION_AGAINST_EVERYTHING) {
        ticks++
        if (!isHolding) {
            return@handler
        }

        paused.forEach { it.enabled = false }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet as? ClientboundPlayerPositionPacket ?: return@handler
        // Runs before the packet itself is handled, so the position is still the one before the setback.
        mc.execute { log(packet) }
    }

    // Omnidirectional sprint moves at sprint speed sideways and backwards, which the server sets back.
    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = SAFETY_FEATURE) { event ->
        if (!event.sprint || event.source == SprintEvent.Source.NETWORK || !game.isPlaying) {
            return@handler
        }

        if (player.input.keyPresses.shift || !isForwardToServer(event.directionalInput)) {
            event.sprint = false
        }
    }

    // NoSlow's full-speed sneaking, as through gaps only a crouching player fits, is set back on each move.
    @Suppress("unused")
    private val sneakHandler = handler<PlayerSneakMultiplier>(priority = SAFETY_FEATURE) { event ->
        if (game.isPlaying) {
            event.multiplier = player.getAttributeValue(Attributes.SNEAKING_SPEED)
        }
    }

    private fun isForwardToServer(input: DirectionalInput): Boolean {
        if (!input.isMoving) {
            return true
        }
        val moving = getMovementDirectionOfInput(inputFrameYaw(), input)
        return abs(Mth.wrapDegrees(moving - serverYaw())) <= SPRINT_ANGLE
    }

    private fun serverYaw() = RotationManager.currentRotation?.yaw ?: player.yRot

    private fun log(packet: ClientboundPlayerPositionPacket) {
        val player = mc.player ?: return
        val before = player.position()
        val change = packet.change.position
        val target = Vec3(
            if (Relative.X in packet.relatives) before.x + change.x else change.x,
            if (Relative.Y in packet.relatives) before.y + change.y else change.y,
            if (Relative.Z in packet.relatives) before.z + change.z else change.z,
        )
        val delta = target.subtract(before)
        lastAt = ticks

        report(
            "setback n=${++count} from=${format(before)} to=${format(target)} delta=${format(delta)} " +
                "dist=${"%.2f".format(Locale.ROOT, delta.length())} ${game.situation() ?: "state=none"} " +
                "sneak=${player.isShiftKeyDown} sprint=${player.isSprinting} onGround=${player.onGround()} " +
                "speed=${ModuleSpeed.enabled} chest=${mc.gui.screen() is AbstractContainerScreen<*>} " +
                "invServer=${InventoryManager.isInventoryOpenServerSide} " +
                "timer=${"%.2f".format(Locale.ROOT, Timer.timerSpeed)} " +
                "yaw=${"%.0f".format(Locale.ROOT, player.yRot)}/${"%.0f".format(Locale.ROOT, serverYaw())}"
        )
        MapRecorder.event(
            "setback", "from" to before, "to" to target, "delta" to delta, "dist" to delta.length(),
            "sneak" to player.isShiftKeyDown, "sprint" to player.isSprinting, "onGround" to player.onGround(),
            "yaw" to player.yRot, "serverYaw" to serverYaw(),
        )
        Clips.noteSetback()
        noteStillness(before, delta.length())
    }

    /**
     * A set back of zero length to the same spot the last one went to means the server refuses every move of a
     * player that stands still. Repeating it forever keeps the bot stuck, so the loop is broken instead.
     */
    private fun noteStillness(from: Vec3, distance: Double) {
        if (!game.isPlaying) {
            return
        }

        val now = System.currentTimeMillis()
        val last = stillFrom
        val sameSpot = last != null && last.distanceToSqr(from) <= STILL_DISTANCE * STILL_DISTANCE
        if (distance > STILL_DISTANCE || !sameSpot || now - stillAt > LOOP_WINDOW_MS) {
            stillFrom = from
            stillAt = now
            stillCount = 1
            return
        }

        if (++stillCount < LOOP_SETBACKS || isHolding) {
            return
        }

        stillFrom = null
        stillCount = 0
        breakLoop()
    }

    /** Stops the movement for a second and sends one honest on-ground position, so the server drops the set back. */
    private fun breakLoop() {
        holdUntil = ticks + HOLD_TICKS
        paused.forEach { it.enabled = false }
        sendPacketSilently(
            ServerboundMovePlayerPacket.Pos(player.x, player.y, player.z, true, player.horizontalCollision)
        )
        report("setback loop broken")
    }

    private fun format(pos: Vec3) = "%.2f,%.2f,%.2f".format(Locale.ROOT, pos.x, pos.y, pos.z)

    /** Forward and diagonally forward, as vanilla sprints. */
    private const val SPRINT_ANGLE = 50f

    /** Two positions this close are the same spot, and a set back this short moved nothing. */
    private const val STILL_DISTANCE = 0.01

    /** Zero-distance setbacks at one spot for this many times within [LOOP_WINDOW_MS] are a loop. */
    private const val LOOP_SETBACKS = 10
    private const val LOOP_WINDOW_MS = 1000L

    private const val HOLD_TICKS = 20

}
