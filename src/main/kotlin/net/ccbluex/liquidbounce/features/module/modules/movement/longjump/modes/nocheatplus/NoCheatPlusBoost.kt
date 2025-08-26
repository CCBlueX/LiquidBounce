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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes.nocheatplus

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.movement.stopXZVelocity

/**
 * @anticheat NoCheatPlus
 * @anticheatVersion 3.16.1-SNAPSHOT-sMD5NET-b115s
 * @testedOn eu.loyisa.cn
 */
internal object NoCheatPlusBoost : Choice("NoCheatPlusBoost") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    val ncpBoost by float("NCPBoost", 4.25f, 1f..10f)

    val repeatable = tickHandler {
        if (ModuleLongJump.canBoost) {
            player.velocity.x *= ncpBoost.toDouble()
            player.velocity.z *= ncpBoost.toDouble()
            ModuleLongJump.boosted = true
        }
        ModuleLongJump.canBoost = false
    }

    val moveHandler = handler<PlayerMoveEvent> {
        if (!player.moving && ModuleLongJump.jumped) {
            player.stopXZVelocity()
        }
    }
}
