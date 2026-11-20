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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.GameRenderTaskQueueEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMovementTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager.Action
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleEasyPearl
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.network.UseItemPacketRotation
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.ccbluex.liquidbounce.utils.entity.anyHorizontal
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.protocol.common.ServerboundPongPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import java.util.function.BooleanSupplier
import kotlin.math.abs
import kotlin.random.Random

/**
 * Freeze module
 *
 * Allows you to freeze yourself without the server knowing.
 */
object ModuleFreeze : ClientModule("Freeze", ModuleCategories.MOVEMENT, disableOnQuit = true) {

    private val modes = choices("Mode", Stationary, arrayOf(Queue, Cancel, Stationary, TickMovement))
        .apply { tagBy(this) }
    private val disableOn by multiEnumChoice("DisableOn", enumSetOf(DisableOn.FLAG))
    private val notification by boolean("Notification", false)

    private enum class DisableOn(
        override val tag: String,
        val trigger: BooleanSupplier?,
    ) : Tagged {
        FLAG("Flag", null),
        ON_GROUND("OnGround", { player.onGround() }),
        ON_MOVEMENT_INPUT("OnMovementInput", { player.input.keyPresses.anyHorizontal }),
        IN_LIQUID("InLiquid", { player.isInLiquid }),
        VOID("Void", { player.y <= player.level().minY }),
        ON_USE_ITEM("OnUseItem", { player.isUsingItem }),
    }

    private fun notifyAndDisable(reason: DisableOn) {
        if (notification) {
            notification(
                this.name,
                message("disabled", reason.tag),
                NotificationEvent.Severity.INFO
            )
        }
        enabled = false
    }

    override fun onEnabled() {
        holdTicks()
    }

    // Runs every frame, since no game tick runs while frozen
    @Suppress("unused")
    private val frameHandler = handler<GameRenderTaskQueueEvent> {
        for (reason in disableOn) {
            if (reason.trigger?.asBoolean ?: continue) {
                notifyAndDisable(reason)
            }
        }

        holdTicks()
    }

    // Acts as timer = 0 replacement
    private fun holdTicks() {
        if (modes.activeMode !== TickMovement) {
            Timer.hold(this, Int.MAX_VALUE)
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is ClientboundPlayerPositionPacket && DisableOn.FLAG in disableOn) {
            notifyAndDisable(DisableOn.FLAG)
        }
    }

    /**
     * Queue network communication - acts as network lag
     */
    private object Queue : Mode("Queue") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val origins by multiEnumChoice("Origin", TransferOrigin.OUTGOING)

        @Suppress("unused")
        private val fakeLagHandler = handler<BlinkPacketEvent>(
            priority = EventPriorityConvention.SAFETY_FEATURE
        ) { event ->
            if (event.origin in origins) {
                event.action = Action.QUEUE
            }
        }

    }

    /**
     * Cancel network communication
     */
    private object Cancel : Mode("Cancel") {

        private val origins by multiEnumChoice("Origin", TransferOrigin.OUTGOING)

        override val parent: ModeValueGroup<Mode>
            get() = modes

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent> { event ->
            if (event.origin in origins) {
                event.cancelEvent()
            }
        }

    }

    /**
     * Stationary freeze - only cancel movement but keeps network communication intact
     */
    private object Stationary : Mode("Stationary") {
        /**
         * Bypasses Grim's BadPacketsR and Matrix7 Timer Check
         */
        private val cancelC0B by boolean("CancelC0B",true)
        private val yawOffset = FloatOffsetGenerator()
        private val pitchOffset = FloatOffsetGenerator()

        override val parent: ModeValueGroup<Mode>
            get() = modes

        /**
         * Bypasses Grim's duplicate rotation check
         */
        private class FloatOffsetGenerator : FloatIterator() {
            private var prev = 0f
            override fun hasNext() = true
            override fun nextFloat(): Float {
                var offset: Float
                do {
                    offset = Random.nextDouble(0.002, 0.01).toFloat()
                } while (abs(offset - prev) < 1.0E-6F)
                return offset.also { prev = it }
            }
        }

        @Suppress("unused")
        private val packetEventHandler = handler<PacketEvent> { event ->
            val yaw = RotationManager.currentRotation?.yaw ?: player.yRot
            val pitch = RotationManager.currentRotation?.pitch ?: player.xRot
            val yawOffset = yawOffset.nextFloat()
            val pitchOffset = pitchOffset.nextFloat()

            when (val packet = event.packet) {

                is ServerboundPongPacket -> {
                    if (cancelC0B) {
                        event.cancelEvent()
                    }
                }

                is ServerboundUseItemPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        ServerboundMovePlayerPacket.Rot(
                            ModuleEasyPearl.currentTargetRotation?.yaw ?: (player.yRot + yawOffset),
                            ModuleEasyPearl.currentTargetRotation?.pitch ?: (player.xRot + pitchOffset),
                            player.onGround(),
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(
                        UseItemPacketRotation.createExplicit(
                            packet.hand,
                            packet.sequence,
                            yaw + yawOffset,
                            pitch + pitchOffset,
                        )
                    )
                }

                is ServerboundInteractPacket, is ServerboundAttackPacket, is ServerboundSpectatorActionPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        ServerboundMovePlayerPacket.Rot(
                            yaw + yawOffset,
                            pitch + pitchOffset,
                            player.onGround(),
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }

                is ServerboundUseItemOnPacket -> {
                    event.cancelEvent()
                    sendPacketSilently(
                        ServerboundMovePlayerPacket.Rot(
                            yaw + yawOffset,
                            pitch + pitchOffset,
                            player.onGround(),
                            player.horizontalCollision
                        )
                    )
                    sendPacketSilently(packet)
                }
            }
        }

    }

    private object TickMovement : Mode("TickMovement") {

        private val interval by intRange("Interval", 20..20, 1..200, "ticks")
        private var ticksUntilMovement = 0

        override val parent: ModeValueGroup<Mode>
            get() = modes

        override fun enable() {
            ticksUntilMovement = interval.random()
        }

        override fun disable() {
            ticksUntilMovement = 0
        }

        @Suppress("unused")
        private val movementTickHandler = handler<PlayerMovementTickEvent> { event ->
            if (--ticksUntilMovement <= 0) {
                ticksUntilMovement = interval.random()
                return@handler
            }

            event.cancelEvent()
        }

    }

}
