package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import kotlin.math.min

/**
 * TickBase module
 *
 * Calls tick function to speed up, when needed
 */

object ModuleTickBase : Module("TickBase", Category.COMBAT) {

    private val distanceToWork by floatRange("DistanceToWork", 3f..4f, 0f..10f)
    private val balanceRecoveryIncrement by float("BalanceRecoverIncrement", 1f, 0f..2f)
    private val balanceMaxValue by int("BalanceMaxValue", 20, 0..200)
    private val maxTicksAtATime by int("MaxTicksAtATime", 2, 1..20)
    private val pauseOnFlag by boolean("PauseOfFlag", true)
    private val pauseAfterTick by int("PauseAfterTick", 3, 0..100)
    private var ticksToSkip = 0
    private var tickBalance = 0f
    private var reachedTheLimit = false

    val repeatable = handler<PlayerTickEvent> {
        if (ticksToSkip-- > 0) {
            return@handler
        }
        if (tickBalance <= 0) {
            reachedTheLimit = true
        }
        if (tickBalance > balanceMaxValue / 2) {
            reachedTheLimit = false
        }
        if (tickBalance <= balanceMaxValue) {
            tickBalance += balanceRecoveryIncrement
        }
        if (world.findEnemy(distanceToWork.start..distanceToWork.endInclusive) != null && !reachedTheLimit) {
            // Tick as much as we can
            repeat(min(tickBalance.toInt(), maxTicksAtATime)) {
                player.tickMovement()
                tickBalance -= 1
            }
            ticksToSkip = pauseAfterTick
        }
    }

    val packetHandler = handler<PacketEvent> {
        if (it.packet is PlayerPositionLookS2CPacket && pauseOnFlag) tickBalance = 0f
        // Stops when you got flagged
    }

    override fun disable() {
        tickBalance = 0f
        super.disable()
    }
}
