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
package net.ccbluex.liquidbounce.utils.navigation

import net.ccbluex.agent.client.ClientBody
import net.ccbluex.agent.math.Vec3
import net.ccbluex.agent.math.ViewRotation
import net.ccbluex.agent.navigation.BodyTraits
import net.ccbluex.agent.navigation.LookPurpose
import net.ccbluex.agent.navigation.Teleporter
import net.ccbluex.agent.navigation.Turning
import net.ccbluex.agent.vanilla.toMinecraft
import net.ccbluex.agent.world.Input
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.features.global.GlobalSettingsTeleport
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleElytraRecast
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_13_2
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.inputFrameYaw
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

/**
 * The local player as LiquidBounce moves it: keys through [MovementInputEvent] ([applyKeys]), rotations through
 * [RotationManager] with [rotations] on behalf of [owner], teleports with silent position packets. Turns are taken at
 * once, since the rotation manager paces them.
 */
class LocalPlayerBody(
    private val owner: EventListener,
    private val rotations: RotationsValueGroup? = null,
) : ClientBody(Turning.instant()) {

    private var pressed: Input? = null
    private var pressedExact = false

    /** The rotation target this body asked for last, to tell it from those of other modules. */
    private var ownTarget: RotationTarget? = null

    override val traits: BodyTraits
        get() = BodyTraits(jumpClimbsLadders = !isOlderThanOrEqual1_13_2, exactSimulation = true)

    override val recastsElytra: Boolean
        get() = ModuleElytraRecast.running

    // Fly moves the player whichever mode runs; the navigator measures how fast
    override val isFlying: Boolean
        get() = ModuleFly.running

    override val teleporter: Teleporter
        get() = PacketTeleporter

    override val sentRotation: ViewRotation
        get() = RotationManager.serverRotation.let { ViewRotation(it.yaw, it.pitch) }

    override val movementYRot: Float
        get() = RotationManager.movementYaw

    override val inputYRot: Float
        get() = inputFrameYaw()

    override val isRotationOverridden: Boolean
        get() = RotationManager.activeRotationTarget.let { it != null && it !== ownTarget }

    override fun press(input: Input, exact: Boolean) {
        pressed = stayOnGroundSurface(input)
        pressedExact = exact
    }

    override fun look(rotation: ViewRotation, purpose: LookPurpose) {
        val rotations = rotations
        if (purpose == LookPurpose.VIEW || rotations == null) {
            return super.look(rotation, purpose)
        }
        // Whole mouse steps from the rotation the player has now; the sent one lags a tick behind
        val paced = if (purpose == LookPurpose.STEER && player.isFallFlying) {
            rotation
        } else {
            val current = RotationManager.currentRotation ?: Rotation(player.yRot, player.xRot)
            pace(ViewRotation(current.yaw, current.pitch), rotation, purpose)
        }
        val target = rotations.toRotationTarget(Rotation(paced.yRot, paced.xRot))
        ownTarget = target
        val priority = if (purpose == LookPurpose.MOVEMENT) Priority.NOT_IMPORTANT else Priority.IMPORTANT_FOR_USAGE_2
        RotationManager.setRotationTarget(target, priority, owner)
    }

    /** Puts the keys of the tick into [event] and lets go of them. */
    fun applyKeys(event: MovementInputEvent) {
        val keys = pressed ?: return
        event.directionalInput = DirectionalInput(keys.forward, keys.backward, keys.left, keys.right)
        event.jump = keys.jump
        event.sneak = keys.shift
    }

    /** Whether the keys of the tick force sprinting. */
    val wantsSprint: Boolean
        get() = pressed?.sprint == true

    /** Whether the keys of the tick must reach the player as they are, past every other module. */
    val keysExact: Boolean
        get() = pressed != null && pressedExact

    /** Clears the keys of the last tick. */
    fun release() {
        pressed = null
        pressedExact = false
        safeWalk = false
    }

}

/** Teleports the local player with movement packets the other modules do not see. */
object PacketTeleporter : Teleporter {

    override fun send(position: Vec3, onGround: Boolean) {
        sendPacketSilently(ServerboundMovePlayerPacket.Pos(position.toMinecraft(), onGround, false))
    }

    override fun land(position: Vec3) {
        GlobalSettingsTeleport.teleportPlayer(position.x, position.y, position.z, resetMotion = true)
        player.resetFallDistance()
    }

}
