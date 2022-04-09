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
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import java.util.*

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

        private val suspectList = ArrayList<UUID>()
        val botList = ArrayList<UUID>()

        override fun disable() {
            suspectList.clear()
            botList.clear()
        }

        val packetHandler = handler<PacketEvent> {
            if (it.packet !is PlayerListS2CPacket) {
                return@handler
            }

            when (it.packet.action) {
                PlayerListS2CPacket.Action.ADD_PLAYER -> {
                    for (entry in it.packet.entries) {
                        if (entry.latency < 2 || !entry.profile.properties.isEmpty || isTheSamePlayer(entry.profile)) {
                            continue
                        }

                        if (isADuplicate(entry.profile)) {
                            botList.add(entry.profile.id)
                            continue
                        }

                        suspectList.add(entry.profile.id)
                    }
                }
                PlayerListS2CPacket.Action.REMOVE_PLAYER -> {
                    for (entry in it.packet.entries) {
                        if (suspectList.contains(entry.profile.id)) {
                            suspectList.remove(entry.profile.id)
                        }

                        if (botList.contains(entry.profile.id)) {
                            botList.remove(entry.profile.id)
                        }
                    }
                }
                else -> {}
            }
        }

        val repeatable = repeatable {
            if (suspectList.isEmpty()) {
                return@repeatable
            }

            for (entity in world.players) {
                if (!suspectList.contains(entity.uuid)) {
                    continue
                }

                if (isFullyArmored(entity) && entity.gameProfile.properties.isEmpty) {
                    botList.add(entity.uuid)
                }

                suspectList.remove(entity.uuid)
            }
        }

        private fun isADuplicate(profile: GameProfile): Boolean {
            return network.playerList.count { it.profile.name == profile.name && it.profile.id != profile.id } == 1
        }

        private fun isFullyArmored(entity: PlayerEntity): Boolean {
            return (0..3).all {
                entity.inventory.getArmorStack(it).item is ArmorItem && !entity.inventory.getArmorStack(
                    it
                ).hasEnchantments()
            }
        }

        private fun isTheSamePlayer(profile: GameProfile): Boolean {
            // Prevents false positives when a player is on a minigame such as Practice and joins a duel
            return network.playerList.count { it.profile.name == profile.name && it.profile.id == profile.id } == 1
        }
    }

    /**
     * Check if player might be a bot
     */
    fun isBot(player: PlayerEntity): Boolean {
        if (!enabled) {
            return false
        }

        if (Matrix.isActive && Matrix.botList.contains(player.uuid)) {
            return true
        }

        return false
    }

}
