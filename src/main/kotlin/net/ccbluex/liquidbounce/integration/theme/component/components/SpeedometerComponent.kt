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

package net.ccbluex.liquidbounce.integration.theme.component.components

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeComponent
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment

/**
 * A speedometer HUD component that displays player movement speed
 */
@Suppress("unused")
object SpeedometerComponent : NativeComponent("Speedometer", true, Alignment.center()) {

    private val unit by enumChoice("Unit", SpeedUnit.KMH)
    private val average by boolean("Average", false)
    private val color by color("Color", Color4b.WHITE)
    private val font by text("Font", "Inter")
    private val fontSize by int("Size", 14, 1..100, "px")

    private val shadow = tree(object : ToggleableConfigurable(this, "Shadow", false) {
        val offsetX by int("OffsetX", 0, -10..10, "px")
        val offsetY by int("OffsetY", 0, -10..10, "px")
        val blurRadius by int("BlurRadius", 0, 0..10, "px")
        val color by color("Color", Color4b.BLACK)
    })

    private val glow = tree(object : ToggleableConfigurable(this, "Glow", false) {
        val radius by int("Radius", 0, 0..10, "px")
        val color by color("Color", Color4b.WHITE)
    })

    init {
        registerComponentListen(this)
    }

    enum class SpeedUnit(override val choiceName: String) : NamedChoice {
        KMH("km/h"),
        BPS("b/s")
    }

}
