/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.entity.player.OtherClientPlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Both
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object FreeCam : Module("FreeCam", Category.RENDER, gameDetecting = false, hideModule = false) {

    private val speed by FloatValue("Speed", 0.8f, 0.1f..2f)
    private val fly by BoolValue("Fly", true)
    private val noClip by BoolValue("NoClip", true)
    private val motion by BoolValue("RecordMotion", true)
    private val c03Spoof by BoolValue("C03Spoof", false)

    private lateinit var fakePlayer: OtherClientPlayerEntity
    private var velocityX = 0.0
    private var velocityY = 0.0
    private var velocityZ = 0.0
    private var packetCount = 0

    override fun onEnable() {
        if (mc.player == null || mc.world == null) {
            return
        }

        if (motion) {
            velocityX = mc.player.velocityX
            velocityY = mc.player.velocityY
            velocityZ = mc.player.velocityZ
        } else {
            velocityX = 0.0
            velocityY = 0.0
            velocityZ = 0.0
        }

        packetCount = 0
        fakePlayer = OtherClientPlayerEntity(mc.world, mc.player.gameProfile)
        fakePlayer.copyFrom(mc.player, true)
        fakePlayer.headYaw = mc.player.headYaw
        fakePlayer.absorption = mc.player.absorption
        fakePlayer.copyPosition(mc.player)
        mc.world.addEntity(-1000, fakePlayer)
        if (noClip) {
            mc.player.noClip = true
        }
    }

    override fun onDisable() {
        if (mc.player == null || mc.world == null) {
            return
        }

        mc.player.updatePosition(fakePlayer.x, fakePlayer.y, fakePlayer.z)
        mc.world.removeEntity(fakePlayer.entityId)
        mc.player.velocityX = velocityX
        mc.player.velocityY = velocityY
        mc.player.velocityZ = velocityZ
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (noClip) {
            mc.player.noClip = true
        }

        mc.player.fallDistance = 0f

        if (fly) {
            mc.player.velocityY = 0.0
            mc.player.velocityX = 0.0
            mc.player.velocityZ = 0.0

            if (mc.options.jumpKey.isPressed) {
                mc.player.velocityY += speed
            }

            if (mc.options.sneakKey.isPressed) {
                mc.player.velocityY -= speed
            }

            strafe(speed)
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (c03Spoof) {
            if (packet is PlayerMoveC2SPacket && (packet.changeLook || packet.changePosition)) {
                if (packetCount >= 20) {
                    packetCount = 0
                    sendPacket(Both(fakePlayer.x, fakePlayer.y, fakePlayer.z, fakePlayer.yaw, fakePlayer.pitch, fakePlayer.onGround), false)
                } else {
                    packetCount++
                    sendPacket(PlayerMoveC2SPacket(fakePlayer.onGround), false)
                }
                event.cancelEvent()
            }
        } else if (packet is PlayerMoveC2SPacket) {
            event.cancelEvent()
        }

        if (packet is PlayerPositionLookS2CPacket) {
            fakePlayer.updatePosition(packet.x, packet.y, packet.z)
            // when teleported, reset motion

            velocityX = 0.0
            velocityY = 0.0
            velocityZ = 0.0

            // apply the flag to bypass some anticheats
            sendPacket(Both(fakePlayer.x, fakePlayer.y, fakePlayer.z, fakePlayer.yaw, fakePlayer.pitch, fakePlayer.onGround), false)

            event.cancelEvent()
        }
    }

    @EventTarget
    fun onWorldChange(event: WorldEvent) {
        // Disable when world changed
        state = false
    }

}