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

import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.MixinKeyMappingAccessor
import net.ccbluex.liquidbounce.utils.client.NullableBypass
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.BossHealthOverlay
import net.minecraft.client.gui.components.ChatComponent
import net.minecraft.client.gui.components.PlayerTabOverlay
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.multiplayer.MultiPlayerGameMode
import net.minecraft.client.player.LocalPlayer
import net.minecraft.client.server.IntegratedServer
import net.minecraft.network.Connection
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.HitResult

/**
 * Everything Minecraft keeps once per client although it belongs to one connection.
 * Holds it for a session while another session is loaded.
 */
internal class GameState {

    @Volatile
    var player: LocalPlayer? = null
        private set

    @Volatile
    var level: ClientLevel? = null
        private set

    @Volatile
    var gameMode: MultiPlayerGameMode? = null
        private set

    @Volatile
    var screen: Screen? = null
        private set

    private var cameraEntity: Entity? = null
    private var hitResult: HitResult? = null
    private var crosshairPickEntity: Entity? = null
    private var pendingConnection: Connection? = null
    private var singleplayerServer: IntegratedServer? = null
    private var isLocalServer = false
    private var rightClickDelay = 0
    private var missTime = 0
    private var serverRenderDistance = 0

    private var chat: ChatComponent? = null
    private var tabList: PlayerTabOverlay? = null
    private var bossOverlay: BossHealthOverlay? = null

    private val keysDown = ReferenceOpenHashSet<KeyMapping>()
    private val keyClicks = Reference2IntOpenHashMap<KeyMapping>()

    fun capture() {
        val mc = NullableBypass.mc() ?: return

        player = mc.player
        level = mc.level
        gameMode = mc.gameMode
        screen = mc.gui.screen
        cameraEntity = mc.gameRenderer.mainCamera().entity
        hitResult = mc.hitResult
        crosshairPickEntity = mc.crosshairPickEntity
        pendingConnection = mc.pendingConnection
        singleplayerServer = mc.singleplayerServer
        isLocalServer = mc.isLocalServer
        rightClickDelay = mc.rightClickDelay
        missTime = mc.missTime
        serverRenderDistance = mc.options.serverRenderDistance

        val hud = mc.gui.hud
        chat = hud.chat
        tabList = hud.tabList
        bossOverlay = hud.bossOverlay

        keysDown.clear()
        keyClicks.clear()
        for (key in KeyMapping.ALL.values) {
            if ((key as MixinKeyMappingAccessor).isHeld) {
                keysDown.add(key)
            }
            if (key.clickCount != 0) {
                keyClicks.put(key, key.clickCount)
            }
        }
    }

    fun install() {
        val mc = NullableBypass.mc() ?: return

        mc.player = player
        mc.level = level
        mc.gameMode = gameMode
        mc.gui.screen = screen
        mc.gameRenderer.mainCamera().entity = cameraEntity
        mc.hitResult = hitResult
        mc.crosshairPickEntity = crosshairPickEntity
        mc.pendingConnection = pendingConnection
        mc.singleplayerServer = singleplayerServer
        mc.isLocalServer = isLocalServer
        mc.rightClickDelay = rightClickDelay
        mc.missTime = missTime
        mc.options.serverRenderDistance = serverRenderDistance

        val hud = mc.gui.hud
        hud.chat = chat ?: ChatComponent(mc)
        hud.tabList = tabList ?: PlayerTabOverlay(mc, hud)
        hud.bossOverlay = bossOverlay ?: BossHealthOverlay(mc)

        for (key in KeyMapping.ALL.values) {
            (key as MixinKeyMappingAccessor).isHeld = key in keysDown
            key.clickCount = keyClicks.getInt(key)
        }
    }

}
