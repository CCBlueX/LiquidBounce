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
package net.ccbluex.liquidbounce.features.session

import net.ccbluex.liquidbounce.event.CoroutineTicker
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.TickLoopTaskExecutor
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screens.DeathScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket

/**
 * Runs a session the window does not show: what Minecraft's tick, screen and disconnect code does for
 * the connection, without what it does to the window.
 *
 * Every function expects its session to be the loaded one.
 */
internal object BackgroundGame {

    private const val MISS_TIME_IN_SCREEN = 10000

    fun tick() {
        CoroutineTicker.beginMinecraftTick()
        TickLoopTaskExecutor.onTickLoopStart()
        CoroutineTicker.tick()
        EventManager.callEvent(GameTickEvent)

        tickGame()

        TickLoopTaskExecutor.onTickLoopCompleted()
        CoroutineTicker.endMinecraftTick()
    }

    /**
     * Follows the order of [net.minecraft.client.Minecraft.tick]. Anything in there may end the connection,
     * so the fields are read again at every step.
     */
    private fun tickGame() {
        mc.level?.tickRateManager()?.tick()

        if (mc.rightClickDelay > 0) {
            mc.rightClickDelay--
        }

        if (mc.level != null) {
            mc.gameMode?.tick()
        }

        mc.pick(1f)
        tickScreen()

        if (mc.gui.screen() != null) {
            mc.missTime = MISS_TIME_IN_SCREEN
        } else if (mc.player != null) {
            mc.handleKeybinds()
            if (mc.missTime > 0) {
                mc.missTime--
            }
        }

        val level = mc.level
        if (level == null) {
            mc.pendingConnection?.tick()
            return
        }

        level.tickEntities()
        level.tickBlockEntities()
        mc.player?.takeUnless { it.isRemoved }?.sendChanges()
        level.tick { true }
        mc.connection?.send(ServerboundClientTickEndPacket.INSTANCE)
    }

    private fun tickScreen() {
        if (mc.gui.screen() == null && mc.player?.isDeadOrDying == true) {
            setScreen(null)
        }

        mc.gui.screen()?.tick()
    }

    fun setScreen(next: Screen?) {
        var screen = next
        mc.gui.screen()?.removed()

        val player = mc.player
        val level = mc.level
        if (screen == null && player != null && level != null && player.isDeadOrDying) {
            if (player.shouldShowDeathScreen()) {
                screen = DeathScreen(null, level.levelData.isHardcore, player)
            } else {
                player.respawn()
            }
        }

        mc.gui.screen = screen
        screen?.added()
        screen?.init(mc.window.guiScaledWidth, mc.window.guiScaledHeight)
    }

    fun disconnect(screen: Screen, keepResourcePacks: Boolean) {
        mc.connection?.let { connection ->
            connection.close()
            if (!keepResourcePacks) {
                EventManager.callEvent(DisconnectEvent)
            }
        }

        mc.gameMode = null
        if (mc.level != null) {
            resetHud()
        }
        mc.level = null
        setScreen(screen)
        mc.updateLevelInEngines(null, false)
        mc.player = null
    }

    fun clearLevel(screen: Screen) {
        mc.connection?.clearLevel()
        mc.gameMode = null
        setScreen(screen)
        resetHud()
        mc.level = null
        mc.updateLevelInEngines(null, false)
        mc.player = null
    }

    /**
     * The part of updateLevelInEngines that is about the session and not about the renderer.
     */
    fun levelChanged() {
        mc.gameRenderer.mainCamera().entity = null
        mc.pendingConnection = null
    }

    private fun resetHud() {
        val hud = mc.gui.hud
        hud.tabList.reset()
        hud.bossOverlay.reset()
        hud.chat.clearMessages(true)
    }

}
