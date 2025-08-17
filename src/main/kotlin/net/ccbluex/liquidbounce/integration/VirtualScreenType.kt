/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.integration

import com.mojang.blaze3d.systems.RenderCall
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.openVfpProtocolSelection
import net.minecraft.client.gui.screen.DisconnectedScreen
import net.minecraft.client.gui.screen.GameMenuScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen
import net.minecraft.client.gui.screen.option.OptionsScreen
import net.minecraft.client.gui.screen.world.CreateWorldScreen
import net.minecraft.client.gui.screen.world.SelectWorldScreen
import net.minecraft.client.realms.gui.screen.RealmsMainScreen
import java.util.function.Predicate

/**
 * Checks for Lunar client screens
 *
 * TODO: Do not simply replace any Lunar Screen with the title screen, if not in a world
 */
private val Screen.isLunar
    get() = javaClass.name.startsWith("com.moonsworth.lunar.") && mc.world == null

enum class VirtualScreenType(
    val routeName: String,
    private val recognizer: Predicate<Screen> = Predicate { false },
    val isInGame: Boolean = false,
    private val open: RenderCall = RenderCall {
        mc.setScreen(VirtualDisplayScreen(byName(routeName)!!))
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
        recognizer = { it is MultiplayerScreen || it is MultiplayerWarningScreen },
        open = { mc.setScreen(MultiplayerScreen(IntegrationListener.parent)) }
    ),

    MULTIPLAYER_REALMS(
        "multiplayer_realms",
        recognizer = { it is RealmsMainScreen },
        open = { mc.setScreen(RealmsMainScreen(IntegrationListener.parent)) }
    ),

    SINGLEPLAYER(
        "singleplayer",
        recognizer = { it is SelectWorldScreen },
        open = {
            mc.setScreen(SelectWorldScreen(IntegrationListener.parent))
        }
    ),

    CREATE_WORLD(
        "create_world",
        recognizer = { it is CreateWorldScreen },
        open = { CreateWorldScreen.show(mc, IntegrationListener.parent) }
    ),

    OPTIONS(
        "options",
        recognizer = { it is OptionsScreen },
        open = {
            mc.setScreen(OptionsScreen(IntegrationListener.parent, mc.options))
        }
    ),

    GAME_MENU(
        "game_menu",
        recognizer = { it is GameMenuScreen }
    ),

    INVENTORY(
        "inventory",
        recognizer = { it is InventoryScreen || it is CreativeInventoryScreen }
    ),

    CONTAINER(
        "container",
        recognizer = { it is GenericContainerScreen }
    ),

    DISCONNECTED("disconnected",
        recognizer = { it is DisconnectedScreen }
    ),

    VIAFABRICPLUS_PROTOCOL_SELECTION("viafabricplus_protocol_selection",
        recognizer = { it::class.java.name == "de.florianmichael.viafabricplus.screen.base.ProtocolSelectionScreen" },
        open = { openVfpProtocolSelection() }
    ),

    BROWSER("browser",
        recognizer = { it is BrowserScreen }
    );

    fun open() = RenderSystem.recordRenderCall(open)

    companion object {
        @JvmStatic
        fun byName(name: String) = entries.find { it.routeName == name }
        @JvmStatic
        fun recognize(screen: Screen) = entries.find { it.recognizer.test(screen) }
    }

}
