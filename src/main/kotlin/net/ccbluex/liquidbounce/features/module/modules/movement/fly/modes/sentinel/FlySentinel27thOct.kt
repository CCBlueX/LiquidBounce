/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.sentinel

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.entity.strafe

/**
 * @anticheat Sentinel
 * @anticheatVersion 27.10.2023
 * @testedOn cubecraft.net
 *
 * @note Tested in SkyWars and EggWars, works fine and no automatic ban.
 * @note This is a very simple fly, it's not the best, but it's not bad either.
 * Bypasses Sentinel's fly check and is a little faster. Might can be improved.
 * This fly does not require any disabler.
 */
internal object FlySentinel27thOct : Choice("Sentinel27thOct") {

    override val parent: ChoiceConfigurable
        get() = ModuleFly.modes

    val repeatable = repeatable {
        player.velocity.y = 0.2
        player.strafe(speed = 0.34)
        waitTicks(5)
    }

}
