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
package net.ccbluex.liquidbounce.features.module.modules.movement.spider

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes.SpiderPolar29thMarch2025
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes.SpiderVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.spider.modes.SpiderVulcan288

object ModuleSpider : ClientModule("Spider", Category.MOVEMENT, aliases = arrayOf("WallClimb")) {

    init {
        enableLock()
    }

    internal val modes = choices("Mode", SpiderVanilla, arrayOf(
        SpiderVanilla,
        SpiderPolar29thMarch2025,
        SpiderVulcan288
    ))
}

