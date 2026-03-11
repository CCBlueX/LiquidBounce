/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.blink.BlinkManager.positions
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.autododge.ModuleAutoDodge
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.findEnemy
import net.ccbluex.liquidbounce.utils.combat.getEntitiesBoxInRange
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket
import net.minecraft.network.protocol.game.ClientboundExplodePacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import net.minecraft.network.protocol.game.ServerboundSwingPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull

/**
 * FakeLag module
 *
 * Holds back packets to prevent you from being hit by an enemy.
 */
@Suppress("MagicNumber")
object ModuleFakeLag : ClientModule("FakeLag", ModuleCategories.COMBAT) {
    private val range by floatRange("Range", 2f..5f, 0f..10f)
    private val delay by intRange("Delay", 300..600, 0..1000, "ms")
    private val recoilTime by int("RecoilTime", 250, 0..1000, "ms")
    private val mode by enumChoice("Mode", Mode.DYNAMIC).apply { tagBy(this) }

    private val flushOn by multiEnumChoice("FlushOn", FlushOn.entries)

    private enum class FlushOn(
        override val tag: String,
        val testPacket: (packet: Packet<*>?) -> Boolean
    ) : Tagged {
        ENTITY_INTERACT("EntityInteract", {
            it is ServerboundInteractPacket
            || it is ServerboundSwingPacket
        }),
        BLOCK_INTERACT("BlockInteract", {
            it is ServerboundUseItemOnPacket
            || it is ServerboundSignUpdatePacket
        }),
        ACTION("Action", {
            it is ServerboundPlayerActionPacket
        })
    }

    private enum class Mode(override val tag: String) : Tagged {
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
                BlinkManager.flush(TransferOrigin.OUTGOING)
            } else if (evadingPacket.ticksToImpact != null) {
                notification("FakeLag", "Trying to evade arrow...", NotificationEvent.Severity.INFO)
                BlinkManager.flush(evadingPacket.idx + 1)
            } else {
                notification("FakeLag", "Arrow evaded.", NotificationEvent.Severity.INFO)
                BlinkManager.flush(evadingPacket.idx + 1)
            }
        }
    }

    @Suppress("unused", "ComplexCondition")
    private val fakeLagHandler = handler<BlinkPacketEvent> { event ->
        if (event.origin != TransferOrigin.OUTGOING || player.isDeadOrDying || player.isInWater
            || mc.screen != null
        ) {
            return@handler
        }

        if (!chronometer.hasAtLeastElapsed(recoilTime.toLong())) {
            return@handler
        }

        if (BlinkManager.isAboveTime(nextDelay.toLong())) {
            nextDelay = delay.random()
            return@handler
        }

        if (flushOn.any { it.testPacket(event.packet) }) {
            chronometer.reset()
            return@handler
        }

        when (val packet = event.packet) {

            is ClientboundPlayerPositionPacket,
            is ServerboundResourcePackPacket -> {
                chronometer.reset()
                return@handler
            }

            is ServerboundInteractPacket,
            is ServerboundSwingPacket -> {
                if (FlushOn.ENTITY_INTERACT in flushOn) {
                    chronometer.reset()
                    return@handler
                }
            }

            // Flush on knockback
            is ClientboundSetEntityMotionPacket -> {
                if (packet.id == player.id && packet.movement != Vec3.ZERO) {
                    chronometer.reset()
                    return@handler
                }
            }

            // Flush on explosion
            is ClientboundExplodePacket -> {
                packet.playerKnockback.getOrNull()?.let { knockback ->
                    if (knockback != Vec3.ZERO) {
                        chronometer.reset()
                        return@handler
                    }
                }
            }

            // Flush on damage
            is ClientboundSetHealthPacket -> {
                chronometer.reset()
                return@handler
            }
        }

        // We don't want to lag when we are using an item that is not a food, milk bucket or potion.
        if (player.isUsingItem && player.useItem.isConsumable) {
            return@handler
        }

        // Support auto shoot with fake lag
        if (running && ModuleAutoShoot.constantLag && ModuleAutoShoot.targetTracker.target == null) {
            event.action = BlinkManager.Action.QUEUE
            return@handler
        }

        event.action = when (mode) {
            Mode.CONSTANT -> BlinkManager.Action.QUEUE
            Mode.DYNAMIC -> {
                // If there is an enemy in range, we want to lag.
                if (!isEnemyNearby) {
                    return@handler
                }

                val position = positions.firstOrNull() ?: run {
                    event.action = BlinkManager.Action.QUEUE
                    return@handler
                }
                val playerBox = player.dimensions.makeBoundingBox(position)

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
                    it.position().distanceTo(position)
                } ?: return@handler
                val clientDistance = entities.minOfOrNull {
                    it.position().distanceTo(player.position())
                } ?: return@handler

                // If the server position is not closer than the client position, we keep lagging.
                // Also, we don't want to lag if the player is intersecting with an entity.
                if (serverDistance < clientDistance || intersects) {
                    return@handler
                }

                BlinkManager.Action.QUEUE
            }
        }
    }

    override fun onDisabled() {
        isEnemyNearby = false
        super.onDisabled()
    }

}
