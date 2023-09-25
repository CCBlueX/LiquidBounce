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

package net.ccbluex.liquidbounce.features.module.modules.misc

import com.mojang.authlib.GameProfile
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.AttackEvent
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
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
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
            tree(AlwaysInRadius)
        }

        val duplicate by boolean("Duplicate", true)
        val noGameMode by boolean("NoGameMode", true)
        val illegalPitch by boolean("IllegalPitch", true)
        val fakeEntityID by boolean("FakeEntityID", false)
        val illegalName by boolean("IllegalName", false)
        val needHit by boolean("NeedHit", false)
        val health by boolean("IllegalHealth", false)

        object AlwaysInRadius : ToggleableConfigurable(ModuleAntiBot, "AlwaysInRadius", false) {
            val alwaysInRadiusRange by float("AlwaysInRadiusRange", 20f, 5f..30f)
        }

        val invalidGroundList = mutableMapOf<Entity, Int>()
        val hitList = ArrayList<UUID>()
        val notAlwaysInRadius = ArrayList<UUID>()

        val repeatable = repeatable {
            for (entity in world.players) {
                if (player.distanceTo(entity) > AlwaysInRadius.alwaysInRadiusRange && !notAlwaysInRadius.contains(entity.uuid)) {
                    notAlwaysInRadius.add(entity.uuid)
                }
            }
        }

        val attackHandler = handler<AttackEvent> {
            hitList.add(it.enemy.uuid)
        }

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
            val packet = it.packet

            if (packet is PlayerListS2CPacket) {
                for (entry in packet.playerAdditionEntries) {
                    if (entry.latency < 2 || !entry.profile.properties.isEmpty || isTheSamePlayer(entry.profile)) {
                        continue
                    }

                    if (isADuplicate(entry.profile)) {
                        botList.add(entry.profileId)
                        continue
                    }

                    suspectList.add(entry.profileId)
                }
            } else if (packet is PlayerRemoveS2CPacket) {
                for (uuid in packet.profileIds) {
                    if (suspectList.contains(uuid)) {
                        suspectList.remove(uuid)
                    }

                    if (botList.contains(uuid)) {
                        botList.remove(uuid)
                    }
                }
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
            when (val packet = it.packet) {
                is PlayerListS2CPacket -> {
                    when (packet.actions.first()) {
                        PlayerListS2CPacket.Action.ADD_PLAYER -> {
                            for (entry in packet.entries) {
                                if (entry.latency < 2 || Matrix.isTheSamePlayer(entry.profile)) {
                                    continue
                                }

                                suspectList[entry.profileId] = Pair(entry.latency, System.currentTimeMillis())
                            }
                        }

                        PlayerListS2CPacket.Action.UPDATE_LATENCY -> {
                            /**
                             * On 7/28/2022 and earlier, Intave sent this packet and instead of updating every player's ping, they update only their bot's ping.
                             * Another flaw they had back then, don't know if they fixed it now, so I'm having this as a comment instead. Minimizes false positives by a large amount.
                             */

                            /*if (packet.entries.size > 1) {
                                return@handler
                            }*/

                            for (entry in packet.entries) {
                                if (!suspectList.containsKey(entry.profileId)) {
                                    continue
                                }

                                val pingSinceJoin = suspectList.getValue(entry.profileId).first

                                val deltaPing = pingSinceJoin - entry.latency
                                val deltaMS = System.currentTimeMillis() - suspectList.getValue(entry.profileId).second

                                /**
                                 * Intave instantly sends this packet, but some servers might lag, so it might be delayed, that's why the difference limit is 15 MS.
                                 * The less the value, the lower the chances of producing false positives, even though it's highly unlikely.
                                 */
                                if (deltaPing == pingSinceJoin && deltaMS <= 15) {
                                    botList.add(entry.profileId)
                                }

                                suspectList.remove(entry.profileId)
                            }
                        }

                        else -> {}
                    }
                }

                is PlayerRemoveS2CPacket -> {
                    for (id in packet.profileIds) {
                        if (suspectList.containsKey(id)) {
                            suspectList.remove(id)
                        }

                        if (botList.contains(id)) {
                            botList.remove(id)
                        }
                    }
                }
            }
        }

    }

    private object Horizon : Choice("Horizon") {
        override val parent: ChoiceConfigurable
            get() = modes

        val botList = ArrayList<UUID>()

        val packetHandler = handler<PacketEvent> {
            when (val packet = it.packet) {
                is PlayerListS2CPacket -> {
                    if (packet.actions.first() == PlayerListS2CPacket.Action.ADD_PLAYER) {
                        for (entry in packet.entries) {
                            if (entry.gameMode != null) {
                                continue
                            }

                            botList.add(entry.profileId)
                        }
                    }
                }

                is PlayerRemoveS2CPacket -> {
                    for (id in packet.profileIds) {
                        if (botList.contains(id)) {
                            botList.remove(id)
                        }
                    }
                }
            }
        }
    }

    private fun isADuplicate(profile: GameProfile): Boolean {
        return network.playerList.count { it.profile.name == profile.name && it.profile.id != profile.id } == 1
    }

    override fun disable() {
        Custom.invalidGroundList.clear()
        Custom.notAlwaysInRadius.clear()
        Custom.hitList.clear()
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
                val alwaysInRadius = Custom.AlwaysInRadius.enabled && !Custom.notAlwaysInRadius.contains(player.uuid)
                val needHit = Custom.needHit && !Custom.hitList.contains(player.uuid)
                val health = Custom.health && player.health > 20f

                return noGameMode || invalidGround || fakeID || isADuplicate || illegalName || illegalPitch || alwaysInRadius || needHit || health
            }
        }
        return false
    }
}
