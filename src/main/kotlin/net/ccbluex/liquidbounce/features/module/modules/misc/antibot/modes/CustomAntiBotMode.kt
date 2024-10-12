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
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot.isADuplicate
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAnimationS2CPacket
import net.minecraft.network.packet.s2c.play.EntityAttributesS2CPacket
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import kotlin.math.abs

object CustomAntiBotMode : Choice("Custom"), ModuleAntiBot.IAntiBotMode {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiBot.modes

    private object InvalidGround : ToggleableConfigurable(ModuleAntiBot, "InvalidGround", true) {
        val vlToConsiderAsBot by int("VLToConsiderAsBot", 10, 1..50, "flags")
    }

    init {
        tree(InvalidGround)
        tree(AlwaysInRadius)
    }

    private val duplicate by boolean("Duplicate", false)
    private val noGameMode by boolean("NoGameMode", true)
    private val illegalPitch by boolean("IllegalPitch", true)
    private val fakeEntityID by boolean("FakeEntityID", true)
    private val illegalName by boolean("IllegalName", true)
    private val needHit by boolean("NeedHit", false)
    private val health by boolean("IllegalHealth", false)
    private val swung by boolean("Swung", false)
    private val critted by boolean("Critted", false)
    private val attributes by boolean("Attributes", false)

    private object AlwaysInRadius : ToggleableConfigurable(ModuleAntiBot, "AlwaysInRadius", false) {
        val alwaysInRadiusRange by float("AlwaysInRadiusRange", 20f, 5f..30f)
    }

    private val flyingSet = Int2IntOpenHashMap()
    private val hitListSet = IntOpenHashSet()
    private val notAlwaysInRadiusSet = IntOpenHashSet()

    private val swungSet = IntOpenHashSet()
    private val crittedSet = IntOpenHashSet()
    private val attributesSet = IntOpenHashSet()

    val repeatable = repeatable {
        val rangeSquared = AlwaysInRadius.alwaysInRadiusRange

        for (entity in world.players) {
            if (player.squaredDistanceTo(entity) > rangeSquared) {
                notAlwaysInRadiusSet.add(entity.id)
            }
        }
    }

    @Suppress("unused")
    private val attackHandler = handler<AttackEvent> {
        hitListSet.add(it.enemy.id)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is EntityS2CPacket -> {
                if (!packet.isPositionChanged || !InvalidGround.enabled) {
                    return@handler
                }

                val entity = packet.getEntity(world) ?: return@handler
                if (entity.isOnGround && entity.prevY != entity.y) {
                    flyingSet.put(entity.id, flyingSet.getOrDefault(entity.id, 0) + 1)
                } else if (!entity.isOnGround && flyingSet.getOrDefault(entity.id, 0) > 0) {
                    val newVL = flyingSet.getOrDefault(entity.id, 0) / 2

                    if (newVL <= 0) {
                        flyingSet.remove(entity.id)
                    } else {
                        flyingSet.put(entity.id, newVL)
                    }
                }
            }

            is EntityAttributesS2CPacket -> {
                attributesSet.add(packet.entityId)
            }

            is EntityAnimationS2CPacket -> {
                val animationId = packet.animationId

                if (animationId == EntityAnimationS2CPacket.SWING_MAIN_HAND ||
                    animationId == EntityAnimationS2CPacket.SWING_OFF_HAND) {
                    swungSet.add(packet.entityId)
                } else if (animationId == EntityAnimationS2CPacket.CRIT ||
                    animationId == EntityAnimationS2CPacket.ENCHANTED_HIT) {
                    crittedSet.add(packet.entityId)
                }
            }

            is EntitiesDestroyS2CPacket -> with(packet.entityIds.intIterator()) {
                // don't use forEach, it provides Integer instead of int
                while (hasNext()) {
                    val entityId = nextInt()
                    attributesSet.remove(entityId)
                    flyingSet.remove(entityId)
                    hitListSet.remove(entityId)
                    notAlwaysInRadiusSet.remove(entityId)
                }
            }
        }


    }

    private fun hasInvalidGround(player: PlayerEntity): Boolean {
        return flyingSet.getOrDefault(player.id, 0) >= InvalidGround.vlToConsiderAsBot
    }

    private fun hasIllegalName(player: PlayerEntity): Boolean {
        val validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"
        val name = player.nameForScoreboard

        return name.length !in 3..16 || name.any { it !in validChars }
    }

    @Suppress("all")
    private fun meetsCustomConditions(player: PlayerEntity): Boolean {
        val noGameMode = noGameMode && network.getPlayerListEntry(player.uuid)?.gameMode == null
        val invalidGround = InvalidGround.enabled && hasInvalidGround(player)
        val fakeId = fakeEntityID && (player.id < 0 || player.id >= 1E+9)
        val isADuplicate = duplicate && isADuplicate(player.gameProfile)
        val illegalName = illegalName && hasIllegalName(player)
        val illegalPitch = illegalPitch && abs(player.pitch) > 90
        val alwaysInRadius = AlwaysInRadius.enabled && !notAlwaysInRadiusSet.contains(player.id)
        val needHit = needHit && !hitListSet.contains(player.id)
        val health = health && player.health > 20f
        val swung = swung && !swungSet.contains(player.id)
        val critted = critted && !crittedSet.contains(player.id)
        val attributes = attributes && !attributesSet.contains(player.id)

        return noGameMode || invalidGround || fakeId || isADuplicate
            || illegalName || illegalPitch || alwaysInRadius || needHit || health
            || swung || critted || attributes
    }

    override fun isBot(entity: PlayerEntity): Boolean {
        return meetsCustomConditions(entity)
    }

    override fun reset() {
        flyingSet.clear()
        notAlwaysInRadiusSet.clear()
        hitListSet.clear()
        swungSet.clear()
        crittedSet.clear()
        attributesSet.clear()
    }
}
