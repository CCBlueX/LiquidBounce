package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

internal object NoFallPacketJump : Choice("PacketJump") {
    private val packetType by enumChoice("PacketType", MovePacketType.FULL,
        arrayOf(MovePacketType.FULL, MovePacketType.POSITION_AND_ON_GROUND))
    private val fallDistance = choices("FallDistance", Smart, arrayOf(Smart, Constant))
    private val timing = choices("Timing", Landing, arrayOf(Landing, Falling))

    private var falling = false

    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    val tickHandler = handler<PlayerTickEvent> {
        falling = player.fallDistance > fallDistance.activeChoice.value
        if (timing.activeChoice is Falling && !player.isOnGround && falling) {
            network.sendPacket(packetType.generatePacket().apply {
                y += 1.0E-9
            })
            if (Falling.resetFallDistance) {
                player.onLanding()
            }
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (timing.activeChoice is Landing && event.packet is PlayerMoveC2SPacket && event.packet.onGround && falling) {
            falling = false
            network.sendPacket(packetType.generatePacket().apply {
                x = player.lastX
                y = player.lastBaseY + 1.0E-9
                z = player.lastZ
                onGround = false
            })
        }
    }

    private object Landing : Choice("Landing") {
        override val parent: ChoiceConfigurable<*>
            get() = timing
    }

    private object Falling : Choice("Falling") {
        override val parent: ChoiceConfigurable<*>
            get() = timing

        val resetFallDistance by boolean("ResetFallDistance", true)
    }

    private abstract class DistanceMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = fallDistance

        abstract val value: Float
    }

    private object Smart : DistanceMode("Smart") {
        override val value: Float
            get() = player.getAttributeValue(EntityAttributes.SAFE_FALL_DISTANCE).toFloat()
    }

    private object Constant : DistanceMode("Constant") {
        override val value by float("Value", 3f, 0f..5f)
    }
}
