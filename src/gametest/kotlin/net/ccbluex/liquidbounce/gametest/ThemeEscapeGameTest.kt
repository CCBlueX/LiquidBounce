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
package net.ccbluex.liquidbounce.gametest

import com.mojang.blaze3d.platform.InputConstants
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.integration.screen.CustomScreenType
import net.ccbluex.liquidbounce.integration.screen.ScreenManager
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.network.chat.Component

/**
 * A theme page keeps Esc while a menu, form or search is open in it, so Esc closes that first and not the screen.
 * Handing Esc back, or the screen being replaced, lets Esc close the screen again; a screen change that something
 * cancels replaces nothing.
 */
class ThemeEscapeGameTest : FabricClientGameTest {

    override fun runTest(context: ClientGameTestContext) {
        context.waitForClient()
        val escape = InputConstants.getKey("key.keyboard.escape")

        context.setScreen { CustomSharedMinecraftScreen(CustomScreenType.CLICK_GUI) }
        keep(context, true)

        context.input.pressKey(escape)
        context.waitTicks(2)
        check(context.fromClient { it.gui.screen() is CustomSharedMinecraftScreen }) {
            "Esc closed the screen while its page kept Esc"
        }

        val cancelled = context.fromClient<Screen> { object : Screen(Component.literal("Cancelled")) {} }
        object : EventListener {}.on(ScreenEvent::class.java) { event ->
            if (event.screen === cancelled) event.cancelEvent()
        }.use {
            context.onClient { it.gui.setScreen(cancelled) }
        }
        context.input.pressKey(escape)
        context.waitTicks(2)
        check(context.fromClient { it.gui.screen() is CustomSharedMinecraftScreen }) {
            "Esc closed the screen after a cancelled screen change"
        }

        keep(context, false)
        context.input.pressKey(escape)
        context.waitTicks(2)
        check(context.fromClient { it.gui.screen() !is CustomSharedMinecraftScreen }) {
            "Esc did not close the screen after its page handed Esc back"
        }

        // A page that kept Esc and never handed it back must not keep it once its screen is shown again
        val screen = context.fromClient { CustomSharedMinecraftScreen(CustomScreenType.CLICK_GUI) }
        context.setScreen { screen }
        keep(context, true)
        context.setScreen { TitleScreen() }
        context.setScreen { screen }
        context.input.pressKey(escape)
        context.waitTicks(2)
        check(context.fromClient { it.gui.screen() !== screen }) {
            "Esc was still kept after the screen changed"
        }
    }

    private fun keep(context: ClientGameTestContext, keep: Boolean) = context.onClient { mc ->
        ScreenManager.escapeKeptBy = mc.gui.screen().takeIf { keep }
    }

}
