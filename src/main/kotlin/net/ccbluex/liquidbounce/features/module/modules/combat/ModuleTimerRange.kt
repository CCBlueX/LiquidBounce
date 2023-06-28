package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.timer
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * TimerRange module
 *
 * Automatically speeds up, when you are near enemy.
 */

object ModuleTimerRange : Module("TimerRange", Category.COMBAT) {

    private val timerBalanceLimit by float("TimerBalanceLimit", 20f, 0f..50f)
    private val normalSpeed by float("normalSpeed", 0.9F, 0.1F..10F)
    private val boostSpeed by float("BoostTimer", 2F, 0.1F..10F)
    private val balanceRecoveryIncrement by float("BalanceRecoveryIncrement", 1f, 1f..10f)
    private val distanceToSpeedUp by float("DistanceToSpeedUp", 3.5f, 0f..10f)
    private val distanceToStartWorking by float("DistanceToStartWorking", 100f, 0f..500f)
    private val pauseOnFlag by boolean("PauseOnFlag", true)

    private val targetTracker = tree(TargetTracker())
    private var reachedTheLimit = false
    private var balanceTimer = 0f

    override fun enable() {
        balanceTimer = timerBalanceLimit
        super.enable()
    }

    val repeatable = repeatable {
        val balanceChange = mc.timer.timerSpeed / balanceRecoveryIncrement - 1
        if ((balanceTimer > 0 || balanceChange > 0) && (balanceTimer < timerBalanceLimit * 2 || balanceChange < 0))
            balanceTimer += balanceChange

        if (balanceTimer <= 0)
            reachedTheLimit = false

        if (targetTracker.enemies().any { player.distanceTo(it) < distanceToStartWorking && it != player } && !reachedTheLimit) {
            if (targetTracker.enemies().any { player.distanceTo(it) < distanceToSpeedUp && it != player }) {
                if (balanceTimer < timerBalanceLimit * 2)
                    mc.timer.timerSpeed = boostSpeed
                else {
                    reachedTheLimit = true
                    mc.timer.timerSpeed = normalSpeed
                }
            } else
                mc.timer.timerSpeed = normalSpeed
        } else
            mc.timer.timerSpeed = 1f
    }

    val packetHandler = handler<PacketEvent> {
        if (it.packet is PlayerPositionLookS2CPacket && pauseOnFlag)
            balanceTimer = timerBalanceLimit * 2
        // Stops speeding up when you got flagged
    }

    override fun disable() {
        mc.timer.timerSpeed = 1f
        super.disable()
    }
}
