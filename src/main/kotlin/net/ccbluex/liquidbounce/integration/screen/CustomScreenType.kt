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

package net.ccbluex.liquidbounce.integration.screen

import com.google.common.base.Predicates
import com.mojang.realmsclient.RealmsMainScreen
import net.ccbluex.liquidbounce.integration.screen.impl.CustomSharedMinecraftScreen
import net.ccbluex.liquidbounce.integration.screen.impl.InternetExplorerScreen
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.openVfpProtocolSelection
import net.minecraft.client.gui.screens.DisconnectedScreen
import net.minecraft.client.gui.screens.PauseScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen
import net.minecraft.client.gui.screens.options.OptionsScreen
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen
import java.util.function.Predicate

/**
 * Checks for Lunar client screens
 *
 * TODO: Do not simply replace any Lunar Screen with the title screen, if not in a world
 */
private val Screen.isLunar
    get() = javaClass.name.startsWith("com.moonsworth.lunar.") && mc.level == null

enum class CustomScreenType(
    val routeName: String,
    private val recognizer: Predicate<Screen> = Predicates.alwaysFalse(),
    val isInGame: Boolean = false,
    private val open: Runnable = Runnable {
        mc.setScreen(CustomSharedMinecraftScreen(byName(routeName)!!))
    }
) {

    HUD("hud", isInGame = true),
    CLICK_GUI("clickgui"),
    ALT_MANAGER("altmanager"),
    PROXY_MANAGER("proxymanager"),

    TITLE(
        "title",
        recognizer = { it is TitleScreen || it.isLunar },
        open = { mc.setScreen(TitleScreen()) }
    ),

    MULTIPLAYER(
        "multiplayer",
        recognizer = { it is JoinMultiplayerScreen || it is SafetyScreen },
        open = { mc.setScreen(JoinMultiplayerScreen(ScreenManager.parent)) }
    ),

    MULTIPLAYER_REALMS(
        "multiplayer_realms",
        recognizer = { it is RealmsMainScreen },
        open = { mc.setScreen(RealmsMainScreen(ScreenManager.parent)) }
    ),

    SINGLEPLAYER(
        "singleplayer",
        recognizer = { it is SelectWorldScreen },
        open = {
            mc.setScreen(SelectWorldScreen(ScreenManager.parent))
        }
    ),

    CREATE_WORLD(
        "create_world",
        recognizer = { it is CreateWorldScreen },
        open = {
            // Store parent before opening CreateWorldScreen, since IntegrationListener.parent
            // will change to CreateWorldScreen once it's opened
            val parentScreen = ScreenManager.parent
            CreateWorldScreen.openFresh(mc) {
                // Return to SelectWorldScreen instead of the stored parent,
                // as this is the expected navigation flow from Create World
                mc.setScreen(SelectWorldScreen(parentScreen))
            }
        }
    ),

    OPTIONS(
        "options",
        recognizer = { it is OptionsScreen },
        open = {
            mc.setScreen(OptionsScreen(ScreenManager.parent, mc.options))
        }
    ),

    GAME_MENU(
        "game_menu",
        recognizer = { it is PauseScreen }
    ),

    INVENTORY(
        "inventory",
        recognizer = { it is InventoryScreen || it is CreativeModeInventoryScreen }
    ),

    CONTAINER(
        "container",
        recognizer = { it is ContainerScreen }
    ),

    DISCONNECTED("disconnected",
        recognizer = { it is DisconnectedScreen }
    ),

    VIAFABRICPLUS_PROTOCOL_SELECTION("viafabricplus_protocol_selection",
        recognizer = { it::class.java.name == "de.florianmichael.viafabricplus.screen.base.ProtocolSelectionScreen" },
        open = ::openVfpProtocolSelection
    ),

    BROWSER("browser",
        recognizer = { it is InternetExplorerScreen }
    );

    fun open() = mc.execute(open)

    companion object {
        @JvmStatic
        fun byName(name: String) = entries.find { it.routeName == name }
        @JvmStatic
        fun recognize(screen: Screen) = entries.find { it.recognizer.test(screen) }
    }

}
