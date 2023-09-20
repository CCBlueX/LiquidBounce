/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable

/**
 * NoRotateSet module.
 *
 * Prevents the server from rotating your head.
 */
object ModuleNoRotateSet : Module("NoRotateSet", Category.PLAYER) {
    val mode = choices(
        "Mode", SilentAccept, arrayOf(
            SilentAccept, ResetRotation
        )
    )

    object ResetRotation : Choice("ResetRotation") {
        override val parent: ChoiceConfigurable
            get() = mode

        val rotationsConfigurable = tree(RotationsConfigurable())
    }

    object SilentAccept : Choice("SilentAccept") {
        override val parent: ChoiceConfigurable
            get() = mode
    }
}
