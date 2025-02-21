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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.positions
import net.ccbluex.liquidbounce.utils.combat.*
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket
import net.minecraft.network.packet.c2s.play.*
import net.minecraft.network.packet.s2c.play.*
import kotlin.jvm.optionals.getOrNull

/**
 * FakeLag module
 *
 * Holds back packets to prevent you from being hit by an enemy.
 */
@Suppress("detekt:all")
object ModuleFakeLag : ClientModule("FakeLag", Category.COMBAT) {

    private val range by floatRange("Range", 2f..5f, 0f..10f)
    private val delay by intRange("Delay", 300..600, 0..1000, "ms")
    private val recoilTime by int("RecoilTime", 250, 0..1000, "ms")
    private val mode by enumChoice("Mode", Mode.DYNAMIC).apply { tagBy(this) }

    private val flushOnEntityInteract by boolean("FlushOnEntityInteract", true)
    private val flushOnBlockInteract by boolean("FlushOnBlockInteract", true)
    private val flushOnAction by boolean("FlushOnAction", true)

    private enum class Mode(override val choiceName: String) : NamedChoice {
        CONSTANT("Constant"),
        DYNAMIC("Dynamic")
    }

    private var nextDelay = delay.random()
    private val chronometer = Chronometer()

    private var isEnemyNearby = false

    @Suppress("unused")
    private val gameTickHandler = tickHandler {
        isEnemyNearby = world.findEnemy(range) != null

        if (ModuleAutoDodge.running) {
            val position = positions.firstOrNull() ?: return@tickHandler

            if (ModuleAutoDodge.getInflictedHit(position) == null) {
                return@tickHandler
            }

            val evadingPacket = ModuleAutoDodge.findAvoidingArrowPosition()

            // We have found no packet that avoids getting hit? Then we default to blinking.
            // AutoDoge might save the situation...
            if (evadingPacket == null) {
                notification(
                    "FakeLag", "Unable to evade arrow. Blinking.",
                    NotificationEvent.Severity.INFO
                )
                PacketQueueManager.flush { snapshot -> snapshot.origin == TransferOrigin.SEND }
            } else if (evadingPacket.ticksToImpact != null) {
                notification("FakeLag", "Trying to evade arrow...", NotificationEvent.Severity.INFO)
                PacketQueueManager.flush(evadingPacket.idx + 1)
            } else {
                notification("FakeLag", "Arrow evaded.", NotificationEvent.Severity.INFO)
                PacketQueueManager.flush(evadingPacket.idx + 1)
            }
        }
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (event.origin != TransferOrigin.SEND || player.isDead || player.isTouchingWater
            || mc.currentScreen != null
        ) {
            return@handler
        }

        if (!chronometer.hasAtLeastElapsed(recoilTime.toLong())) {
            return@handler
        }

        if (PacketQueueManager.isAboveTime(nextDelay.toLong())) {
            nextDelay = delay.random()
            return@handler
        }

        when (val packet = event.packet) {

            is PlayerPositionLookS2CPacket,
            is ResourcePackStatusC2SPacket -> {
                chronometer.reset()
                return@handler
            }

            is PlayerInteractEntityC2SPacket,
            is HandSwingC2SPacket -> {
                if (flushOnEntityInteract) {
                    chronometer.reset()
                    return@handler
                }
            }

            is PlayerInteractBlockC2SPacket,
            is UpdateSignC2SPacket -> {
                if (flushOnBlockInteract) {
                    chronometer.reset()
                    return@handler
                }
            }

            is PlayerActionC2SPacket -> {
                if (flushOnAction) {
                    chronometer.reset()
                    return@handler
                }
            }

            // Flush on knockback
            is EntityVelocityUpdateS2CPacket -> {
                if (packet.entityId == player.id && (packet.velocityX != 0 || packet.velocityY != 0 || packet.velocityZ != 0)) {
                    chronometer.reset()
                    return@handler
                }
            }

            // Flush on explosion
            is ExplosionS2CPacket -> {
                packet.playerKnockback.getOrNull()?.let { knockback ->
                    if (knockback.x != 0.0 || knockback.y != 0.0 || knockback.z != 0.0) {
                        chronometer.reset()
                        return@handler
                    }
                }
            }

            // Flush on damage
            is HealthUpdateS2CPacket -> {
                chronometer.reset()
                return@handler
            }
        }

        // We don't want to lag when we are using an item that is not a food, milk bucket or potion.
        if (player.isUsingItem && player.activeItem.isConsumable) {
            return@handler
        }

        // Support auto shoot with fake lag
        if (running && ModuleAutoShoot.constantLag && ModuleAutoShoot.targetTracker.target == null) {
            event.action = PacketQueueManager.Action.QUEUE
            return@handler
        }

        event.action = when (mode) {
            Mode.CONSTANT -> PacketQueueManager.Action.QUEUE
            Mode.DYNAMIC -> {
                // If there is an enemy in range, we want to lag.
                if (!isEnemyNearby) {
                    return@handler
                }

                val position = positions.firstOrNull() ?: run {
                    event.action = PacketQueueManager.Action.QUEUE
                    return@handler
                }
                val playerBox = player.dimensions.getBoxAt(position)

                // todo: implement if enemy is facing old player position

                val entities = world.getEntitiesBoxInRange(position, range.endInclusive.toDouble()) {
                    it != player && it.shouldBeAttacked()
                }

                // If there are no entities, we don't want to lag.
                if (entities.isEmpty()) {
                    return@handler
                }

                val intersects = entities.any {
                    it.box.intersects(playerBox)
                }
                val serverDistance = entities.minOfOrNull {
                    it.pos.distanceTo(position)
                } ?: return@handler
                val clientDistance = entities.minOfOrNull {
                    it.pos.distanceTo(player.pos)
                } ?: return@handler

                // If the server position is not closer than the client position, we keep lagging.
                // Also, we don't want to lag if the player is intersecting with an entity.
                if (serverDistance < clientDistance || intersects) {
                    return@handler
                }

                PacketQueueManager.Action.QUEUE
            }
        }
    }

    override fun disable() {
        isEnemyNearby = false
        super.disable()
    }

}
