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
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import java.util.*
import kotlin.math.abs

object ModuleAntiBot : Module("AntiBot", Category.MISC) {

    private val modes = choices("Mode", Custom, arrayOf(Custom, Matrix, IntaveHeavy, Horizon))
    private val literalNPC by boolean("LiteralNPC", false)

    private object Custom : Choice("Custom") {
        override val parent: ChoiceConfigurable
            get() = modes

        object InvalidGround : ToggleableConfigurable(ModuleAntiBot, "InvalidGround", true) {
            val vlToConsiderAsBot by int("VLToConsiderAsBot", 10, 1..50)
        }

        init {
            tree(InvalidGround)
        }

        val duplicate by boolean("Duplicate", true)
        val noGameMode by boolean("NoGameMode", true)
        val illegalPitch by boolean("IllegalPitch", true)
        val fakeEntityID by boolean("FakeEntityID", false)
        val illegalName by boolean("IllegalName", false)

        val invalidGroundList = mutableMapOf<Entity, Int>()

        val packetHandler = handler<PacketEvent> {
            if (it.packet !is EntityS2CPacket || !it.packet.isPositionChanged || !InvalidGround.enabled) {
                return@handler
            }

            val entity = it.packet.getEntity(world) ?: return@handler

            if (entity.isOnGround && entity.prevY != entity.y) {
                invalidGroundList[entity] = invalidGroundList.getOrDefault(entity, 0) + 1
            } else if (!entity.isOnGround && invalidGroundList.getOrDefault(entity, 0) > 0) {
                val newVL = invalidGroundList.getOrDefault(entity, 0) / 2

                if (newVL <= 0) {
                    invalidGroundList.remove(entity)
                } else {
                    invalidGroundList[entity] = newVL
                }
            }
        }

        fun hasInvalidGround(player: PlayerEntity): Boolean {
            return invalidGroundList.getOrDefault(player, 0) >= InvalidGround.vlToConsiderAsBot
        }

        fun hasIllegalName(player: PlayerEntity): Boolean {
            val validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"
            val name = player.entityName

            if (name.length < 3 || name.length > 16) {
                return true
            }

            val result = name.indices.find { !validChars.contains(name[it]) }

            return result != null
        }
    }

    private object Matrix : Choice("Matrix") {
        override val parent: ChoiceConfigurable
            get() = modes

        val suspectList = ArrayList<UUID>()
        val botList = ArrayList<UUID>()

        val packetHandler = handler<PacketEvent> {
            if (it.packet !is PlayerListS2CPacket) {
                return@handler
            }
//
//            for (action in it.packet.actions) {
//                when (action) {
//                    PlayerListS2CPacket.Action.ADD_PLAYER -> {
//                        for (entry in it.packet.entries) {
//                            if (entry.latency < 2 || !entry.profile.properties.isEmpty || isTheSamePlayer(entry.profile)) {
//                                continue
//                            }
//
//                            if (isADuplicate(entry.profile)) {
//                                botList.add(entry.profile.id)
//                                continue
//                            }
//
//                            suspectList.add(entry.profile.id)
//                        }
//                    }
//
//                    PlayerListS2CPacket.Action.REMOVE_PLAYER -> {
//                        for (entry in it.packet.entries) {
//                            if (suspectList.contains(entry.profile.id)) {
//                                suspectList.remove(entry.profile.id)
//                            }
//
//                            if (botList.contains(entry.profile.id)) {
//                                botList.remove(entry.profile.id)
//                            }
//                        }
//                    }
//
//                    else -> {}
//                }
//            }

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

        private fun isFullyArmored(entity: PlayerEntity): Boolean {
            return (0..3).all {
                val stack = entity.inventory.getArmorStack(it)
                stack.item is ArmorItem && stack.hasEnchantments()
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

        val suspectList = hashMapOf<UUID, Pair<Int, Long>>()
        val botList = ArrayList<UUID>()

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

//            when (it.packet.action) {
//                PlayerListS2CPacket.Action.ADD_PLAYER -> {
//                    for (entry in it.packet.entries) {
//                        if (entry.latency < 2 || Matrix.isTheSamePlayer(entry.profile)) {
//                            continue
//                        }
//
//                        suspectList[entry.profile.id] = Pair(entry.latency, System.currentTimeMillis())
//                    }
//                }
//
//                PlayerListS2CPacket.Action.REMOVE_PLAYER -> {
//                    for (entry in it.packet.entries) {
//                        if (suspectList.containsKey(entry.profile.id)) {
//                            suspectList.remove(entry.profile.id)
//                        }
//
//                        if (botList.contains(entry.profile.id)) {
//                            botList.remove(entry.profile.id)
//                        }
//                    }
//                }
//
//                PlayerListS2CPacket.Action.UPDATE_LATENCY -> {
//                    for (entry in it.packet.entries) {
//                        if (!suspectList.containsKey(entry.profile.id)) {
//                            continue
//                        }
//
//                        val pingSinceJoin = suspectList.getValue(entry.profile.id).first
//
//                        val deltaPing = pingSinceJoin - entry.latency
//                        val deltaMS = System.currentTimeMillis() - suspectList.getValue(entry.profile.id).second
//
//                        /**
//                         * Intave instantly sends this packet, but some servers might lag, so it might be delayed, that's why the difference limit is 15 MS.
//                         * The less the value, the lower the chances of producing false positives, even though it's highly unlikely.
//                         */
//                        if (deltaPing == pingSinceJoin && deltaMS <= 15) {
//                            botList.add(entry.profile.id)
//                        }
//
//                        suspectList.remove(entry.profile.id)
//                    }
//                }
//
//                else -> {}
//            }
        }

    }

    private object Horizon : Choice("Horizon") {
        override val parent: ChoiceConfigurable
            get() = modes

        val botList = ArrayList<UUID>()

        val packetHandler = handler<PacketEvent> {
            if (it.packet !is PlayerListS2CPacket) {
                return@handler
            }

//            when (it.packet.action) {
//                PlayerListS2CPacket.Action.ADD_PLAYER -> {
//                    for (entry in it.packet.entries) {
//                        // There are no accidents, no legit player joins with no gamemode.
//                        if (entry.gameMode != null) {
//                            continue
//                        }
//
//                        botList.add(entry.profile.id)
//                    }
//                }
//
//                PlayerListS2CPacket.Action.REMOVE_PLAYER -> {
//                    for (entry in it.packet.entries) {
//                        if (botList.contains(entry.profile.id)) {
//                            botList.remove(entry.profile.id)
//                        }
//                    }
//                }
//
//                else -> {}
//            }
        }
    }

    private fun isADuplicate(profile: GameProfile): Boolean {
        return network.playerList.count { it.profile.name == profile.name && it.profile.id != profile.id } == 1
    }

    override fun disable() {
        Custom.invalidGroundList.clear()
        Matrix.suspectList.clear()
        Matrix.botList.clear()
        IntaveHeavy.suspectList.clear()
        IntaveHeavy.botList.clear()
        Horizon.botList.clear()
    }

    /**
     * Check if player might be a bot
     */
    fun isBot(player: PlayerEntity): Boolean {
        if (!enabled) {
            return false
        }

        if (literalNPC && !network.playerUuids.contains(player.uuid)) {
            return true
        }

        when (modes.activeChoice) {
            is Matrix -> return Matrix.botList.contains(player.uuid)
            is IntaveHeavy -> return IntaveHeavy.botList.contains(player.uuid)
            is Horizon -> return Horizon.botList.contains(player.uuid)
            is Custom -> {
                val noGameMode = Custom.noGameMode && network.getPlayerListEntry(player.uuid)?.gameMode == null
                val invalidGround = Custom.InvalidGround.enabled && Custom.hasInvalidGround(player)
                val fakeID = Custom.fakeEntityID && (player.id < 0 || player.id >= 1E+9)
                val isADuplicate = Custom.duplicate && isADuplicate(player.gameProfile)
                val illegalName = Custom.illegalName && Custom.hasIllegalName(player)
                val illegalPitch = Custom.illegalPitch && abs(player.pitch) > 90

                return noGameMode || invalidGround || fakeID || isADuplicate || illegalName || illegalPitch
            }
        }

        return false
    }

}
