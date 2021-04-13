/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.minecraft.block.Blocks

/**
 * A eagle module
 *
 * Legit trick to build faster
 */
object ModuleEagle : Module("Eagle", Category.PLAYER) {

    val repeatable = repeatable {
        // Check if player is on the edge and is NOT flying
        val nothing = player.blockPos.down().getBlock() == Blocks.AIR && player.canFly()

        // Sneak when player is at the edge
        mc.options.keySneak.isPressed = nothing
    }

    override fun disable() {
        mc.options.keySneak.isPressed = false // Default back to off
        super.disable()
    }

}
