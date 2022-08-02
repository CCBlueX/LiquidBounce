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
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import java.util.*

object ModuleAntiBot : Module("AntiBot", Category.MISC) {

    private val modes = choices("Mode", Custom, arrayOf(Custom, Matrix, IntaveHeavy))

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

                var armor: MutableIterable<ItemStack>? = null

                if (!isFullyArmored(entity)) {
                    armor = entity.armorItems
                    wait(1)
                }

                if ((isFullyArmored(entity) || updatesArmor(entity, armor)) && entity.gameProfile.properties.isEmpty) {
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

        fun isTheSamePlayer(profile: GameProfile): Boolean {
            // Prevents false positives when a player is on a minigame such as Practice and joins a duel
            return network.playerList.count { it.profile.name == profile.name && it.profile.id == profile.id } == 1
        }

        /**
         * Matrix spawns its bot with a random set of armor but then instantly and silently gets a new set,
         * therefore somewhat tricking the client that the bot already had the new armor.
         *
         * With the help of at least 1 tick of waiting time, this function patches this "trick".
         */
        private fun updatesArmor(entity: PlayerEntity, prevArmor: MutableIterable<ItemStack>?): Boolean {
            return prevArmor != entity.armorItems
        }
    }

    /**
     * Intave anti-cheat, Heavy bot type, their best bot type.
     *
     * Tested on: gamster.org and a private server with latest Intave as of 7/28/2022.
     */
    private object IntaveHeavy : Choice("IntaveHeavy") {
        override val parent: ChoiceConfigurable
            get() = modes

        private val suspectList = hashMapOf<UUID, Pair<Int, Long>>()
        val botList = ArrayList<UUID>()

        override fun disable() {
            suspectList.clear()
            botList.clear()
        }

        /**
         * Ping logic:
         *
         * When you join a server, you always have 0 ping at start. However, if you are on a game like Practice and come back from a duel, you will keep your ping.
         *
         * As for Matrix and Intave, they defy this logic. Intave though decides instead to fix it by sending [PlayerListS2CPacket.Action.UPDATE_LATENCY]
         * to make up for the ping issue. Unfortunately, that leads to even more problems.
         */
        val packetHandler = handler<PacketEvent> {
            if (it.packet !is PlayerListS2CPacket) {
                return@handler
            }

            when (it.packet.action) {
                PlayerListS2CPacket.Action.ADD_PLAYER -> {
                    for (entry in it.packet.entries) {
                        if (entry.latency < 2 || Matrix.isTheSamePlayer(entry.profile)) {
                            continue
                        }

                        suspectList[entry.profile.id] = Pair(entry.latency, System.currentTimeMillis())
                    }
                }
                PlayerListS2CPacket.Action.REMOVE_PLAYER -> {
                    for (entry in it.packet.entries) {
                        if (suspectList.containsKey(entry.profile.id)) {
                            suspectList.remove(entry.profile.id)
                        }

                        if (botList.contains(entry.profile.id)) {
                            botList.remove(entry.profile.id)
                        }
                    }
                }
                PlayerListS2CPacket.Action.UPDATE_LATENCY -> {
                    for (entry in it.packet.entries) {
                        if (!suspectList.containsKey(entry.profile.id)) {
                            continue
                        }

                        val pingSinceJoin = suspectList.getValue(entry.profile.id).first

                        val deltaPing = pingSinceJoin - entry.latency
                        val deltaMS = System.currentTimeMillis() - suspectList.getValue(entry.profile.id).second

                        /**
                         * Intave instantly sends this packet, but some servers might lag, so it might be delayed, that's why the difference limit is 10 MS.
                         * The less the value, the lower the chances of producing false positives, even though it's highly unlikely.
                         */
                        if (deltaPing == pingSinceJoin && deltaMS <= 10) {
                            botList.add(entry.profile.id)
                        }

                        suspectList.remove(entry.profile.id)
                    }
                }
                else -> {}
            }
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

        if (IntaveHeavy.isActive && IntaveHeavy.botList.contains(player.uuid)) {
            return true
        }

        return false
    }

}
