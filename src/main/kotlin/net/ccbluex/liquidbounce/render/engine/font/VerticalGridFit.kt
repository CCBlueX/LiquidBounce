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
package net.ccbluex.liquidbounce.render.engine.font

import kotlin.math.floor

/**
 * Light vertical hinting in the way of FreeType's autohinter, which Chromium's text gets on Linux:
 * the x-height and cap height land on whole device pixels, so the tops of most letters stay sharp.
 * Heights in between are stretched linearly, as the autohinter interpolates the points between
 * aligned edges.
 *
 * Works on heights above the baseline in the text's local units. Callers snap the baseline itself.
 */
internal class VerticalGridFit(private val metrics: TextMetrics) {

    var active = false
        private set

    /** Heights the fit bends at, from the top down. */
    val cuts = FloatArray(3)

    private var scale = 1f
    private var xHeight = 0f
    private var fittedXHeight = 0f
    private var capHeight = 0f
    private var fittedCapHeight = 0f

    /**
     * @param pixelsPerUnit device pixels per local unit
     * @param textScale local units per atlas pixel
     */
    fun update(pixelsPerUnit: Float, textScale: Float) {
        val toPixels = pixelsPerUnit * textScale

        // af_latin_metrics_scale_dim: scale so the round x-height fits, rounding up from 3/8 of a pixel
        val roundXHeight = metrics.roundXHeight * toPixels
        val scale = floor(roundXHeight + 0.625f) / roundXHeight
        val xHeight = floor(metrics.xHeight * toPixels * scale + 0.5f)
        val capHeight = floor(metrics.capHeight * toPixels * scale + 0.5f)

        active = roundXHeight > 0f && xHeight > 0f && capHeight > xHeight && metrics.capHeight > metrics.xHeight
        if (!active) {
            return
        }

        this.scale = scale
        this.xHeight = metrics.xHeight * textScale
        this.capHeight = metrics.capHeight * textScale
        fittedXHeight = xHeight / pixelsPerUnit
        fittedCapHeight = capHeight / pixelsPerUnit

        cuts[0] = this.capHeight
        cuts[1] = this.xHeight
        cuts[2] = 0f
    }

    fun disable() {
        active = false
    }

    fun fit(height: Float): Float = when {
        height <= 0f -> height * scale
        height <= xHeight -> height * fittedXHeight / xHeight
        height <= capHeight ->
            fittedXHeight + (height - xHeight) * (fittedCapHeight - fittedXHeight) / (capHeight - xHeight)
        else -> fittedCapHeight + (height - capHeight) * scale
    }

}
