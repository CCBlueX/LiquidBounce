/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.misc

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.NotificationEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket

object ModuleAntiBot : Module("AntiBot", Category.MISC) {

    private val modes = choices("Mode", Custom, arrayOf(Custom, Matrix))

    private object Custom : Choice("Custom") {
        override val parent: ChoiceConfigurable
            get() = modes

        // This part is up to 1zuna or superblauberee, so I don't mess things up.
        // Basically a multiple antibot option dependent mode.
    }

    private object Matrix : Choice("Matrix") {
        override val parent: ChoiceConfigurable
            get() = modes

        private var pName: String? = null

        val packetHandler = handler<PacketEvent> { event ->
            if (event.packet is PlayerListS2CPacket && event.packet.action == PlayerListS2CPacket.Action.ADD_PLAYER) {
                for (entry in event.packet.entries) {
                    if (entry.latency < 2 || !entry.profile.properties.isEmpty || isTheSamePlayer(entry.profile)) {
                        continue
                    }

                    if (isADuplicate(entry.profile)) {
                        event.cancelEvent()
                        notification("AntiBot", "Removed ${entry.profile.name}", NotificationEvent.Severity.INFO)
                        continue
                    }

                    pName = entry.profile.name
                }
            }
        }

        val repeatable = repeatable {
            if (pName == null) {
                return@repeatable
            }

            for (entity in world.entities) {
                if (entity is PlayerEntity && entity.entityName == pName) {
                    if (isArmored(entity) && entity.gameProfile.properties.isEmpty) {
                        world.removeEntity(entity.id, Entity.RemovalReason.DISCARDED)
                        notification("AntiBot", "Removed $pName", NotificationEvent.Severity.INFO)
                    }
                    pName = null
                }
            }
        }

        private fun isADuplicate(profile: GameProfile): Boolean {
            return network.playerList.count { it.profile.name == profile.name && it.profile.id != profile.id } == 1
        }

        private fun isArmored(entity: PlayerEntity): Boolean {
            var count = 0
            for (slot in 0..3) {
                if (entity.inventory.getArmorStack(slot).item is ArmorItem &&
                    !entity.inventory.getArmorStack(slot).hasEnchantments()
                ) {
                    count += 1
                }
            }
            return count == 4
        }

        private fun isTheSamePlayer(profile: GameProfile): Boolean {
            // Prevents false positives when a player is on a minigame such as Practice and joins a duel
            return network.playerList.count { it.profile.name == profile.name && it.profile.id == profile.id } == 1
        }
    }
}
