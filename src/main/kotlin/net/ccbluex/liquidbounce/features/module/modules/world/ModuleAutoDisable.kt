/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.DeathEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoClip
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * AutoDisable module
 *
 * Automatically disables modules, when special event happens.
 */
object ModuleAutoDisable : Module("AutoDisable", Category.WORLD) {

    val listOfModules = arrayListOf(ModuleFly, ModuleSpeed, ModuleNoClip, ModuleKillAura, ModuleScaffold)
    private val onFlag by boolean("OnFlag", false)
    private val onDeath by boolean("OnDeath", false)
    private val onDisconnects by boolean("onDisconnect", false)

    @Suppress("unused")
    val worldChangesHandler = handler<PacketEvent> {
        if (it.packet is PlayerPositionLookS2CPacket && onFlag) {
            autoDisabled("flag")
        }
    }

    @Suppress("unused")
    val deathHandler = handler<DeathEvent> {
        if (onDeath) autoDisabled("your death")
    }

    @Suppress("unused")
    val disHandler = handler<DisconnectEvent> {
        if (onDisconnects) autoDisabled("disconnected")
    }

    private fun autoDisabled(reason: String) {
        listOfModules.filter { it.enabled }.let {
            if (it.isNotEmpty()) {
                it.forEach {
                    it.enabled = false
                }
                notification("Notifier", "Disabled modules due to $reason", NotificationEvent.Severity.INFO)
            }
        }
    }
}
