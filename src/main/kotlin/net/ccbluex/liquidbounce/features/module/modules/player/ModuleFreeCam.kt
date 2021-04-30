package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.packetHandler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.client.network.OtherClientPlayerEntity
import net.minecraft.network.Packet
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

    override fun enable() {
        if (mc.player == null) {
            return
        } else {
            if (resetMotion) {
                player.setVelocity(0.0, 0.0, 0.0)
            } else {
                x = player.velocity.x
                y = player.velocity.y
                z = player.velocity.z
            }
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
        if (mc.player == null || mc.world == null) {
            return
        } else {
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

    val packetHandler = packetHandler<Packet<*>> {
        when (val packet = packet) {
            // For better FreeCam detecting AntiCheats, we need to prove to them that the player's moving
            is PlayerMoveC2SPacket.PositionOnly, is PlayerMoveC2SPacket.LookOnly, is PlayerMoveC2SPacket.Both -> {
                if (spoofMovement) {
                    network.sendPacket(PlayerMoveC2SPacket(fakePlayer!!.isOnGround))
                    cancelEvent()
                }
            }
            is PlayerMoveC2SPacket, is PlayerActionC2SPacket -> cancelEvent()
            // In case of a teleport
            is PlayerPositionLookS2CPacket -> {
                fakePlayer!!.setPos(packet.x, packet.y, packet.z)
                // Reset the motion
                player.setVelocity(0.0, 0.0, 0.0)
                cancelEvent()
            }
        }
    }
}
