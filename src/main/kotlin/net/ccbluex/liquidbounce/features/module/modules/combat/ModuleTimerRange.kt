package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.timer
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * TimerRange module
 *
 * Increases your speed, when you are near an enemy.
 */

object ModuleTimerRange : Module("TimerRange", Category.COMBAT) {

    private val timerBalanceValue by float("TimerBalance", 10f, 0f..50f)
    private val distanceToSpeedUp by float("DistanceToSpeedUp", 3.2f, 0f..5f)
    private val distanceToStartWorking by float("DistanceToStartWorking", 50f, 5f..250f)
    private val normalSpeedValue by float("normalSpeed", 0.85F, 0.1F..10F)
    private val boostSpeedValue by float("BoostTimer", 1.5F, 0.1F..10F)

    private var balanceTimer = 0f
    private var reachedTheLimit = false

    override fun enable() {
        balanceTimer = timerBalanceValue
        super.enable()
    }

    val repeatable = repeatable {
        if (balanceTimer > 0 || mc.timer.timerSpeed - 1 > 0)
            balanceTimer += mc.timer.timerSpeed - 1
        if (balanceTimer <= 0)
            reachedTheLimit = false
        if (world.findEnemy(distanceToStartWorking) != null) {
            if (world.findEnemy(distanceToSpeedUp) != null) {
                if (!reachedTheLimit) {
                    if (balanceTimer < timerBalanceValue * 2)
                        mc.timer.timerSpeed = boostSpeedValue
                    else {
                        reachedTheLimit = true
                        mc.timer.timerSpeed = normalSpeedValue
                    }
                } else
                    mc.timer.timerSpeed = 1f
                    // When an enemy is near, but you can't speed up anymore you should use normal timer
            } else
                mc.timer.timerSpeed = normalSpeedValue
                // Starts slowing up when enemy isn't near, but you might speed up in the near future
        } else
            mc.timer.timerSpeed = 1f
            // There is no need to slow down, when you won't speed up in the near future
    }

    val packetHandler = handler<PacketEvent> {
        if (it.packet is PlayerPositionLookS2CPacket)
            balanceTimer = timerBalanceValue * 2
            // Stops speeding up when you got flagged
    }

    override fun disable() {
        mc.timer.timerSpeed = 1f
        super.disable()
    }
}
