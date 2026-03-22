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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import com.google.common.collect.Queues
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.events.NotificationEvent

import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerDamage
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerVelocity
import net.ccbluex.liquidbounce.utils.raytracing.findEntityInCrosshair
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.world.InteractionHand
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.game.ClientboundDisguisedChatPacket
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket
import net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket
import net.minecraft.network.protocol.game.ClientboundHurtAnimationPacket
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundMoveEntityPacket
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundRotateHeadPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ClientboundSetEquipmentPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket
import net.minecraft.network.protocol.game.ClientboundUpdateAttributesPacket
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.VecDeltaCodec
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
/*
*for play.bjd-mc.com:19132
*/

object VelocityReduce : VelocityMode("Reduce") {

    private val attackCount by intRange("AttackCount", 3..3, 0..20)
    private val alinkTargetRange by floatRange("AlinkTargetRange", 2.5f..6f, 0f..20f)
    private val alinkMaxDelay by int("AlinkMaxDelay", 20, 0..100, "ticks")
    private val alinkRequireKillAura by boolean("AlinkRequireKillAura", true)
    private val horizontal by float("Horizontal", 0.6f, 0f..1f)
    private val vertical by float("Vertical", 1.0f, 0f..1f)
    private val chatMessage by boolean("ChatMessage", false)
    private val notification by boolean("Notification", false)

    private val canAlink: Boolean
        get() = !alinkRequireKillAura || ModuleKillAura.running

    private var target: Entity? = null
    private var renderTarget: Entity? = null
    private var renderTargetPos: TrackedPosition? = null
    private var attackQueue = 0
    private var receiveDamage = false
    private var alinkTicks = -1
    private var releaseReason: String? = null
    private val packets = Queues.newConcurrentLinkedQueue<Packet<*>>()


    val shouldStopBacktrack: Boolean
        get() = alinkTicks >= 0 || attackQueue > 0

    override fun enable() {
        target = null
        renderTarget = null
        renderTargetPos = null
        attackQueue = 0
        receiveDamage = false
        alinkTicks = -1
        releaseReason = null
        packets.clear()
    }

    override fun disable() {
        handle()
        target = null
        renderTarget = null
        renderTargetPos = null
        attackQueue = 0
        receiveDamage = false
        alinkTicks = -1
        releaseReason = null
        packets.clear()

    }

    private fun findTarget() {
        if (!canAlink && alinkTicks >= 0) return

        if (ModuleKillAura.running && ModuleKillAura.targetTracker.target != null) {
            if (alinkTicks == -1) {
                renderTarget = ModuleKillAura.targetTracker.target
            }
            if (!canAlink ||
                ModuleKillAura.targetTracker.target!!.squaredBoxedDistanceTo(player) <= alinkTargetRange.start.sq()
            ) {
                target = ModuleKillAura.targetTracker.target
            }
            return
        }

        target = findEntityInCrosshair(
            (if (canAlink) {
                alinkTargetRange.start.toDouble()
            } else {
                ModuleKillAura.range.interactionRange.toDouble()
            }),
            RotationManager.currentRotation ?: player.rotation
        ) { !it.isRemoved && it.shouldBeAttacked() }?.entity

        if (alinkTicks == -1) {
            renderTarget = target
        }

        if (target != null) return

        if (alinkTicks >= 0) return

        val farTarget = world.entitiesForRendering().filter { entity ->
            entity is LivingEntity
                && entity != player
                && !entity.isRemoved
                && entity.shouldBeAttacked()
                && entity.squaredBoxedDistanceTo(player) <= alinkTargetRange.endInclusive.sq().toDouble()
        }.minByOrNull { entity -> entity.squaredBoxedDistanceTo(player) }

        renderTarget = farTarget
    }

    private fun handle() {
        packets.removeIf {
            if (it is Packet<*>) {
                @Suppress("UNCHECKED_CAST")
                (it as Packet<ClientGamePacketListener>).handle(network)
            }
            true
        }
    }

    private fun notifyDebug(message: String) {
        if (notification) {
            notification(ModuleVelocity.name, message, NotificationEvent.Severity.INFO)
        }

        if (chatMessage) {
            chat(message)
        }
    }


    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) return@handler

        val packet = event.packet

        if (alinkTicks >= 0) {
            when (packet) {
                is ClientboundSystemChatPacket,
                is ClientboundDisguisedChatPacket -> {
                    return@handler
                }

                is ClientboundDisconnectPacket,
                is ClientboundRespawnPacket,
                is ClientboundLoginPacket -> {
                    handle()
                    return@handler
                }

                is ClientboundPlayerPositionPacket -> {
                    releaseReason = "flag"
                    return@handler
                }

                is ClientboundMoveEntityPacket -> {
                    if (renderTargetPos != null && packet.getEntity(world) == renderTarget) {
                        renderTargetPos!!.decode(packet.xa.toLong(), packet.ya.toLong(), packet.za.toLong())
                    }
                }

                is ClientboundTeleportEntityPacket -> {
                    if (renderTargetPos != null && packet.id == renderTarget!!.id) {
                        renderTargetPos!!.set(packet.change.position)
                    }
                }

                is ClientboundEntityPositionSyncPacket -> {
                    if (renderTargetPos != null && packet.id == renderTarget!!.id) {
                        renderTargetPos!!.set(packet.values.position)
                    }
                }
            }

            event.cancelEvent()
            packets.add(packet)
            return@handler
        }

        if (ModuleVelocity.pause > 0) return@handler

        if (packet.isLocalPlayerDamage()) {
            receiveDamage = true
        }

        if (packet.isLocalPlayerVelocity() && receiveDamage) {
            receiveDamage = false
            if (player.isUsingItem || ModuleScaffold.running) return@handler

            findTarget()

            if (renderTarget == null) return@handler

            if ((target == null && canAlink) || (target != null && !player.isSprinting)) {
                if (target != null) {
                    notifyDebug("Alink... (not sprinting)")
                } else {
                    notifyDebug("Alink...")
                }

                if (target == null) {
                    renderTargetPos = TrackedPosition().apply { this.set(renderTarget!!.position()) }
                }
                alinkTicks = alinkMaxDelay
                event.cancelEvent()
                packets.add(packet)
            } else if (target != null) {
                attackQueue = attackCount.random()
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (attackQueue > 0) {
            if (target == null) {
                attackQueue = 0
                return@tickHandler
            }
            repeat(attackQueue) {
                if (player.isSprinting) player.isSprinting = false
                network.send(ServerboundInteractPacket.createAttackPacket(target!!, player.isShiftKeyDown))
                player.swing(InteractionHand.MAIN_HAND)
                player.deltaMovement = Vec3(
                    player.deltaMovement.x * horizontal,
                    player.deltaMovement.y * vertical,
                    player.deltaMovement.z * horizontal
                )
            }
            attackQueue = 0
            target = null
        }
    }

    @Suppress("unused")
    private val tickPacketProcessEventHandler = handler<TickPacketProcessEvent> {
        if (releaseReason != null) {
            handle()
            alinkTicks = -1
            renderTarget = null
            renderTargetPos = null
            if (releaseReason!!.isEmpty()) {
                notifyDebug("Finish alink")
                attackQueue = attackCount.random()
            } else {
                notifyDebug("Finish alink ($releaseReason)")
            }
            releaseReason = null
        }
    }

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> { event ->
        if (alinkTicks > 0 && releaseReason == null) {
            alinkTicks--
            findTarget()

            if (player.abilities.flying) {
                releaseReason = "spectator"
            } else if (target != null) {
                event.directionalInput = DirectionalInput(
                    forwards = true,
                    backwards = false,
                    left = false,
                    right = false
                )
                releaseReason = ""
            } else if (player.distanceToSqr(renderTargetPos?.pos ?: Vec3.ZERO) > alinkTargetRange.endInclusive.sq()) {
                releaseReason = "out of range"
            } else if (alinkTicks == 0) {
                releaseReason = "max delay"
            }
        }
    }

    private val wireframePlayer = WireframePlayer()

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (alinkTicks == -1 || renderTarget == null || renderTargetPos == null) return@handler

        wireframePlayer.pos = renderTargetPos!!.pos
        wireframePlayer.yRot = renderTarget!!.yRot
        wireframePlayer.xRot = renderTarget!!.xRot
        wireframePlayer.render(
            event,
            Color4b.WHITE.alpha(100),
            outlineColor = Color4b.WHITE,
        )
    }

    private class TrackedPosition {
        private val codec = VecDeltaCodec()
        val pos: Vec3
            get() = codec.base

        fun set(pos: Vec3) {
            codec.setBase(pos)
        }

        fun decode(xa: Long, ya: Long, za: Long) {
            codec.setBase(codec.decode(xa, ya, za))
        }
    }

}
