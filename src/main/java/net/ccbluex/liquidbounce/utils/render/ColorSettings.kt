/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.Element.Companion.MAX_GRADIENT_COLORS
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import java.awt.Color

class ColorSettingsFloat(owner: Any, name: String, val index: Int? = null, generalApply: () -> Boolean = { true }) {
    private val r by FloatValue("$name-R${index ?: ""}", if ((index ?: 0) % 3 == 1) 255f else 0f, 0f..255f) { generalApply() }
    private val g by FloatValue("$name-G${index ?: ""}", if ((index ?: 0) % 3 == 2) 255f else 0f, 0f..255f) { generalApply() }
    private val b by FloatValue("$name-B${index ?: ""}", if ((index ?: 0) % 3 == 0) 255f else 0f, 0f..255f) { generalApply() }

    fun color() = Color(r / 255f, g / 255f, b / 255f)

    init {
        when (owner) {
            is Element -> owner.addConfigurable(this)
            is Module -> owner.addConfigurable(this)
            // Should any other class use this, add here
        }
    }

    companion object {
        fun create(
            owner: Any, name: String, colors: Int = MAX_GRADIENT_COLORS, generalApply: (Int) -> Boolean = { true },
        ): List<ColorSettingsFloat> {
            return (1..colors).map { ColorSettingsFloat(owner, name, it) { generalApply(it) } }
        }
    }
}

class ColorSettingsInteger(
    owner: Any, name: String? = null, val index: Int? = null, withAlpha: Boolean = true, zeroAlphaCheck: Boolean = false,
    alphaApply: Boolean? = null, generalApply: () -> Boolean = { true },
) {
    val string = if (name == null) "" else "$name-"

    var r by IntegerValue("${string}R${index ?: ""}", 255, 0..255) { generalApply() && (!zeroAlphaCheck || a > 0) }
    var g by IntegerValue("${string}G${index ?: ""}", 0, 0..255) { generalApply() && (!zeroAlphaCheck || a > 0) }
    var b by IntegerValue("${string}B${index ?: ""}", 0, 0..255) { generalApply() && (!zeroAlphaCheck || a > 0) }
    var a by IntegerValue("${string}Alpha${index ?: ""}", 255, 0..255) { alphaApply ?: generalApply() && withAlpha }

    fun color(a: Int = this.a) = Color(r, g, b, a)

    init {
        when (owner) {
            is Element -> owner.addConfigurable(this)
            is Module -> owner.addConfigurable(this)
            // Should any other class use this, add here
        }
    }

    companion object {
        fun create(
            owner: Any, name: String, colors: Int, withAlpha: Boolean = true, zeroAlphaCheck: Boolean = true,
            generalApply: (Int) -> Boolean = { true },
        ): List<ColorSettingsInteger> {
            return (1..colors).map {
                ColorSettingsInteger(owner,
                    name,
                    it,
                    withAlpha,
                    zeroAlphaCheck
                ) { generalApply(it) }
            }
        }
    }
}

fun List<ColorSettingsFloat>.toColorArray(max: Int) = (0 until MAX_GRADIENT_COLORS).map {
    val colors = this[it].color()

    floatArrayOf(colors.red.toFloat() / 255f, colors.green.toFloat() / 255f, colors.blue.toFloat() / 255f, if (it > max - 1) 0f else 1f)
}