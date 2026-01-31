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

package net.ccbluex.liquidbounce.features.global

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams.uboDirty
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.utils.toHex

object GlobalSettingsColorTheme : ToggleableValueGroup(
    name = "ColorTheme",
    enabled = true,
    aliases = listOf("Palette")
) {

    var color: Array<String> = arrayOf()

    // Custom palette
    object CustomPalette : ValueGroup("CustomPalette") {
        val firstColor by color("FirstColor",
            Color4b(0, 0, 0)).onChanged { uboDirty = true }
        val secondColor by color("SecondColor",
            Color4b(255, 255, 255)).onChanged { uboDirty = true }
    }

    val firstColor get() = CustomPalette.firstColor.toHex()
    val secondColor get() = CustomPalette.secondColor.toHex()


    // Palette Presets
    @Suppress("unused")
    enum class Palette(override val tag: String, val colors: Array<String>) : Tagged {
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
    enum class AccentColor(override val tag: String, val num: Int) : Tagged {
        FIRST("First", 0),
        SECOND("Second", 1),
    }

    object Alpha : ValueGroup("UnifiedAlpha") {
        val a by int("Value", 255, 0..255).onChanged { uboDirty = true }
    }

    @Suppress("unused")
    object AdaptiveList : ToggleableValueGroup(this@GlobalSettingsColorTheme, "AdaptiveList", true) {

        class AlphaGroup(name: String, default: Int = 255) :
            ToggleableValueGroup(this@AdaptiveList, name, true) {
            val alpha by int("Alpha", default, 0..255).onChanged { uboDirty = true }
        }

        val Breadcrumbs = tree(AlphaGroup("Breadcrumbs", 75))
        val BlockOutline = tree(AlphaGroup("BlockOutline", 125))
        val Crosshair = tree(AlphaGroup("Crosshair", 255))
        val ESP = tree(AlphaGroup("ESP", 125))
        val Hats = tree(AlphaGroup("Hats", 200))
        val ItemChams = tree(AlphaGroup("ItemChams", 255))
        val ItemESP = tree(AlphaGroup("ItemESP", 125))
        val JumpCircle = tree(AlphaGroup("JumpCircle", 255))
        val Particles = tree(AlphaGroup("Particles", 255))
        val Tracers = tree(AlphaGroup("Tracers", 255))

    }
    val palette by enumChoice("Palette", Palette.STANDART).onChanged { uboDirty = true }
    val currentColors get() = getActiveColors()
    val accentColors = AccentColor.entries
    val accentColor by enumChoice("AccentColor", AccentColor.FIRST).onChanged { uboDirty = true }
    val nonAccentColor get() = accentColors.first { it.num != accentColor.num }

    init {
        tree(CustomPalette)
        tree(Alpha)
        tree(AdaptiveList)
    }

    fun getActiveColors(): Array<String> {
        return if (palette == Palette.CUSTOM) {
            arrayOf(CustomPalette.firstColor.toHex(), CustomPalette.secondColor.toHex())
        } else {
            palette.colors
        }
    }
}
