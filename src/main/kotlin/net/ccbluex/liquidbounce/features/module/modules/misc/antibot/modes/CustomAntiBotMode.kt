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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot.isADuplicate
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.EntityS2CPacket
import java.util.*
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

    private val duplicate by boolean("Duplicate", true)
    private val noGameMode by boolean("NoGameMode", true)
    private val illegalPitch by boolean("IllegalPitch", true)
    private val fakeEntityID by boolean("FakeEntityID", false)
    private val illegalName by boolean("IllegalName", false)
    private val needHit by boolean("NeedHit", false)
    private val health by boolean("IllegalHealth", false)

    private object AlwaysInRadius : ToggleableConfigurable(ModuleAntiBot, "AlwaysInRadius", false) {
        val alwaysInRadiusRange by float("AlwaysInRadiusRange", 20f, 5f..30f)
    }

    private val invalidGroundList = mutableMapOf<Entity, Int>()
    private val hitList = HashSet<UUID>()
    private val notAlwaysInRadius = HashSet<UUID>()

    val repeatable = repeatable {
        for (entity in world.players) {
            if (player.distanceTo(entity) > AlwaysInRadius.alwaysInRadiusRange) {
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

    private fun hasInvalidGround(player: PlayerEntity): Boolean {
        return invalidGroundList.getOrDefault(player, 0) >= InvalidGround.vlToConsiderAsBot
    }

    private fun hasIllegalName(player: PlayerEntity): Boolean {
        val validChars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_"
        val name = player.nameForScoreboard

        if (name.length < 3 || name.length > 16) {
            return true
        }

        val result = name.indices.find { !validChars.contains(name[it]) }

        return result != null
    }

    @Suppress("all")
    private fun meetsCustomConditions(player: PlayerEntity): Boolean {
        val noGameMode = noGameMode && network.getPlayerListEntry(player.uuid)?.gameMode == null
        val invalidGround = InvalidGround.enabled && hasInvalidGround(player)
        val fakeID = fakeEntityID && (player.id < 0 || player.id >= 1E+9)
        val isADuplicate = duplicate && isADuplicate(player.gameProfile)
        val illegalName = illegalName && hasIllegalName(player)
        val illegalPitch = illegalPitch && abs(player.pitch) > 90
        val alwaysInRadius = AlwaysInRadius.enabled && !notAlwaysInRadius.contains(player.uuid)
        val needHit = needHit && !hitList.contains(player.uuid)
        val health = health && player.health > 20f

        return noGameMode || invalidGround || fakeID || isADuplicate
            || illegalName || illegalPitch || alwaysInRadius || needHit || health
    }

    override fun isBot(entity: PlayerEntity): Boolean {
        return meetsCustomConditions(entity)
    }

    override fun reset() {
        invalidGroundList.clear()
        notAlwaysInRadius.clear()
        hitList.clear()
    }
}
