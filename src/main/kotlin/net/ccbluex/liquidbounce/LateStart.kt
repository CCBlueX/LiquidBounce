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
package net.ccbluex.liquidbounce

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.ClientStartEvent
import net.ccbluex.liquidbounce.event.events.SessionEvent
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleHud
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager
import net.ccbluex.liquidbounce.integration.interop.ClientInteropServer
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.modloader.api.LoadedMod
import net.ccbluex.modloader.api.RuntimeMod
import net.minecraft.server.packs.resources.ReloadableResourceManager

/**
 * Starts the client when ModLoader loads it into a running game, whose [net.minecraft.client.Minecraft]
 * constructor already ran past the mixins that fire [SessionEvent] and [ClientStartEvent].
 */
class LateStart : RuntimeMod {

    override fun load(mod: LoadedMod) {
        // Registers the start handlers; at startup the window title mixin gets there first
        LiquidBounce
        // MixinOptions turns an automatic GUI scale into 2 on the first read, at startup while the window opens.
        // Left to the browser's first request, the change comes from a worker thread after the HUD read the old scale.
        mc.options.guiScale()
        EventManager.callEvent(SessionEvent(mc.user))
        EventManager.callEvent(ClientStartEvent)
        // The game already runs the mixins, so the managers and modules have to exist when this returns: the
        // immediate dispatcher runs the initializer here up to its first wait, the rest follows on the render thread
        LiquidBounce.initializeClient(Dispatchers.Default, Dispatchers.Minecraft.immediate)
        // For the reload listeners registered on start; the client's own joins the initialization above.
        // A world already open gets its HUD once that is done, as no screen change follows to open it.
        mc.reloadResourcePacks().thenRunAsync({
            if (BrowserBackendManager.isInitialized) {
                ModuleHud.reopen()
            }
        }, mc)
    }

    override fun unload() {
        // Shutdown stops the server in the background, which would outlive the class loader ModLoader closes next
        runBlocking { ClientInteropServer.stop() }
        EventManager.callEvent(ClientShutdownEvent)
        (mc.resourceManager as? ReloadableResourceManager)?.listeners?.removeIf {
            it.javaClass.classLoader == LateStart::class.java.classLoader
        }
    }

}
