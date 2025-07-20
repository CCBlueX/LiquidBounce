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
package net.ccbluex.liquidbounce.features.spoofer

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.ToggleableConfigurable

@Suppress("SpellCheckingInspection")
object SpooferClient : ToggleableConfigurable(name = "ClientSpoofer", enabled = false) {

    val mode = choices(
        this,
        "Mode",
        Lunar,
        arrayOf(Vanilla, Geyser, Lunar, Cheatbreaker, Custom)
    )

    override val running: Boolean
        get() = this.enabled

    fun clientBrand(brand: String) = if (running) mode.activeChoice.getBrand() else brand

    private object Vanilla : SpoofMode("Vanilla") {
        override fun getBrand(): String = "vanilla"
    }

    private object Geyser : SpoofMode("Geyser") {
        override fun getBrand(): String = "geyser"
    }

    private object Lunar : SpoofMode("Lunar") {
        override fun getBrand(): String = "lunarclient:v2.16.8-2433"
    }

    private object Cheatbreaker : SpoofMode("Cheatbreaker") {
        override fun getBrand(): String = "CB"
    }

    private object Custom : SpoofMode("Custom") {

        val brandName by text("BrandName", "")

        override fun getBrand(): String = brandName

    }

    abstract class SpoofMode(name: String) : Choice(name) {

        override val parent: ChoiceConfigurable<*>
            get() = mode

        abstract fun getBrand(): String

    }

}
