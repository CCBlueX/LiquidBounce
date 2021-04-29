package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object ModuleFreeCam : Module("FreeCam", Category.PLAYER) {

    private val speed by float("Speed", 1f, 0.1f..2f)
    private val fly by boolean("Fly", true)
    private val collision by boolean("Collision", false)
    private val resetMotion by boolean("ResetMotion", true)
    private val spoofMovement by boolean("SpoofMovement", true)

    private var fakePlayer: OtherClientPlayerEntity? = null
    private var x = 0.0
    private var y = 0.0
    private var z = 0.0
    private var posX = 0.0
    private var posY = 0.0
    private var posZ = 0.0
    private var ground = false

    override fun enable() {
        if (mc.player != null) {
            if (resetMotion) {
                player.setVelocity(0.0, 0.0, 0.0)
            }
            x = 0.0
            y = 0.0
            z = 0.0
            posX = player.x
            posY = player.y
            posZ = player.z
            ground = player.isOnGround
            val faker = OtherClientPlayerEntity(world, player.gameProfile)

            faker.headYaw = player.headYaw
            faker.copyPositionAndRotation(player)
            world.addEntity(faker.entityId, faker)
            fakePlayer = faker

            if (!collision) {
                player.noClip = true
            }
        }
    }

    override fun disable() {
        if (mc.player != null || mc.world != null || fakePlayer != null) {
            player.updatePositionAndAngles(fakePlayer!!.x, fakePlayer!!.y, fakePlayer!!.z, player.yaw, player.pitch)
            world.removeEntity(fakePlayer!!.entityId)
            fakePlayer = null
            player.setVelocity(x, y, z)
        }
    }

    val repeatable = repeatable {
        if (mc.world == null) {
            enabled = false
        } else {
            // Just to make sure it stays enabled
            if (!collision) {
                player.noClip = true
                player.fallDistance = 0f
                player.isOnGround = false
            }

            if (fly) {
                val speed = speed.toDouble()
                if (player.moving) {
                    player.strafe(speed = speed)
                }

                player.velocity.y = when {
                    mc.options.keyJump.isPressed -> speed
                    mc.options.keySneak.isPressed -> -speed
                    else -> 0.0
                }
            }
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            // For better FreeCam detecting AntiCheats, we need to prove to them that the player's moving
            is PlayerMoveC2SPacket -> {
                if (spoofMovement) {
                    if (packet.changePosition) {
                        packet.x = posX
                        packet.y = posY
                        packet.z = posZ
                    }
                    packet.onGround = ground
                    if (packet.changeLook) {
                        packet.yaw = player.yaw
                        packet.pitch = player.pitch
                    }
                } else {
                    event.cancelEvent()
                }
            }
            is PlayerActionC2SPacket -> event.cancelEvent()
            // In case of a teleport
            is PlayerPositionLookS2CPacket -> {
                fakePlayer!!.updatePosition(packet.x, packet.y, packet.z)
                // Reset the motion
                event.cancelEvent()
            }
        }
    }
}
