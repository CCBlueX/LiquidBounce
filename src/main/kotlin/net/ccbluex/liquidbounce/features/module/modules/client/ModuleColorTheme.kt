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

package net.ccbluex.liquidbounce.features.module.modules.client

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams.uboDirty
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.utils.toHex

object ModuleColorTheme : ClientModule(
    "ColorTheme",
    ModuleCategories.CLIENT,
    notActivatable = true,
    hide = true
) {
    val syncClientWithPalette by boolean("SyncClientWithPalette", true)

    // Custom palette
    object CustomPalette : Configurable("CustomPalette") {
        val firstColor by color("FirstColor",
            Color4b(0, 0, 0)).onChanged { uboDirty = true }
        val secondColor by color("SecondColor",
            Color4b(255, 255, 255)).onChanged { uboDirty = true }
    }

    val firstColor get() = CustomPalette.firstColor.toHex()
    val secondColor get() = CustomPalette.secondColor.toHex()

    // Palette Presets
    @Suppress("unused")
    enum class Color(override val choiceName: String, val colors: Array<String>) : NamedChoice {
        STANDART("Standart", arrayOf("4677ff", "465c99")),
        LUME("Lume", arrayOf("ff6b6b", "ffd93d")),
        NIGHT("Night", arrayOf("1b263b", "415a77")),
        VEIL("Veil", arrayOf("00f5d4", "3a0ca3")),
        WILD("Wild", arrayOf("2d6a4f", "4caf7f")),
        EMBER("Ember", arrayOf("ffb5a7", "fcd5ce")),
        SPACE("Space", arrayOf("6d597a", "c9b56b")),
        CUSTOM("Custom", arrayOf(firstColor, secondColor))
    }

    @Suppress("unused")
    enum class AccentColor(override val choiceName: String, val num: Int) : NamedChoice {
        FIRST("First", 0),
        SECOND("Second", 1),
    }

    @Suppress("unused")
    object Transparency : Configurable("Transparency") {
        val a by int("Value", 255, 0..255).onChanged { uboDirty = true }
        // Unique transparency for every module
        object AdaptiveA : ToggleableConfigurable(this@ModuleColorTheme, "AdaptiveList", true) {
            val breadCrumbs by int("Breadcrumbs", 75, 0..255)
            val blockESP by int("BlockESP", 125, 0..255)
            val blockOutline by int("BlockOutline", 125, 0..255)
            val esp by int("ESP", 125, 0..255)
            val hats by int("Hats", 200, 0..255)
            val itemChams by int("ItemChams", 255, 0..255).onChanged {
                uboDirty = true
            }
            val itemESP by int("ItemESP", 125, 0..255)
            val jumpcircle by int("JumpCircle", 255, 0..255)
            val particles by int("Particles", 255, 0..255)
            val tracers by int("Tracers", 255, 0..255)
        }
        val adaptiveA = tree(AdaptiveA)
    }

    val colors by enumChoice("Color", Color.STANDART).onChanged { uboDirty = true }

    val currentColors get() = getActiveColors()
    val accentColors = AccentColor.entries
    val accentColor by enumChoice("AccentColor", AccentColor.FIRST).onChanged { uboDirty = true }
    val nonAccentColor get() = accentColors.first { it.num != accentColor.num }


    init {
        tree(CustomPalette)
        tree(Transparency)
    }

    fun getActiveColors(): Array<String> {
        return if (colors == Color.CUSTOM) {
            arrayOf(CustomPalette.firstColor.toHex(), CustomPalette.secondColor.toHex())
        } else {
            colors.colors
        }
    }
}
