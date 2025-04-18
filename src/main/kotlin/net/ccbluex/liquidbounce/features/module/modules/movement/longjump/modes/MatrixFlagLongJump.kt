package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import kotlin.math.*

/**
 * @anticheat Matrix
 * @anticheatVersion 7.14.5
 * @testedOn mc.loyisa.cn
 */
internal object MatrixFlagLongJump : Choice("MatrixFlag") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    private val boostSpeed by float("BoostSpeed", 1.97f, 0.1f..5f)
    private val motionY by float("MotionY", 0.42f, 0.0f..5.0f)
    private val delay by int("Delay", 0, 0..3)

    private var movementYaw = 0.0
    private var flagTicks = 0
    private var jumped = false
    private var shouldBoost = false

    override fun enable() {
        if (!player.isOnGround) ModuleLongJump.enabled = false
    }

    override fun disable() {
        flagTicks = 0
        jumped = false
        shouldBoost = false
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (player.airTicks >= delay) shouldBoost = true

        if (flagTicks > 1 && !jumped && player.isOnGround) flagTicks = 0

        if (player.isOnGround) {
            movementYaw = Math.toRadians(player.yaw.toDouble())
            jumped = true
            return@tickHandler
        }

        if (jumped && shouldBoost) {
            if (flagTicks <= 1) {
                player.movement.x = -sin(movementYaw) * boostSpeed
                player.movement.y = motionY.toDouble()
                player.movement.z = cos(movementYaw) * boostSpeed
            }
            if (flagTicks >= 1) {
                jumped = false
                if (ModuleLongJump.autoDisable) {
                    ModuleLongJump.enabled = false
                }
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlayerPositionLookS2CPacket) flagTicks++
    }
}
