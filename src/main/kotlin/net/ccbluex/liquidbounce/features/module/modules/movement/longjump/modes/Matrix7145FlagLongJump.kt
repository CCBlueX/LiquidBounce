package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * @anticheat Matrix
 * @anticheatVersion 7.14.5
 * @testedOn mc.loyisa.cn
 */
internal object Matrix7145FlagLongJump : Choice("Matrix-7.14.5-Flag") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    private val boostSpeed by float("BoostSpeed", 1.97f, 0.1f..5f)
    private val motionY by float("MotionY", 0.42f, 0.0f..5.0f)
    private val delay by int("Delay", 0, 0..3)

    private var flagTicks = 0
    private const val ACCEPTED_AIR_TIME = 5

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (!player.isOnGround) {
            return@tickHandler
        }

        onCancellation { flagTicks = 0 }

        // Wait until we are not on ground and reached the delay
        waitUntil { !player.isOnGround && player.airTicks >= delay }

        val yaw = player.yaw
        // Repeat the jump until we get at least 2 flags and have not floated for too long
        while (flagTicks < 2 && player.airTicks < ACCEPTED_AIR_TIME) {
            player.velocity = player.velocity
                .withStrafe(speed = boostSpeed.toDouble(), yaw = yaw, input = null)
                .copy(y = motionY.toDouble())

            // On the first flag, we wait for the player to be on ground
            if (flagTicks == 1) {
                waitUntil { player.isOnGround || player.airTicks >= ACCEPTED_AIR_TIME }
            }
            waitTicks(1)
        }

        // Reset
        waitUntil { player.isOnGround }
        flagTicks = 0
        if (ModuleLongJump.autoDisable) {
            ModuleLongJump.enabled = false
        }
    }

    override fun disable() {
        flagTicks = 0
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlayerPositionLookS2CPacket) {
            flagTicks++
        }
    }

}
