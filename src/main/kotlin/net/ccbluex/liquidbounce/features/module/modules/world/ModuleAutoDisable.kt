/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.DeathEvent
import net.ccbluex.liquidbounce.event.NotificationEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleNoClip
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSpeed
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * AutoDisable module
 *
 * Automatically disables modules, when special event happens.
 */
object ModuleAutoDisable : Module("AutoDisable", Category.WORLD) {
    val listOfModules = arrayListOf<Module>(ModuleFly, ModuleSpeed, ModuleNoClip, ModuleKillAura)
    private val onFlag by boolean("OnFlag", false)
    private val onDeath by boolean("OnDeath", false)

    val worldChangesHandler = handler<PacketEvent> {
        if (it.packet is PlayerPositionLookS2CPacket && onFlag) {
            autoDisabled("flag")
        }
    }

    val deathHandler = handler<DeathEvent> {
        if (onDeath) autoDisabled("your death")
    }

    fun autoDisabled(reason: String) {
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
