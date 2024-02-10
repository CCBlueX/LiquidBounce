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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.fakelag.FakeLag
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.*
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.math.component1
import net.ccbluex.liquidbounce.utils.math.component2
import net.ccbluex.liquidbounce.utils.math.component3
import net.minecraft.item.MilkBucketItem
import net.minecraft.item.PotionItem
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.s2c.play.*
import net.minecraft.util.math.Vec3d

/**
 * FakeLag module
 *
 * Holds back packets to prevent you from being hit by an enemy.
 */
@Suppress("detekt:all")
object ModuleFakeLag : Module("FakeLag", Category.COMBAT) {

    private val range by floatRange("Range", 2f..5f, 0f..10f)
    private val delay by int("Delay", 550, 0..1000, "ms")

    private val evadeArrows by boolean("EvadeArrows", true)

    fun shouldLag(packet: Packet<*>?): Boolean {
        if (!enabled || !inGame || player.isDead || player.isTouchingWater || mc.currentScreen != null) {
            return false
        }

        if (FakeLag.isAboveTime(delay.toLong())) {
            return false
        }

        when (packet) {
            is PlayerPositionLookS2CPacket, is PlayerInteractBlockC2SPacket,
            is PlayerActionC2SPacket, is UpdateSignC2SPacket, is PlayerInteractEntityC2SPacket,
            is ResourcePackStatusC2SPacket -> {
                return false
            }

            // Flush on knockback
            is EntityVelocityUpdateS2CPacket -> {
                if (packet.id == player.id && (packet.velocityX != 0 || packet.velocityY != 0 || packet.velocityZ != 0)) {
                    return false
                }
            }

            // Flush on explosion
            is ExplosionS2CPacket -> {
                if (packet.playerVelocityX != 0f || packet.playerVelocityY != 0f || packet.playerVelocityZ != 0f) {
                    return false
                }
            }

            // Flush on damage
            is HealthUpdateS2CPacket -> {
                return false
            }
        }

        // We don't want to lag when we are using an item that is not a food, milk bucket or potion.
        if (player.isUsingItem && (player.activeItem.isFood || player.activeItem.item is MilkBucketItem
                || player.activeItem.item is PotionItem)) {
            return false
        }

        // Support auto shoot with fake lag
        if (ModuleAutoShoot.enabled && ModuleAutoShoot.targetTracker.lockedOnTarget == null) {
            return true
        }

        // If there is an enemy in range, we want to lag.
        world.findEnemy(range) ?: return false

        val playerPosition = FakeLag.firstPosition() ?: return true
        val playerBox = player.dimensions.getBoxAt(playerPosition)

        // todo: implement if enemy is facing old player position

        return !world.getEntitiesBoxInRange(playerPosition, range.endInclusive.toDouble()) { it.shouldBeAttacked() }
            .any {
                it != player && it.box.intersects(playerBox)
            }
    }

    val repeatable = repeatable {
        if (evadeArrows) {
            val (x, y, z) = FakeLag.firstPosition() ?: return@repeatable

            if (FakeLag.getInflictedHit(Vec3d(x, y, z)) == null) {
                return@repeatable
            }

            val evadingPacket = FakeLag.findAvoidingArrowPosition()

            // We have found no packet that avoids getting hit? Then we default to blinking.
            // AutoDoge might save the situation...
            if (evadingPacket == null) {
                notification("FakeLag", "Unable to evade arrow. Blinking.",
                    NotificationEvent.Severity.INFO)
                enabled = false
            } else if (evadingPacket.ticksToImpact != null) {
                notification("FakeLag", "Trying to evade arrow...", NotificationEvent.Severity.INFO)
                FakeLag.flush(evadingPacket.idx + 1)
            } else {
                notification("FakeLag", "Arrow evaded.", NotificationEvent.Severity.INFO)
                FakeLag.flush(evadingPacket.idx + 1)
            }
        }
    }

}
