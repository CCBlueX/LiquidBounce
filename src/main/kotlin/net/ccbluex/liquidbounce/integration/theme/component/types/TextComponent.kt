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

package net.ccbluex.liquidbounce.integration.theme.component.types

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.render.Alignment

/**
 * A text component
 */
@Suppress("unused")
class TextComponent(
    text: String,
    enabled: Boolean = true,
    alignment: Alignment = Alignment.center()
) : Component("Text", enabled, alignment) {

    private val text by text("Text", text)
    private val color by color("Color", Color4b.WHITE)
    private val font by text("Font", "Inter")
    private val fontSize by int("Size", 14, 1.. 100, "px")

    private val decorations = tree(object : Configurable("Decorations") {
        val bold by boolean("Bold", false)
        val italic by boolean("Italic", false)
        val underline by boolean("Underline", false)
        val strikethrough by boolean("Strikethrough", false)
    })

    private val shadow = tree(object : ToggleableConfigurable(this, "Shadow",
        false) {
        val offsetX by int("OffsetX", 0, -10..10, "px")
        val offsetY by int("OffsetY", 0, -10..10, "px")
        val blurRadius by int("BlurRadius", 0, 0..10, "px")
        val color by color("Color", Color4b.BLACK)
    })

    private val glow = tree(object : ToggleableConfigurable(this, "Glow",
        false) {
        val radius by int("Radius", 0, 0..10, "px")
        val color by color("Color", Color4b.WHITE)
    })

    init {
        registerComponentListen()
    }

    enum class TextAlignment(override val choiceName: String) : NamedChoice {
        LEFT("Left"),
        CENTER("Center"),
        RIGHT("Right")
    }

}
