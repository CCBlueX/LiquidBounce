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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule

/**
 * Entity Control module
 *
 * Control rideable entities without a saddle
 */
object ModuleEntityControl : ClientModule("EntityControl", Category.MOVEMENT) {
    private val enforce by multiEnumChoice("Enforce", Enforce.entries)

    @JvmStatic
    val enforceSaddled get() = running && Enforce.SADDLED in enforce

    @JvmStatic
    val enforceJumpStrength get() = running && Enforce.JUMP_STRENGTH in enforce

    private enum class Enforce(
        override val choiceName: String
    ) : NamedChoice {
        SADDLED("Saddled"),
        JUMP_STRENGTH("JumpStrength"),
    }
}
