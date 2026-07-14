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
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TickPacketProcessEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.blink.TrackedEntityPosition
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspBox
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspData
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspModel
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspNone
import net.ccbluex.liquidbounce.features.blink.esp.BlinkEspWireframe
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.ModuleVelocity
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.AccelerationAngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.InterpolationAngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.LinearAngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.SigmoidAngleSmooth
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.attackEntity
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.multiply
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerDamage
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerVelocity
import net.ccbluex.liquidbounce.utils.raytracing.findEntityInCrosshair
import net.minecraft.network.protocol.common.ClientboundKeepAlivePacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity

/**
 * Attack Reduce
 */
object VelocityReduce : VelocityMode("Reduce") {

    private val attackCount by intRange("AttackCount", 5..5, 0..20)

    private enum class AttackMode(override val tag: String) : Tagged {
        ONE_TIME("OneTime"),
        PER_TICK("PerTick")
    }

    private val attackMode by enumChoice("AttackMode", AttackMode.PER_TICK)
    private val attackTargetRange by floatRange("AttackTargetRange", 2f..3f, 0f..6f)
    private val lagInAir by boolean("LagInAir", true)
    private val lagTargetRange by float("LagTargetRange", 6f, 0f..20f)
    private val lagMaxDelay by int("LagMaxDelay", 30, 0..200, "ticks")
    private val requireKillAura by boolean("RequireKillAura", true)
    private val horizontal by float("Horizontal", 0.6f, 0f..1f)
    private val vertical by float("Vertical", 0.6f, 0f..1f)

    private val autoRotate = tree(object : ToggleableValueGroup(this, "AutoRotate", true) {
        val rotationTime by int("RotationTime", 2, 0..20, "ticks")
        val angleSmooth = choices("AngleSmooth", 0) {
            val linear = LinearAngleSmooth(it)
            val interpolation = InterpolationAngleSmooth(it)
            arrayOf(
                linear,
                SigmoidAngleSmooth(it),
                interpolation,
                AccelerationAngleSmooth(it)
            )
        }
        val notDuringKillAura by boolean("NotDuringKillAura", false)

        val canRotate: Boolean
            get() = enabled
                && (!notDuringKillAura
                || !ModuleKillAura.running
                || ModuleKillAura.targetTracker.target == null)
    })

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

    // ESPRender

    @Suppress("unused")
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

    // State

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

    // Computed properties

    private val canLag: Boolean
        get() = !requireKillAura || ModuleKillAura.running

    val backtrackBlocked: Boolean
        get() = lagTicks >= 0 || remainingAttackCount > 0

    val ownsIncomingBlinkQueue: Boolean
        get() = lagTicks >= 0

    private val isInAir: Boolean
        get() = !player.onGround() && !player.isInLiquid

    // Helpers

    private fun resetRenderState() {
        renderTarget = null
        renderTargetPos = null
    }

    private fun getCurrentAttackCount(): Int = attackCount.random()

    private fun attackCurrentTarget(targetEntity: Entity) {
        val sprinting = player.isSprinting
        if (sprinting) player.isSprinting = false
        attackEntity(targetEntity, SwingMode.DO_NOT_HIDE)
        forwardInputAttackGameTick = currentGameTick
        if (sprinting) {
            player.deltaMovement = player.deltaMovement.multiply(
                horizontal, vertical, horizontal
            )
        }
    }

    private fun rotate(targetEntity: Entity) {
        val rotation = Rotation.lookingAt(point = targetEntity.boundingBox.center, from = player.eyePosition)
        RotationManager.setRotationTarget(
            RotationTarget(
                rotation = rotation,
                entity = targetEntity,
                processors = listOf(autoRotate.angleSmooth.activeMode),
                ticksUntilReset = autoRotate.rotationTime,
                resetThreshold = 2f,
                considerInventory = false,
                movementCorrection = MovementCorrection.STRICT
            ),
            priority = Priority.IMPORTANT_FOR_USAGE_2,
            provider = ModuleVelocity
        )
    }

    private fun getEspData(): BlinkEspData? {
        if (lagTicks == -1) {
            return null
        }
        val rt = renderTarget ?: return null
        val rtp = renderTargetPos ?: return null
        return BlinkEspData(rt, rtp.base, rt.rotation)
    }

    // Enable / Disable

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

    // Target finding

    private fun trySetKillAuraTarget(): Boolean {
        if (!ModuleKillAura.running) return false
        val killAuraTarget = ModuleKillAura.targetTracker.target
        if (killAuraTarget == null) return false
        if (lagTicks == -1) {
            renderTarget = killAuraTarget
        }
        if (!canLag || killAuraTarget.squaredBoxedDistanceTo(player) <= attackTargetRange.endInclusive.sq()) {
            target = killAuraTarget
        }
        return true
    }

    private fun findTarget() {
        if (trySetKillAuraTarget()) return

        target = findEntityInCrosshair(
            attackTargetRange.start.toDouble(),
            RotationManager.currentRotation ?: player.rotation
        ) { !it.isRemoved && it.shouldBeAttacked() }?.entity

        if (lagTicks == -1) {
            renderTarget = target
        }
        if (target != null) return

        val nearestEntity = world.entitiesForRendering()
            .filterIsInstance<LivingEntity> { !it.isRemoved && it.shouldBeAttacked() }
            .minByOrNull { it.distanceTo(player) }

        renderTarget = nearestEntity
        if (nearestEntity == null) return

        val canAutoRotate = lagTicks >= 0 && autoRotate.canRotate
                && nearestEntity.squaredBoxedDistanceTo(player) <= attackTargetRange.start.sq()
        if (!canAutoRotate) return

        rotate(nearestEntity)
        target = nearestEntity
    }

    // Event handlers

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
            findTarget()

            if (renderTarget == null && !(lagInAir && isInAir)) return@handler

            if (target == null || !player.isSprinting || (lagInAir && isInAir)) {
                debug.notify(when {
                    !player.isSprinting -> "Lag... (not sprinting)"
                    else -> "Lag..."
                })
                val rt = renderTarget
                if (rt != null) {
                    renderTargetPos = TrackedEntityPosition(rt)
                }
                lagTicks = lagMaxDelay
            } else if (target != null) {
                remainingAttackCount = getCurrentAttackCount()
                debug.notify("Attack count: $remainingAttackCount")
            }
        }
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<BlinkPacketEvent> { event ->
        if (lagTicks >= 0 && event.origin == TransferOrigin.INCOMING && event.packet !is ClientboundKeepAlivePacket) {
            event.action = BlinkManager.Action.QUEUE
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        currentGameTick++

        if (remainingAttackCount > 0) {
            if (target == null || target !in world.entitiesForRendering()) {
                remainingAttackCount = 0
                target = null
                return@handler
            }

            when (attackMode) {
                AttackMode.ONE_TIME -> {
                    for (i in 1..remainingAttackCount) {
                        if (target !in world.entitiesForRendering()) break
                        attackCurrentTarget(target!!)
                    }
                    remainingAttackCount = 0
                    target = null
                }

                AttackMode.PER_TICK -> {
                    val currentTarget = target ?: return@handler
                    if (currentTarget.boxedDistanceTo(player) > attackTargetRange.endInclusive) {
                        debug.notify("Unable to attack")
                        remainingAttackCount--
                        if (remainingAttackCount == 0) {
                            target = null
                        }
                        return@handler
                    }
                    if (autoRotate.canRotate) {
                        rotate(currentTarget)
                    }
                    attackCurrentTarget(currentTarget)
                    remainingAttackCount--
                    if (remainingAttackCount == 0) {
                        target = null
                    }
                }
            }
        }
    }

    @Suppress("unused")
    private val tickPacketProcessHandler = sequenceHandler<TickPacketProcessEvent> {
        val reason = releaseReason ?: return@sequenceHandler
        BlinkManager.flush(TransferOrigin.INCOMING)
        lagTicks = -1
        resetRenderState()
        if (reason == ReleaseReason.TARGET_REACHED) {
            debug.notify("Finish lag")
            remainingAttackCount = getCurrentAttackCount()
        } else {
            debug.notify("Finish lag (${reason.debugSuffix})")
        }
        releaseReason = null
    }

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> { event ->
        if (remainingAttackCount > 0) {
            event.directionalInput = DirectionalInput.FORWARDS
        }

        if (lagTicks >= 0 && releaseReason == null) {
            if (lagTicks > 0) lagTicks--
            findTarget()

            when {
                lagTicks == 0 -> releaseReason = ReleaseReason.MAX_DELAY
                player.abilities.flying -> releaseReason = ReleaseReason.SPECTATOR
                renderTarget !in world.entitiesForRendering() -> releaseReason = ReleaseReason.OUT_OF_RANGE
                renderTargetPos?.let { pos ->
                    player.distanceToSqr(pos.base) > lagTargetRange.sq()
                } == true -> releaseReason = ReleaseReason.OUT_OF_RANGE
                target != null && (!lagInAir || !isInAir) -> {
                    remainingAttackCount = getCurrentAttackCount()
                    releaseReason = ReleaseReason.TARGET_REACHED
                    event.directionalInput = DirectionalInput.FORWARDS
                }
            }

            if (releaseReason != null && releaseReason != ReleaseReason.TARGET_REACHED) {
                if (player.onGround() && player.isSprinting) {
                    player.isSprinting = false
                }
            }
        }

        if (currentGameTick == forwardInputAttackGameTick) {
            event.directionalInput = DirectionalInput.FORWARDS
        }
    }

}
