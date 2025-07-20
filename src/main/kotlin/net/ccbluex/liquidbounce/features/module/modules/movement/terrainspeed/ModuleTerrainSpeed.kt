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
package net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.fastclimb.FastClimb
import net.ccbluex.liquidbounce.features.module.modules.movement.terrainspeed.icespeed.IceSpeed

/**
 * TerrainSpeed module
 *
 * Move faster on specific surfaces.
 */
object ModuleTerrainSpeed : ClientModule("TerrainSpeed", Category.MOVEMENT, aliases = arrayOf("FastClimb")) {

    init {
        enableLock()
    }

    init {
        tree(FastClimb)
        tree(IceSpeed)
    }

}
