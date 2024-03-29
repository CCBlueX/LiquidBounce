/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * NoSwing module
 *
 * Disables the swing effect.
 */

object ModuleNoSwing : Module("NoSwing", Category.RENDER) {
    private val mode by enumChoice("Mode", Mode.HIDE_BOTH)

    fun shouldHideForServer() = this.enabled && mode.hideServerSide
    fun shouldHideForClient() = this.enabled && mode.hideClientSide

    private enum class Mode(
        override val choiceName: String,
        val hideClientSide: Boolean,
        val hideServerSide: Boolean
    ): NamedChoice {
        HIDE_BOTH("HideForBoth", true, true),
        HIDE_CLIENT("HideForClient", true, false),
        HIDE_SERVER("HideForServer", false, true),
    }
}
