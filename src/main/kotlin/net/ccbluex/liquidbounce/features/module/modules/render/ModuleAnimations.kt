/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * Animations module
 *
 * This module affects item animations. It allows the user to customize the animation.
 * If you are looking forward to contribute to this module, please name your animation with a reasonable name. Do not name them after clients or yourself.
 * Please credit from where you got the animation from and make sure they are willing to contribute.
 * If they are not willing to contribute, please do not add the animation to this module.
 */

object ModuleAnimation : Module("Animations", Category.RENDER) {

    init {
        tree(MainHand)
        tree(OffHand)
    }

    val itemScale by float("ItemScale",  1f, -5f..5f)
    object MainHand : ToggleableConfigurable(this, "MainHand", false) {
        val mainHandX by float("MainHandX", 1f, -5f..5f)
        val mainHandY by float("MainHandY", 1f, -5f..5f)
        val mainHandPositiveX by float("MainHandPositiveRotationX", 1f, -50f..50f)
        val mainHandPositiveY by float("MainHandPositiveRotationY", 1f, -50f..50f)
        val mainHandPositiveZ by float("MainHandPositiveRotationZ", 1f, -50f..50f)
    }
    object OffHand : ToggleableConfigurable(this, "OffHand", false) {
        val offHandX by float("offHandX", 1f, -1f..1f)
        val offHandY by float("offHandY", 1f, -1f..1f)
        val OffHandPositiveX by float("OffHandPositiveRotationX", 1f, -50f..50f)
        val OffHandPositiveY by float("OffHandPositiveRotationY", 1f, -50f..50f)
        val OffHandPositiveZ by float("OffHandPositiveRotationZ", 1f, -50f..50f)
    }
}
