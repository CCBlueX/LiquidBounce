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
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.blink.TrackedEntityPosition
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspBox
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspData
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspModel
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspNone
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspWireframe
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
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
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3

/**
 * Attack Reduce
 */
object VelocityReduce : VelocityMode("Reduce") {

    private val attackCount by intRange("AttackCount", 3..3, 0..20)
    private val lagTargetRange by floatRange("LagTargetRange", 2f..6f, 0f..20f)
    private val lagMaxDelay by int("LagMaxDelay", 10, 1..1000, "ticks")
    private val lagRequireKillAura by boolean("LagRequireKillAura", false)
    private val horizontal by float("Horizontal", 0.6f, 0f..1f)
    private val vertical by float("Vertical", 1.0f, 0f..1f)

    private object Debug : ToggleableValueGroup(this, "Debug", false) {
        val chatMessage by boolean("ChatMessage", false)
        val notification by boolean("Notification", false)

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
    }

    private val debug = tree(Debug).apply {
        doNotIncludeAlways()
    }

    private val espMode = modes("BlinkEsp", 2) {
        arrayOf(
            BlinkEspBox(it, ::getEspData),
            BlinkEspModel(it, getEspData = ::getEspData),
            BlinkEspWireframe(it, ::getEspData),
            BlinkEspNone(it),
        )
    }.apply {
        doNotIncludeAlways()
    }

    private val canLag: Boolean
        get() = !lagRequireKillAura || ModuleKillAura.running

    private var target: Entity? = null
    private var renderTarget: Entity? = null
    private var renderTargetPos: TrackedEntityPosition? = null

    var remainingAttackCount = 0
        private set
    private var currentGameTick = 0L
    private var forwardInputAttackGameTick = -1L
    private var receiveDamage = false
    private var lagTicks = -1
    private var releaseReason: ReleaseReason? = null

    private enum class ReleaseReason(val debugSuffix: String?) {
        TARGET_REACHED(null),
        FLAG("flag"),
        SPECTATOR("spectator"),
        OUT_OF_RANGE("out of range"),
        MAX_DELAY("max delay"),
    }

    val backtrackBlocked: Boolean
        get() = lagTicks >= 0 || remainingAttackCount > 0

    val ownsIncomingBlinkQueue: Boolean
        get() = lagTicks >= 0

    private fun resetRenderState() {
        renderTarget = null
        renderTargetPos = null
    }

    override fun enable() {
        target = null
        resetRenderState()
        remainingAttackCount = 0
        currentGameTick = 0L
        forwardInputAttackGameTick = -1L
        receiveDamage = false
        lagTicks = -1
        releaseReason = null
    }

    override fun disable() {
        if (lagTicks >= 0) {
            BlinkManager.flush(TransferOrigin.INCOMING)
        }
        target = null
        resetRenderState()
        remainingAttackCount = 0
        currentGameTick = 0L
        forwardInputAttackGameTick = -1L
        receiveDamage = false
        lagTicks = -1
        releaseReason = null
    }

    private fun findTarget() {
        if (!canLag && lagTicks >= 0) return

        if (ModuleKillAura.running && ModuleKillAura.targetTracker.target != null) {
            if (lagTicks == -1) {
                renderTarget = ModuleKillAura.targetTracker.target
            }
            if (!canLag ||
                ModuleKillAura.targetTracker.target!!.squaredBoxedDistanceTo(player) <= lagTargetRange.start.sq()
            ) {
                target = ModuleKillAura.targetTracker.target
            }
            return
        }


        target = findEntityInCrosshair(
            (if (canLag) {
                lagTargetRange.start.toDouble()
            } else {
                ModuleKillAura.range.interactionRange.toDouble()
            }),
            RotationManager.currentRotation ?: player.rotation
        ) { !it.isRemoved && it.shouldBeAttacked() }?.entity

        if (lagTicks == -1) {
            renderTarget = target
        }

        if (target != null || lagTicks >= 0) return

        renderTarget = world.entitiesForRendering().filterIsInstance<LivingEntity> { entity ->
            !entity.isRemoved && entity.shouldBeAttacked()
        }.weightedMinByOrNullAtMost(lagTargetRange.endInclusive.sq().toDouble()) { entity ->
            entity.squaredBoxedDistanceTo(player)
        }
    }


    private fun getEspData(): BlinkEspData? {
        if (lagTicks == -1) {
            return null
        }

        val renderTarget = renderTarget ?: return null
        val renderTargetPos = renderTargetPos ?: return null
        return BlinkEspData(renderTarget, renderTargetPos.base, renderTarget.rotation)
    }

    private fun hasLostReduceTarget(): Boolean {
        val reduceTarget = target ?: return true

        if (!ModuleKillAura.running) {
            return false
        }

        val killAuraTarget = ModuleKillAura.targetTracker.target ?: return true
        return killAuraTarget.id != reduceTarget.id
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        if (event.origin != TransferOrigin.INCOMING) return@handler

        val packet = event.packet

        if (lagTicks >= 0) {
            if (packet is ClientboundPlayerPositionPacket) {
                releaseReason = ReleaseReason.FLAG
            }

            val trackedTargetPosition = renderTargetPos
            val trackedTarget = renderTarget
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

            if (renderTarget == null) return@handler

            if ((target == null && canLag) || (target != null && !player.isSprinting)) {
                if (target != null) {
                    debug.notify("Lag... (not sprinting)")
                } else {
                    debug.notify("Lag...")
                }

                if (target == null) {
                    renderTargetPos = TrackedEntityPosition(renderTarget!!)
                }
                lagTicks = lagMaxDelay
            } else if (target != null) {
                remainingAttackCount = attackCount.random()
            }
        }
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<BlinkPacketEvent> { event ->
        if (lagTicks >= 0 && event.origin == TransferOrigin.INCOMING) {
            event.action = BlinkManager.Action.QUEUE
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        currentGameTick++

        if (remainingAttackCount > 0) {
            if (hasLostReduceTarget()) {
                remainingAttackCount = 0
                target = null
                return@handler
            }
            player.isSprinting = false
            attackEntity(target!!, SwingMode.DO_NOT_HIDE)
            forwardInputAttackGameTick = currentGameTick
            player.deltaMovement = player.deltaMovement.multiply(horizontal, vertical, horizontal)
            remainingAttackCount--
            if (remainingAttackCount == 0) {
                target = null
            }
        }
    }

    @Suppress("unused")
    private val tickPacketProcessEventHandler = handler<TickPacketProcessEvent> {
        releaseReason?.let { releaseReason ->
            BlinkManager.flush(TransferOrigin.INCOMING)
            lagTicks = -1
            resetRenderState()
            if (releaseReason == ReleaseReason.TARGET_REACHED) {
                debug.notify("Finish lag")
                remainingAttackCount = attackCount.random()
            } else {
                debug.notify("Finish lag (${releaseReason.debugSuffix})")
            }
            this.releaseReason = null
        }
    }

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> { event ->
        if (lagTicks > 0 && releaseReason == null) {
            lagTicks--
            findTarget()

            when {
                player.abilities.flying -> releaseReason = ReleaseReason.SPECTATOR

                target != null -> {
                    event.directionalInput = DirectionalInput.FORWARDS
                    releaseReason = ReleaseReason.TARGET_REACHED
                }

                player.distanceToSqr(renderTargetPos?.base ?: Vec3.ZERO) > lagTargetRange.endInclusive.sq() -> {
                    releaseReason = ReleaseReason.OUT_OF_RANGE
                }

                lagTicks == 0 -> releaseReason = ReleaseReason.MAX_DELAY
            }
        }

        if (currentGameTick == forwardInputAttackGameTick) {
            event.directionalInput = event.directionalInput.copy(
                forwards = true,
                backwards = false
            )
        }
    }

}
