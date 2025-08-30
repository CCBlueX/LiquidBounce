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
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot.isADuplicate
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot.isGameProfileUnique
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
import java.util.*

object MatrixAntiBotMode : Choice("Matrix"), ModuleAntiBot.IAntiBotMode {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiBot.modes

    private val suspectList = hashSetOf<UUID>()
    private val botList = hashSetOf<UUID>()

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is PlayerListS2CPacket) {
            for (entry in packet.playerAdditionEntries) {
                val profile = entry.profile ?: continue

                if (entry.latency < 2 || profile.properties?.isEmpty == false || isGameProfileUnique(profile)) {
                    continue
                }

                if (isADuplicate(profile)) {
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

    val repeatable = tickHandler {
        if (suspectList.isEmpty()) {
            return@tickHandler
        }

        for (entity in world.players) {
            if (!suspectList.contains(entity.uuid)) {
                continue
            }

            var armor: MutableIterable<ItemStack>? = null

            if (!isFullyArmored(entity)) {
                armor = entity.armorItems
                waitTicks(1)
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

    /**
     * Matrix spawns its bot with a random set of armor but then instantly and silently gets a new set,
     * therefore somewhat tricking the client that the bot already had the new armor.
     *
     * With the help of at least 1 tick of waiting time, this function patches this "trick".
     */
    private fun updatesArmor(entity: PlayerEntity, prevArmor: MutableIterable<ItemStack>?): Boolean {
        return prevArmor != entity.armorItems
    }

    override fun isBot(entity: PlayerEntity): Boolean {
        return botList.contains(entity.uuid)
    }

    override fun reset() {
        suspectList.clear()
        botList.clear()
    }
}
