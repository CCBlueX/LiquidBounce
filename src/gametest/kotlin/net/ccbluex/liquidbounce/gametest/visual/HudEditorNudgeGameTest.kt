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
package net.ccbluex.liquidbounce.gametest.visual

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.gametest.fromClient
import net.ccbluex.liquidbounce.gametest.onClient
import net.ccbluex.liquidbounce.gametest.waitForClient
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * Moving the selected component with the arrow keys in the HUD editor.
 */
class HudEditorNudgeGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        // The ClickGUI page is loaded once the world is joined and opens on the tab it was left on
        context.onClient { PersistentLocalStorage.map[TAB_STORAGE_KEY] = HUD_EDITOR_TAB }

        context.inVisualTestWorld {
            context.onClient { ModuleClickGui.enabled = true }
            context.waitFor({ ModuleHud.hudEditorSelected }, SETTLE_TIMEOUT_TICKS)
            context.waitTicks(SETTLE_TICKS)

            val alignment = context.fromClient {
                checkNotNull(ThemeManager.theme).components.first { it.name == "Watermark" }.alignment
            }
            val left = context.fromClient { alignment.horizontalOffset }
            val top = context.fromClient { alignment.verticalOffset }

            context.input.pressKey(InputConstants.KEY_RIGHT)
            context.waitTicks(SETTLE_TICKS)
            check(context.fromClient { alignment.horizontalOffset } == left) {
                "the watermark moved without being selected"
            }

            // The watermark is anchored at the top left, in HUD pixels of GUI scale 2
            val guiScale = context.fromClient { client -> client.window.guiScale }
            click(context, (left + CLICK_INSET) * guiScale / 2.0, (top + CLICK_INSET) * guiScale / 2.0)

            context.input.pressKey(InputConstants.KEY_RIGHT)
            context.waitFor({ alignment.horizontalOffset == left + 1 }, SETTLE_TICKS * 2)
            check(context.fromClient { alignment.verticalOffset } == top) { "the watermark moved vertically" }

            context.input.pressKey(InputConstants.KEY_DOWN)
            context.waitFor({ alignment.verticalOffset == top + 1 }, SETTLE_TICKS * 2)
            check(context.fromClient { alignment.horizontalOffset } == left + 1) {
                "the watermark moved horizontally"
            }

            context.waitTicks(2)
            context.takeScreenshot("visual_hud_editor_nudged")
            context.onClient { client -> client.gui.setScreen(null) }
        }
    }

    /**
     * Clicks the window at [x], [y].
     */
    private fun click(context: ClientGameTestContext, x: Double, y: Double) {
        // The mouse handler only takes over the position of the first move after the cursor was grabbed
        repeat(2) { context.input.setCursorPos(x, y) }
        context.waitTicks(2)
        context.input.holdMouseFor(InputConstants.MOUSE_BUTTON_LEFT, 2)
        context.waitTicks(2)
    }

    private companion object {
        const val SETTLE_TICKS = 40
        const val CLICK_INSET = 6
        const val TAB_STORAGE_KEY = "clickgui.tab"
        const val HUD_EDITOR_TAB = "HUD Editor"
    }

}
