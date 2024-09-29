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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MouseScrollInHotbarEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinMouse
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.util.math.MathHelper
import kotlin.math.abs
import kotlin.math.round

/**
 * Module Zoom
 *
 * Allows you to zoom.
 *
 * The mose is slowed down with the help of mixins in [MixinMouse].
 */
object ModuleZoom : Module("Zoom", Category.RENDER, bindAction = BindAction.HOLD) {

    val zoom by int("Zoom", 30, 10..150)

    object Scroll : ToggleableConfigurable(this, "Scroll", true) {

        val speed by float("Speed", 2f, 0.5f..8f)

        @Suppress("unused")
        val onScroll = handler<MouseScrollInHotbarEvent> {
            previousFov = getFov(true)
            targetFov = (targetFov + round(it.speed * this.speed).toInt()).coerceIn(1..179)
            reset()
            it.cancelEvent()
        }

    }

    init {
        tree(Scroll)
    }

    private val chronometer = Chronometer()
    private var targetFov = 0
    private var previousFov = 0
    private var scaledDifference = 0.0
    private var disableAnimationFinished = true

    override fun enable() {
        targetFov = zoom
        previousFov = getDefaultFov()
        reset()
    }

    override fun disable() {
        previousFov = getFov(true)
        chronometer.reset()
        targetFov = getDefaultFov()
        reset()
        disableAnimationFinished = false
    }

    fun getFov(enabled: Boolean, original: Int = 0): Int {
        if (!enabled && disableAnimationFinished) {
            return original
        }

        val factor = (chronometer.elapsed / scaledDifference).toFloat().coerceIn(0F..1F)
        if (!enabled && factor == 1f) {
            disableAnimationFinished = true
        }

        return MathHelper.lerp(factor, previousFov, targetFov)
    }

    private fun getDefaultFov(): Int {
        val fov = mc.options.fov.value
        return if (ModuleNoFov.enabled) ModuleNoFov.getFov(fov) else fov
    }

    private fun reset() {
        chronometer.reset()
        scaledDifference = 2.5 * abs(targetFov - previousFov)
    }

}
