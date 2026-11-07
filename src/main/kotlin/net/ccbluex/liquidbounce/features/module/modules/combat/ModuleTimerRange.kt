/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.utils.percentageChance
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * TimerRange module
 *
 * Automatically speeds up when you are near an enemy.
 */

object ModuleTimerRange : ClientModule("TimerRange", ModuleCategories.COMBAT) {

    private val chance = percentageChance("Chance", 100f)
    private val normalSpeed by float("NormalSpeed", 0.9F, 0.1F..10F)
    private val inRangeSpeed by float("InRangeSpeed", 0.95F, 0.1F..10F)
    private val boostSpeed by float("BoostTimer", 2F, 0.1F..10F).apply { tagBy(this) }
    private val distanceToSpeedUp by float("DistanceToSpeedUp", 3.5f, 0f..10f)
    private val distanceToPause by float("DistanceToPause", 3f, 0f..10f)
    private val distanceToStartWorking by float("DistanceToStartWorking", 100f, 0f..500f)
    private val onlyOnGround by boolean("OnlyOnGround", false)

    private val requiresKillAura by boolean("RequiresKillAura", true)

    /**
     * Set once boosting used up the balance, until the balance is full again.
     */
    private var exhausted = false

    override fun onEnabled() {
        exhausted = false
        super.onEnabled()
    }

    val repeatable = tickHandler {
        if (onlyOnGround && !player.onGround()) return@tickHandler
        if (requiresKillAura && (!ModuleKillAura.running || ModuleKillAura.targetTracker.target == null)) {
            return@tickHandler
        }

        val speed = updateTimerSpeed() ?: return@tickHandler
        Timer.requestBalancedTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleTimerRange)
    }

    private fun updateTimerSpeed(): Float? {
        if (world.findEnemy(0f, distanceToPause) != null) {
            return 1.0f
        }

        if (world.findEnemy(0f, distanceToStartWorking) == null || !chance.asBoolean) {
            return null
        }

        if (world.findEnemy(0f, distanceToSpeedUp) == null) {
            return normalSpeed
        }

        if (Timer.balance <= 0f) {
            exhausted = true
        } else if (Timer.balance >= Timer.maxBalance) {
            exhausted = false
        }

        return if (exhausted) inRangeSpeed else boostSpeed
    }

}
