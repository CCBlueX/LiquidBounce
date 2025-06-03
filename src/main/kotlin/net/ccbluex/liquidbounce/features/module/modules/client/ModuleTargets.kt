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

package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.combat.Targets
import java.util.*

object ModuleTargets : ClientModule(
    name = "Targets",
    category = Category.CLIENT,
    notActivatable = true,
    hide = true,
    aliases = arrayOf("Enemies")
) {
    val combatConfigurable = multiEnumChoice("Combat",
        Targets.PLAYERS,
        Targets.HOSTILE,
        Targets.ANGERABLE,
        Targets.WATER_CREATURE,
        Targets.INVISIBLE,
    )

    val visualConfigurable = multiEnumChoice("Visual",
        Targets.PLAYERS,
        Targets.HOSTILE,
        Targets.ANGERABLE,
        Targets.WATER_CREATURE,
        Targets.INVISIBLE,
        Targets.FRIENDS
    )

    inline val combat: EnumSet<Targets> get() = combatConfigurable.get() as EnumSet

    inline val visual: EnumSet<Targets> get() = visualConfigurable.get() as EnumSet
}
