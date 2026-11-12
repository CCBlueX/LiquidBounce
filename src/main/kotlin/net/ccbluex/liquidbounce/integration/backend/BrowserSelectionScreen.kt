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

package net.ccbluex.liquidbounce.integration.backend

import kotlinx.coroutines.CompletableDeferred
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.splash.SplashPhaseScreen
import net.ccbluex.liquidbounce.render.splash.SplashTimeline
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Splash phase that asks which browser backend to use, before any is loaded.
 */
class BrowserSelectionScreen(
    private val backends: List<BrowserBackendProvider>,
    private val selection: CompletableDeferred<BrowserBackendProvider>,
) : SplashPhaseScreen(translation("liquidbounce.splash.chooseBrowser"), SplashTimeline.Phase.CHOICE) {

    private val names = backends.map(BrowserBackendProvider::name)

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        extractSplash(graphics, null)

        val alpha = ((age - 200f) / 400f).coerceIn(0f, 1f)
        extractCenteredText(graphics, title.string, contentTop, alpha * 0.6f)
        extractOptions(graphics, names, contentTop + LIST_OFFSET, alpha, mouseX, mouseY)
    }

    override fun pickOption(index: Int) {
        selection.complete(backends[index])
        mc.gui.setScreen(null)
    }

    private companion object {
        const val LIST_OFFSET = 18f
    }

}
