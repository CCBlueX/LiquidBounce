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
package net.ccbluex.liquidbounce.features.module.modules.combat.tpaura

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.modes.AStarMode
import net.ccbluex.liquidbounce.features.module.modules.combat.tpaura.modes.ImmediateMode
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.combat.attack
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.util.math.Vec3d

object ModuleTpAura : Module("TpAura", Category.COMBAT, disableOnQuit = true) {

    val mode = choices<TpAuraChoice>("Mode", ImmediateMode, arrayOf(ImmediateMode, AStarMode))

    val clickScheduler = tree(ClickScheduler(this, true))
    private val attackRange by float("AttackRange", 4.2f, 3f..5f)
    val targetTracker = tree(TargetTracker())

    val stuckChronometer = Chronometer()
    var desyncPlayerPosition: Vec3d? = null

    val attackRepeatable = repeatable {
        val position = desyncPlayerPosition ?: player.pos

        clickScheduler.clicks {
            val enemy = targetTracker.enemies()
                .filter { it.squaredBoxedDistanceTo(position) <= attackRange * attackRange }
                .minByOrNull { it.hurtTime } ?: return@clicks false

            enemy.attack(true, keepSprint = true)
            true
        }
    }

}

open class TpAuraChoice(name: String) : Choice(name) {

    override val parent: ChoiceConfigurable<TpAuraChoice>
        get() = ModuleTpAura.mode

}
