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

package net.ccbluex.liquidbounce.features.global

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.utils.combat.Targets
import java.util.EnumSet

object GlobalSettingsTarget : ValueGroup(
    name = "Targets",
    aliases = listOf("Enemies")
) {

    val combatChoices = multiEnumChoice("Combat",
        default = enumSetOf(
            Targets.PLAYERS,
            Targets.HOSTILE,
            Targets.ANGERABLE,
            Targets.WATER_CREATURE,
            Targets.INVISIBLE,
        ),
        choices = EnumSet.allOf(Targets::class.java).apply { remove(Targets.SELF) }
    )

    val visualChoices = multiEnumChoice("Visual",
        default = enumSetOf(
            Targets.PLAYERS,
            Targets.HOSTILE,
            Targets.ANGERABLE,
            Targets.WATER_CREATURE,
            Targets.INVISIBLE,
        ),
        choices = EnumSet.allOf(Targets::class.java)
    )

    inline val combat: EnumSet<Targets> get() = combatChoices.get() as EnumSet

    inline val visual: EnumSet<Targets> get() = visualChoices.get() as EnumSet
}
