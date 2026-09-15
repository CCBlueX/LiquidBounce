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
package net.ccbluex.liquidbounce.addon

import net.ccbluex.liquidbounce.utils.client.inGame
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.ClientPacketListener
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer

/**
 * The running game. Everything but [minecraft] and [isInGame] throws outside a world, so check first
 * or only call from handlers of modules, which never run outside one.
 */
object Game {

    @JvmStatic
    val minecraft: Minecraft
        get() = Minecraft.getInstance()

    @JvmStatic
    val isInGame: Boolean
        get() = inGame

    @JvmStatic
    val player: LocalPlayer
        get() = requireNotNull(minecraft.player) { "not in a world" }

    @JvmStatic
    val world: ClientLevel
        get() = requireNotNull(minecraft.level) { "not in a world" }

    @JvmStatic
    val connection: ClientPacketListener
        get() = requireNotNull(minecraft.connection) { "not connected" }

    @JvmStatic
    val interaction: MultiPlayerGameMode
        get() = requireNotNull(minecraft.gameMode) { "not in a world" }

    /**
     * Runs [task] on the render thread, now if this is it.
     */
    @JvmStatic
    fun onClientThread(task: Runnable) {
        minecraft.execute(task)
    }

}
