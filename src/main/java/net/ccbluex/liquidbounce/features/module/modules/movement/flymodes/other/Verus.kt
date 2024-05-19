package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.boostMotion
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.boostTicksValue
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.damage
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.timerSlow
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly.yBoost
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook

/**
 * Modified code ported from VerusDamage Script by Arcane
 *
 * Note:
 * - Getting below block (Like NCPLatest Fly Method), should help to temporarily bypass Speed(A) Checks
 * - Turning off Damage should bypass Fly(G) Checks
 */
object Verus : FlyMode("Verus") {
    private var boostTicks = 0

    override fun onEnable() {
        boostTicks = 0
        if (mc.theWorld.getCollidingBoundingBoxes(player, player.entityBoundingBox.offset(0.0, 3.0001, 0.0).expand(0.0, 0.0, 0.0)).isEmpty()) {
            if (damage)
                sendPacket(C04PacketPlayerPosition(player.posX, player.posY + 3.0001, player.posZ, false))

            sendPacket(C06PacketPlayerPosLook(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch, false))
            sendPacket(C06PacketPlayerPosLook(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch, true))
        }
        player.setPosition(player.posX, player.posY + yBoost.toDouble(), player.posZ)
    }

    override fun onDisable() {
        if (boostTicks > 0) {
            player?.stopXZ()
            mc.timer.timerSpeed = 1f
        }
    }

    override fun onUpdate() {
        player?.stopXZ()
        player?.stop()

        if (boostTicks == 0 && player.hurtTime > 0) {
            boostTicks = boostTicksValue
        }

        boostTicks--

        if (timerSlow) {
            if (player.ticksExisted % 3 == 0) {
                mc.timer.timerSpeed = 0.15f
            } else {
                mc.timer.timerSpeed = 0.08f
            }
        }

        strafe(boostMotion, true)
    }

    override fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer) {
            packet.onGround = true
        }
    }

    override fun onJump(event: JumpEvent) {
        event.cancelEvent()
    }
}