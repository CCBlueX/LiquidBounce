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

import net.ccbluex.fastutil.filterIsInstance
import net.ccbluex.fastutil.weightedMinByOrNullAtMost
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.blink.TrackedEntityPosition
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityReduce.debug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.multiply
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerDamage
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerVelocity
import net.ccbluex.liquidbounce.utils.raytracing.findEntityInCrosshair
import net.ccbluex.liquidbounce.utils.render.WireframePlayer
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

object VelocityReduce : VelocityMode("Reduce") {

    private val attackCount by intRange("AttackCount", 3..3, 0..20)
    private val alinkTargetRange by floatRange("AlinkTargetRange", 2.5f..6f, 0f..20f)
    private val alinkMaxDelay by int("AlinkMaxDelay", 20, 0..100, "ticks")
    private val alinkRequireKillAura by boolean("AlinkRequireKillAura", true)
    private val horizontal by float("Horizontal", 0.6f, 0f..1f)
    private val vertical by float("Vertical", 1.0f, 0f..1f)

    private object Debug : ToggleableValueGroup(this, "Debug", false) {
        val chatMessage by boolean("ChatMessage", false)
        val notification by boolean("Notification", false)

        var renderTarget: Entity? = null
        var renderTargetPos: TrackedEntityPosition? = null

        private val wireframePlayer = WireframePlayer()

        fun reset() {
            renderTarget = null
            renderTargetPos = null
        }

        fun notify(message: String) {
            if (!this.enabled) {
                return
            }

            if (notification) {
                notification(ModuleVelocity.name, message, NotificationEvent.Severity.INFO)
            }

            if (chatMessage) {
                chat(message)
            }
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (!debug.enabled) {
                return@handler
            }

            if (alinkTicks == -1 || renderTarget == null || renderTargetPos == null) return@handler

            wireframePlayer.pos = renderTargetPos!!.base
            wireframePlayer.yRot = renderTarget!!.yRot
            wireframePlayer.xRot = renderTarget!!.xRot
            wireframePlayer.render(
                event,
                Color4b.WHITE.alpha(100),
                outlineColor = Color4b.WHITE,
            )
        }
    }

    private val debug = tree(Debug)

    private val canAlink: Boolean
        get() = !alinkRequireKillAura || ModuleKillAura.running

    private var target: Entity? = null
    private var attackQueue = 0
    private var receiveDamage = false
    private var alinkTicks = -1
    private var releaseReason: String? = null

    val shouldStopBacktrack: Boolean
        get() = alinkTicks >= 0 || attackQueue > 0

    override fun enable() {
        target = null
        debug.reset()
        attackQueue = 0
        receiveDamage = false
        alinkTicks = -1
        releaseReason = null
    }

    override fun disable() {
        if (alinkTicks >= 0) {
            BlinkManager.flush(TransferOrigin.INCOMING)
        }
        target = null
        debug.reset()
        attackQueue = 0
        receiveDamage = false
        alinkTicks = -1
        releaseReason = null
    }

    private fun findTarget() {
        if (!canAlink && alinkTicks >= 0) return

        if (ModuleKillAura.running && ModuleKillAura.targetTracker.target != null) {
            if (alinkTicks == -1) {
                debug.renderTarget = ModuleKillAura.targetTracker.target
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
            debug.renderTarget = target
        }

        if (target != null) return

        if (alinkTicks >= 0) return

        debug.renderTarget = world.entitiesForRendering().filterIsInstance<LivingEntity> { entity ->
            !entity.isRemoved && entity.shouldBeAttacked()
        }.weightedMinByOrNullAtMost(alinkTargetRange.endInclusive.sq().toDouble()) { entity ->
            entity.squaredBoxedDistanceTo(player)
        }
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) return@handler

        val packet = event.packet

        if (alinkTicks >= 0) {
            when (packet) {
                is ClientboundPlayerPositionPacket -> {
                    releaseReason = "flag"
                }
            }

            val trackedTargetPosition = debug.renderTargetPos
            val trackedTarget = debug.renderTarget
            if (trackedTargetPosition != null && trackedTarget != null) {
                trackedTargetPosition.handlePacket(packet, world, trackedTarget)
            }

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

            if (debug.renderTarget == null) return@handler

            if ((target == null && canAlink) || (target != null && !player.isSprinting)) {
                if (target != null) {
                    debug.notify("Alink... (not sprinting)")
                } else {
                    debug.notify("Alink...")
                }

                if (target == null) {
                    debug.renderTargetPos = TrackedEntityPosition(debug.renderTarget!!.position())
                }
                alinkTicks = alinkMaxDelay
            } else if (target != null) {
                attackQueue = attackCount.random()
            }
        }
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<BlinkPacketEvent> { event ->
        if (alinkTicks >= 0 && event.origin == TransferOrigin.INCOMING) {
            event.action = BlinkManager.Action.QUEUE
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (attackQueue <= 0) {
            return@handler
        }

        if (target == null) {
            attackQueue = 0
            return@handler
        }
        repeat(attackQueue) {
            if (player.isSprinting) player.isSprinting = false
            attackEntity(target!!, SwingMode.DO_NOT_HIDE)
            player.deltaMovement = player.deltaMovement.multiply(horizontal, vertical, horizontal)
        }
        attackQueue = 0
        target = null
    }

    @Suppress("unused")
    private val tickPacketProcessEventHandler = handler<TickPacketProcessEvent> {
        if (releaseReason != null) {
            BlinkManager.flush(TransferOrigin.INCOMING)
            alinkTicks = -1
            debug.reset()
            if (releaseReason!!.isEmpty()) {
                debug.notify("Finish alink")
                attackQueue = attackCount.random()
            } else {
                debug.notify("Finish alink ($releaseReason)")
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
                event.directionalInput = DirectionalInput.FORWARDS
                releaseReason = ""
            } else if (player.distanceToSqr(debug.renderTargetPos?.base ?: Vec3.ZERO) > alinkTargetRange.endInclusive.sq()) {
                releaseReason = "out of range"
            } else if (alinkTicks == 0) {
                releaseReason = "max delay"
            }
        }
    }

}
