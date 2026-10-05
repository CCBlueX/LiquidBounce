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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MouseScrollInHotbarEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinMouseHandler
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.input.InputBind
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.round

/**
 * Module Zoom
 *
 * Allows you to zoom.
 *
 * The mouse is slowed down with the help of mixins in [MixinMouseHandler].
 */
object ModuleZoom : ClientModule("Zoom", ModuleCategories.RENDER, bindAction = InputBind.BindAction.HOLD) {

    val zoom by int("Zoom", 30, 10..150)

    object Scroll : ToggleableValueGroup(this, "Scroll", true) {

        val speed by float("Speed", 2f, 0.5f..8f)

        @Suppress("unused")
        val onScroll = handler<MouseScrollInHotbarEvent> {
            previousFov = getFov(true)
            targetFov = (targetFov - round(it.speed * this.speed).toInt()).coerceIn(1, 179)
            reset()
            it.cancelEvent()
        }

    }

    init {
        tree(Scroll)
    }

    private val transition by easing("Transition", Easing.QUAD_IN)
    private val durationFactor by float("DurationFactor", 2f, 0f..10f, "x")

    private val chronometer = Chronometer()
    // TODO: Whole degrees only, so re-anchoring a transition (scroll wheel, re-enabling) snaps the displayed
    //  fov back to this integer base and can jump by up to one degree. Holding the displayed fov as a float
    //  would remove that last step.
    private var targetFov = 0
    private var previousFov = 0
    private var scaledDifference = 0.0
    private var disableAnimationFinished = true

    override fun onEnabled() {
        // Continue from the fov that is currently on screen: the disabling animation may still be running,
        // so starting over from the default fov would snap the view instead of transitioning smoothly.
        previousFov = getFov(false, getDefaultFov())
        targetFov = zoom
        reset()
    }

    override fun onDisabled() {
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

        val factor = progress()
        if (!enabled && factor == 1f) {
            disableAnimationFinished = true
        }

        return Mth.lerpInt(transition.transform(factor), previousFov, targetFov)
    }

    /**
     * The fov of the client is built on an [Int] base fov, so a transition can only move in whole degrees:
     * a curve which comes to rest slowly, like [Easing.EXPONENTIAL_OUT], then spends its tail standing
     * still and dropping one degree at a time. This puts the fraction, the integer base cannot carry, back
     * into the projection matrix.
     */
    fun applyFractionalFov(vanillaFov: Float): Float {
        // Same guard as [getFov]: while it hands out the vanilla fov, there is no fraction of ours to add
        // back, and rescaling would keep the offset the integer base dropped alive forever.
        if (!inGame || (!running && disableAnimationFinished)) {
            return vanillaFov
        }

        val eased = transition.transform(progress())
        val whole = Mth.lerpInt(eased, previousFov, targetFov)
        if (whole == 0) {
            return vanillaFov
        }

        return vanillaFov * (Mth.lerp(eased, previousFov.toFloat(), targetFov.toFloat()) / whole)
    }

    private fun getDefaultFov(): Int {
        val fov = mc.options.fov().get()
        return if (ModuleNoFov.running) ModuleNoFov.getFov(fov) else fov
    }

    private fun reset() {
        chronometer.reset()
        scaledDifference = durationFactor.toDouble() * abs(targetFov - previousFov)
    }

    private fun progress(): Float = if (scaledDifference <= 0.0 || !scaledDifference.isFinite()) {
        1f
    } else {
        (chronometer.elapsed / scaledDifference).toFloat().coerceIn(0F, 1F)
    }

}
