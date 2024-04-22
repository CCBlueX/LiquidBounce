/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.server.S08PacketPlayerPosLook

object AutoDisable : Module("AutoDisable", ModuleCategory.MISC, gameDetecting = false, hideModule = false) {
    val modulesList = arrayListOf(KillAura, Scaffold, Fly, Speed)

    private val onFlagged by BoolValue("onFlag", true)
    private val onWorldChange by BoolValue("onWorldChange", false)
    private val onDeath by BoolValue("onDeath", false)

    private val warn by ListValue("Warn", arrayOf("Chat", "Notification"), "Chat")

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is S08PacketPlayerPosLook && onFlagged) {
            disabled("flagged")
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        if (onDeath && player.isDead) {
            disabled("deaths")
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (onWorldChange) {
            disabled("world changed")
        }
    }

    private fun disabled(reason: String) {
        val anyModuleEnabled = modulesList.any { it.state }

        if (anyModuleEnabled) {
            modulesList.forEach { module ->
                if (module.state) {
                    module.state = false
                    module.onDisable()
                }
            }

            if (warn == "Chat") {
                Chat.print("§eModules have been disabled due to §c$reason")
            } else {
                hud.addNotification(Notification("Modules have been disabled due to $reason", 2000F))
            }
        }
    }

    fun addModule(module: Module) {
        if (!modulesList.contains(module)) {
            modulesList.add(module)
        }
    }

    fun removeModule(module: Module) {
        modulesList.remove(module)
    }

    fun getModules(): List<Module> {
        return modulesList.toList()
    }
}