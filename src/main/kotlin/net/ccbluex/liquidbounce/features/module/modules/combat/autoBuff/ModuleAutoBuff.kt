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

package net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff.features.Head
import net.ccbluex.liquidbounce.features.module.modules.combat.autoBuff.features.Soup

object ModuleAutoBuff : Module("AutoHeal", Category.COMBAT) {

    val features = arrayOf(
        Soup,
        Head
    )

    private val activeFeatures
        get() = features.filter { it.enabled && it.passesHealthRequirements }

    val repeatable = repeatable {
        for (feature in activeFeatures) {
            if (!feature.runIfPossible(this)) {
                // TODO: Implement Refill feature
                // TODO: Implement Auto Crafting feature
            }
        }
    }

}
