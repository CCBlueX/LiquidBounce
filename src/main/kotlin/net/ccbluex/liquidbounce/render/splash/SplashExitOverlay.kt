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
package net.ccbluex.liquidbounce.render.splash

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ScreenRenderEvent
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Overlay
import net.minecraft.util.ARGB
import net.minecraft.util.Util

/**
 * Fades the splash into the menu once the startup tasks are done and the menu can be seen, the same way the
 * loading overlay does when no tasks are left.
 */
class SplashExitOverlay : Overlay() {

    private val menu = MenuWait()
    private var start = -1L

    init {
        Splash.hurry()
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) {
        if (start == -1L && menu.isOver()) {
            start = Util.getMillis()
            Splash.request(SplashTimeline.Phase.OUTRO)
        }
        val fade = if (start == -1L) 0f else ((Util.getMillis() - start) / FADE_OUT_TIME).coerceIn(0f, 1f)

        mc.gui.screen()?.let { screen ->
            screen.extractRenderStateWithTooltipAndSubtitles(graphics, mouseX, mouseY, a)
            // Theme screens draw through this event
            EventManager.callEvent(ScreenRenderEvent(graphics, a))
        }

        graphics.nextStratum()
        val background = ARGB.multiplyAlpha(Splash.background, 1f - fade)
        graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), background)
        Splash.render(graphics, 1f, 1f - fade)

        if (fade >= 1f) {
            mc.gui.setOverlay(null)
        }
    }

    private companion object {
        const val FADE_OUT_TIME = 1000f
    }

}
