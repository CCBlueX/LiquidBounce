/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.Timer.timerSpeed
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * TimerRange module
 *
 * Automatically speeds up, when you are near enemy.
 */
@IncludeModule
object ModuleTimerRange : Module("TimerRange", Category.COMBAT) {

    private val timerBalanceLimit by float("TimerBalanceLimit", 20f, 0f..50f)
    private val normalSpeed by float("NormalSpeed", 0.9F, 0.1F..10F)
    private val boostSpeed by float("BoostTimer", 2F, 0.1F..10F).apply { tagBy(this) }
    private val balanceRecoveryIncrement by float("BalanceRecoveryIncrement", 1f, 1f..10f)
    private val distanceToSpeedUp by float("DistanceToSpeedUp", 3.5f, 0f..10f)
    private val distanceToStartWorking by float("DistanceToStartWorking", 100f, 0f..500f)
    private val pauseOnFlag by boolean("PauseOnFlag", true)

    private var reachedTheLimit = false
    private var balanceTimer = 0f

    override fun enable() {
        balanceTimer = timerBalanceLimit
        super.enable()
    }

    val repeatable = repeatable {
        val newTimerSpeed = updateTimerSpeed()

        if (newTimerSpeed != null) {
            Timer.requestTimerSpeed(newTimerSpeed, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleTimerRange)
        }

        val balanceChange = timerSpeed / balanceRecoveryIncrement - 1
        if ((balanceTimer > 0 || balanceChange > 0) && (balanceTimer < timerBalanceLimit * 2 || balanceChange < 0))
            balanceTimer += balanceChange

        if (balanceTimer <= 0) {
            reachedTheLimit = false
        }
    }

    private fun updateTimerSpeed(): Float? {
        if (world.findEnemy(0f..distanceToStartWorking) == null) {
            return null
        }

        if (world.findEnemy(0f..distanceToSpeedUp) == null) {
            return normalSpeed
        }

        return if (balanceTimer < timerBalanceLimit * 2 && !reachedTheLimit)
            boostSpeed
        else {
            reachedTheLimit = true

            // if we slowdown while enemy is close we might easily loose
            1.0f
        }
    }

    val packetHandler = handler<PacketEvent> {
        if (it.packet is PlayerPositionLookS2CPacket && pauseOnFlag) {
            balanceTimer = timerBalanceLimit * 2
        }
        // Stops speeding up when you got flagged
    }

}
