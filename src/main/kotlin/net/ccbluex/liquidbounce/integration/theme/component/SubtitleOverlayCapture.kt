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

package net.ccbluex.liquidbounce.integration.theme.component

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClosedCaptionDirection
import net.ccbluex.liquidbounce.event.events.ClosedCaptionEntry
import net.ccbluex.liquidbounce.event.events.ClosedCaptionsEvent
import net.minecraft.network.chat.Component

object SubtitleOverlayCapture {

    private val entries = mutableListOf<ClosedCaptionEntry>()
    private var active = false
    private var renderVanilla = true
    private var direction = ClosedCaptionDirection.NONE
    private var backgroundColor = 0
    private var hadEntries = false

    @JvmStatic
    fun begin() {
        check(!active) { "Subtitle overlay capture is already active" }

        entries.clear()
        resetEntryState()
        renderVanilla = !HudComponentManager.isTweakEnabled(HudComponentTweak.DISABLE_SUBTITLE_OVERLAY)
        active = true
    }

    @JvmStatic
    fun captureBackground(color: Int) {
        checkActive()
        backgroundColor = color
    }

    @JvmStatic
    fun captureDirection(text: String) {
        checkActive()
        direction = when (text) {
            "<" -> ClosedCaptionDirection.LEFT
            ">" -> ClosedCaptionDirection.RIGHT
            else -> direction
        }
    }

    @JvmStatic
    fun captureSubtitle(text: Component, textColor: Int) {
        checkActive()
        entries += ClosedCaptionEntry(text, direction, textColor, backgroundColor)
        resetEntryState()
    }

    @JvmStatic
    fun shouldRenderVanilla(): Boolean {
        checkActive()
        return renderVanilla
    }

    @JvmStatic
    fun publish() {
        checkActive()
        if (entries.isNotEmpty() || hadEntries) {
            EventManager.callEvent(ClosedCaptionsEvent(entries.toTypedArray()))
        }
        hadEntries = entries.isNotEmpty()
    }

    @JvmStatic
    fun end() {
        checkActive()
        entries.clear()
        resetEntryState()
        active = false
    }

    private fun checkActive() {
        check(active) { "Subtitle overlay capture is not active" }
    }

    private fun resetEntryState() {
        direction = ClosedCaptionDirection.NONE
        backgroundColor = 0
    }

}
