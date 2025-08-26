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
package net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkNoCheatPlus
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkVerusB3901
import net.ccbluex.liquidbounce.features.module.modules.movement.liquidwalk.modes.LiquidWalkVulcan291
import net.ccbluex.liquidbounce.utils.block.collideBlockIntersects
import net.ccbluex.liquidbounce.utils.block.isBlockAtPosition
import net.ccbluex.liquidbounce.utils.entity.box
import net.minecraft.block.FluidBlock

/**
 * LiquidWalk module
 *
 * Allows you to walk on water like jesus. Also known as Jesus module.
 */
object ModuleLiquidWalk : ClientModule("LiquidWalk", Category.MOVEMENT, aliases = arrayOf("Jesus", "WaterWalk")) {

    init {
        enableLock()
    }

    internal val modes = choices("Mode", LiquidWalkVanilla, arrayOf(
        LiquidWalkVanilla,
        LiquidWalkNoCheatPlus,
        LiquidWalkVerusB3901,
        LiquidWalkVulcan291,
    )).apply { tagBy(this) }

    /**
     * Check if player is standing on water
     */
    fun standingOnWater(): Boolean {
        val boundingBox = player.box
        val detectionBox = boundingBox.withMinY(boundingBox.minY - 0.01)

        return detectionBox.isBlockAtPosition { it is FluidBlock }
    }

    fun collidesWithAnythingElse(): Boolean {
        val boundingBox = player.box
        val detectionBox = boundingBox.withMinY(boundingBox.minY - 0.5)

        return detectionBox.collideBlockIntersects { it !is FluidBlock }
    }

}
