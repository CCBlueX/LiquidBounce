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


package net.ccbluex.liquidbounce.features.chat

import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screens.ConfirmScreen
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.resolver.ServerAddress

/**
 * Always behind a confirmation, since anyone can name any address.
 */
object ServerJoin {

    fun confirm(address: String, from: String) = mc.execute {
        val previous = mc.gui.screen()
        mc.gui.setScreen(ConfirmScreen(
            { accepted ->
                if (accepted) {
                    join(address)
                } else {
                    mc.gui.setScreen(previous)
                }
            },
            translation("liquidbounce.liquidchat.join.title"),
            translation("liquidbounce.liquidchat.join.message", from, address),
        ))
    }

    private fun join(address: String) {
        if (mc.level != null) {
            mc.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE)
        }

        ConnectScreen.startConnecting(
            JoinMultiplayerScreen(TitleScreen()),
            mc,
            ServerAddress.parseString(address),
            ServerData(address, address, ServerData.Type.OTHER),
            false,
            null,
        )
    }

}
