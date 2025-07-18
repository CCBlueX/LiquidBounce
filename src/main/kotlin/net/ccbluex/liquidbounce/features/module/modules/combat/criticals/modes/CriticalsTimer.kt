/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoClicker
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals.wouldDoCriticalHit
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.kotlin.Priority

object CriticalsTimer : Choice("Timer") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleCriticals.modes

    private val speed by float("Speed", 0.8f, 0.1f..1.0f)
    private val range by float("Range", 4.0f, 0.0f..10.0f)

    // 新增配置项
    private val optimizeForCooldown by boolean("OptimizeForCooldown", true)
    private val checkKillAura by boolean("CheckKillAura", true)
    private val checkAutoClicker by boolean("CheckAutoClicker", true)

    private fun isActive(): Boolean {
        if (!ModuleCriticals.running) {
            return false
        }

        if (!checkKillAura && !checkAutoClicker) {
            return true
        }

        return (ModuleKillAura.running && checkKillAura) ||
            (ModuleAutoClicker.running && checkAutoClicker)
    }

    private fun calculateTicksUntilNextCrit(): Float {
        val durationToWait = player.attackCooldownProgressPerTick * 0.9F - 0.5F
        val waitedDuration = player.lastAttackedTicks.toFloat()

        return (durationToWait - waitedDuration).coerceAtLeast(0.0f)
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val world = mc.world ?: return@handler
        val player = mc.player ?: return@handler

        val enemy = world.findEnemy(0.0f..range) ?: return@handler

        if (!isActive()) {
            return@handler
        }

        if (optimizeForCooldown && calculateTicksUntilNextCrit() > 0.0f) {
            return@handler
        }

        if (wouldDoCriticalHit(ignoreSprint = true)) {
            Timer.requestTimerSpeed(speed, Priority.IMPORTANT_FOR_USAGE_1, ModuleCriticals)
        }
    }

}


