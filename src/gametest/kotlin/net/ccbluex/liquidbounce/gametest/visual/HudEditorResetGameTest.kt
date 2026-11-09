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
import net.ccbluex.liquidbounce.integration.theme.component.HudComponentManager
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext

/**
 * Resetting the HUD layout in the HUD editor with R, pressed twice to confirm.
 */
class HudEditorResetGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        // The ClickGUI page is loaded once the world is joined and opens on the tab it was left on
        context.onClient { PersistentLocalStorage.map[TAB_STORAGE_KEY] = HUD_EDITOR_TAB }

        context.inVisualTestWorld {
            context.onClient { ModuleClickGui.enabled = true }
            context.waitFor({ ModuleHud.hudEditorSelected }, SETTLE_TIMEOUT_TICKS)
            context.waitTicks(SETTLE_TICKS)

            val watermark = context.fromClient {
                checkNotNull(ThemeManager.theme).components.first { it.name == "Watermark" }
            }
            val alignment = watermark.alignment
            // The shared run directory may keep a moved watermark, the reset goes to its default
            val start = context.fromClient {
                watermark.resetAlignment()
                alignment.horizontalOffset
            }

            fun moveWatermark() = context.onClient {
                alignment.setFrom(
                    Alignment(
                        alignment.horizontalAlignment,
                        start + MOVED_BY,
                        alignment.verticalAlignment,
                        alignment.verticalOffset,
                    )
                )
                HudComponentManager.updateComponents()
            }

            moveWatermark()
            context.waitTicks(SETTLE_TICKS)

            context.input.pressKey(InputConstants.KEY_R)
            context.waitTicks(SETTLE_TICKS / 2)
            check(context.fromClient { alignment.horizontalOffset } == start + MOVED_BY) {
                "the layout was reset without confirming"
            }
            context.takeScreenshot("visual_hud_editor_reset_confirm")

            context.input.pressKey(InputConstants.KEY_R)
            context.waitFor({ alignment.horizontalOffset == start }, SETTLE_TICKS * 2)
            context.waitTicks(2)
            context.takeScreenshot("visual_hud_editor_reset")

            // The ClickGUI page stays loaded once closed, R in game must not reach it
            context.onClient { client -> client.gui.setScreen(null) }
            moveWatermark()
            context.waitTicks(SETTLE_TICKS)
            repeat(2) {
                context.input.pressKey(InputConstants.KEY_R)
                context.waitTicks(SETTLE_TICKS / 2)
            }
            check(context.fromClient { alignment.horizontalOffset } == start + MOVED_BY) {
                "the layout was reset outside of the HUD editor"
            }
            context.onClient { watermark.resetAlignment() }
        }
    }

    private companion object {
        const val SETTLE_TICKS = 40
        const val MOVED_BY = 40
        const val TAB_STORAGE_KEY = "clickgui.tab"
        const val HUD_EDITOR_TAB = "HUD Editor"
    }

}
