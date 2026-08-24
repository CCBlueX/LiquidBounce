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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories

/**
 * TridentBoost module
 *
 * Strengthens the riptide dash of a trident and allows it to be used on dry land.
 */
object ModuleTridentBoost : ClientModule("TridentBoost", ModuleCategories.MOVEMENT) {

    private val horizontalMultiplier by float("HorizontalMultiplier", 2f, 0.1f..5f)
    private val verticalMultiplier by float("VerticalMultiplier", 2f, 0.1f..5f)
    private val onLand by boolean("OnLand", default = true)

    @JvmStatic
    fun scaleHorizontal(original: Double) = if (running) original * horizontalMultiplier else original

    @JvmStatic
    fun scaleVertical(original: Double) = if (running) original * verticalMultiplier else original

    @JvmStatic
    fun ignoresWaterRequirement() = running && onLand

}
