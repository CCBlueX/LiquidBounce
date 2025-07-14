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
 */
package net.ccbluex.liquidbounce.features.misc

import com.mojang.blaze3d.systems.RenderSystem
import com.terraformersmc.modmenu.util.mod.Mod
import kotlinx.coroutines.cancel
import net.ccbluex.liquidbounce.api.core.scope
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.modmenu.ModMenuCompatibility
import net.fabricmc.loader.impl.FabricLoaderImpl
import net.minecraft.SharedConstants
import net.minecraft.client.util.Icons
import org.lwjgl.glfw.GLFW
import java.lang.Thread.sleep
import kotlin.concurrent.thread

private val modMenuPresent = runCatching {
    Class.forName("com.terraformersmc.modmenu.ModMenu")
    true
}.getOrDefault(false)

/**
 * Hides client appearance
 *
 * using 2x CRTL + SHIFT to hide and unhide the client
 */
object HideAppearance : EventListener {

    /**
     * These mods will be removed from ModMenu.
     * When [isHidingNow] is true
     * Or added, if [isHidingNow] is false
     *
     * Because we don't know about the [Mod] container on each mod in this list
     * We set the default value is null.
     * And we'll provide the value after first removing the mod
     */
    private val modContainersToHide: MutableMap<String, Mod?> = arrayOf(
        "liquidbounce", "mcef"
    ).associateWith { null }.toMutableMap()

    private val shiftChronometer = Chronometer()

    var isHidingNow = false
        set(value) {
            field = value
            RenderSystem.recordRenderCall(::updateClient)

            if (modMenuPresent) {
                if (value) {
                    for (id in modContainersToHide.keys) {
                        modContainersToHide[id] = ModMenuCompatibility.INSTANCE.removeModUnchecked(id)
                    }
                } else {
                    for ((id, container) in modContainersToHide) {
                        container?.let {
                            ModMenuCompatibility.INSTANCE.addModUnchecked(id, it)
                        }
                    }
                }
            }
        }

    var isDestructed = false

    private fun updateClient() {
        if (isHidingNow) {
            IntegrationListener.restoreOriginalScreen()
        } else {
            IntegrationListener.update()
        }

        mc.updateWindowTitle()
        mc.window.setIcon(
            mc.defaultResourcePack,
            if (SharedConstants.getGameVersion().isStable) Icons.RELEASE else Icons.SNAPSHOT)
    }

    @Suppress("unused")
    private val keyHandler = handler<KeyboardKeyEvent> { event ->
        val keyCode = event.keyCode
        val modifier = event.mods

        if (inGame) {
            return@handler
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT && modifier == GLFW.GLFW_MOD_CONTROL) {
            if (!shiftChronometer.hasElapsed(400L)) {
                isHidingNow = !isHidingNow
            }

            shiftChronometer.reset()
        }
    }

    /**
     * Attempt to destruct the client
     */
    fun destructClient() {
        isHidingNow = true
        isDestructed = true

        mc.inGameHud.chatHud.messageHistory.removeIf {
            it.startsWith(CommandManager.Options.prefix)
        }

        // Cancel all async tasks
        scope.cancel()

        callEvent(ClientShutdownEvent)
        EventManager.unregisterAll()

        // Disable all modules
        // Be careful to not trigger ConfigManager saving, but this should be prevented by [isDestructed]
        // and unregistering all events
        for (module in ModuleManager) {
            module.enabled = false
        }
        ModuleManager.clear()
    }

    fun wipeClient() = thread(name = "wipe-client") {
        // Wait for the client to be destructed
        sleep(1000L)

        // Clear log folder
        mc.runDirectory.resolve("logs").listFiles()?.forEach {
            runCatching {
                it.delete()
            }
        }

        // Delete LiquidBounce folder and its content
        runCatching {
            ConfigSystem.rootFolder.deleteRecursively()
        }

        FabricLoaderImpl.INSTANCE.allMods.find {
            it.metadata.id == "liquidbounce"
        }?.let { mod ->
            // Delete JAR file
            runCatching {
                val origin = mod.origin

                for (path in origin.paths) {
                    runCatching {
                        path.toFile().delete()
                    }
                }
            }

            // Remove from Fabric Loader Impl
            runCatching {
                FabricLoaderImpl.INSTANCE.modsInternal.remove(mod)
            }
        }

        // History clear
        mc.inGameHud.chatHud.clear(true)
    }

}
