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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.CrystalAuraDamageOptions.RequestingSubmodule
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.destroy.SubmoduleCrystalDestroyer
import net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura.trigger.CrystalAuraTriggerer
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBox
import net.ccbluex.liquidbounce.utils.math.topCenter
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
import net.minecraft.network.protocol.game.ServerboundAttackPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.boss.enderdragon.EndCrystal
import net.minecraft.world.phys.AABB
import kotlin.math.max

private const val MAX_PREDICTION_AGE_MS = 1000L

internal data class PendingPrediction(
    val generation: Long,
    val placePos: BlockPos,
    val highestId: Int,
    val createdAtMs: Long,
    val dispatchedIds: List<Int> = emptyList(),
    val dispatchedAtMs: Long? = null,
)

internal enum class PredictedEntityKind {
    END_CRYSTAL,
    OTHER,
}

internal enum class PredictionRejectReason {
    STALE_ACTION,
    EXPIRED,
    POSITION_MISMATCH,
    ALREADY_DISPATCHED,
    UNEXPECTED_ENTITY,
    ID_OVERFLOW,
    EMPTY_RANGE,
}

internal sealed interface PredictionOutcome {
    data class Dispatch(val entityIds: List<Int>) : PredictionOutcome
    data class AttackExisting(val entityId: Int) : PredictionOutcome
    data class Reject(val reason: PredictionRejectReason, val entityId: Int? = null) : PredictionOutcome
}

internal data class PredictionTransition(
    val pending: PendingPrediction?,
    val outcome: PredictionOutcome,
)

/**
 * Resolves a predicted attack without accessing the world or sending packets. A stale call cannot clear or replace a
 * newer pending action. An existing non-crystal invalidates the complete offset sequence because later IDs are no
 * longer trustworthy either.
 */
internal fun planPrediction(
    pending: PendingPrediction?,
    generation: Long,
    placePos: BlockPos,
    offsetRange: IntRange,
    nowMs: Long,
    maxAgeMs: Long,
    existingEntities: Map<Int, PredictedEntityKind>,
): PredictionTransition {
    if (pending == null || pending.generation != generation) {
        return PredictionTransition(pending, PredictionOutcome.Reject(PredictionRejectReason.STALE_ACTION))
    }

    if (pending.placePos != placePos) {
        return PredictionTransition(null, PredictionOutcome.Reject(PredictionRejectReason.POSITION_MISMATCH))
    }

    val age = nowMs - pending.createdAtMs
    if (age < 0L || age > maxAgeMs.coerceAtLeast(0L)) {
        return PredictionTransition(null, PredictionOutcome.Reject(PredictionRejectReason.EXPIRED))
    }

    if (pending.dispatchedAtMs != null) {
        return PredictionTransition(
            pending,
            PredictionOutcome.Reject(PredictionRejectReason.ALREADY_DISPATCHED)
        )
    }

    if (offsetRange.isEmpty()) {
        return PredictionTransition(null, PredictionOutcome.Reject(PredictionRejectReason.EMPTY_RANGE))
    }

    val ids = offsetRange.map { offset ->
        val id = pending.highestId.toLong() + offset
        if (id !in 1L..Int.MAX_VALUE.toLong()) {
            return PredictionTransition(null, PredictionOutcome.Reject(PredictionRejectReason.ID_OVERFLOW))
        }
        id.toInt()
    }

    ids.forEach { id ->
        when (existingEntities[id]) {
            PredictedEntityKind.OTHER -> return PredictionTransition(
                null,
                PredictionOutcome.Reject(PredictionRejectReason.UNEXPECTED_ENTITY, id)
            )
            PredictedEntityKind.END_CRYSTAL -> return PredictionTransition(
                null,
                PredictionOutcome.AttackExisting(id)
            )
            null -> Unit
        }
    }

    return PredictionTransition(
        pending.copy(dispatchedIds = ids, dispatchedAtMs = nowMs),
        PredictionOutcome.Dispatch(ids)
    )
}

/**
 * Allows the crystal aura to send a break packet right when a crystal is placed by predicting the
 * expected entity id.
 */
object SubmoduleIdPredict : ToggleableValueGroup(ModuleCrystalAura, "IDPredict", false) {

    /**
     * Sends a packet for all included offsets.
     */
    private val offsetRange by intRange("OffsetRange", 1..2, 1..100)

    /**
     * Swings before every attack. Otherwise, it will only swing once.
     *
     * Only works when [SubmoduleCrystalDestroyer.swingMode] is enabled.
     */
    private val swingAlways by boolean("SwingAlways", false)

    /**
     * Sends an additional rotation packet.
     */
    private object Rotate : ToggleableValueGroup(this, "Rotate", true) {

        val back by boolean("Back", false)

        var oldRotation: Rotation? = null

        fun sendRotation(rotation: Rotation) {
            if (!enabled) {
                return
            }

            oldRotation = RotationManager.serverRotation
            network.send(
                ServerboundMovePlayerPacket.PosRot(
                player.x,
                player.y,
                player.z,
                rotation.yaw,
                rotation.pitch,
                player.onGround(),
                player.horizontalCollision
            ))
        }

        fun rotateBack() {
            if (!enabled || !back || oldRotation == null) {
                return
            }

            network.send(
                ServerboundMovePlayerPacket.PosRot(
                player.x,
                player.y,
                player.z,
                oldRotation!!.yaw,
                oldRotation!!.pitch,
                player.onGround(),
                player.horizontalCollision
            ))
        }

    }

    init {
        tree(Rotate)
    }

    private val highestIdMonitor = Object()

    @Volatile
    private var highestId = 0
        set(value) {
            field = value
            ModuleCrystalAura.debugParameter("Highest ID") { highestId }
        }

    private var generation = 0L

    @Volatile
    private var pendingPrediction: PendingPrediction? = null
    private var predictionTargetId: Int? = null

    override fun onEnabled() {
        reset()
    }

    override fun onDisabled() {
        invalidatePrediction()
        Rotate.oldRotation = null
    }

    /**
     * Must be called after the placement interaction was dispatched. The action is rebound to the current world,
     * target, position, and entity-ID snapshot before any attack packet is sent.
     */
    @Suppress("CognitiveComplexMethod", "LongMethod", "ReturnCount")
    fun run(placePos: BlockPos) {
        if (!enabled) {
            return
        }

        val targetPos = placePos.immutable()
        if (!mc.isSameThread) {
            val requestWorld = mc.level ?: return
            mc.execute {
                if (mc.level === requestWorld) {
                    run(targetPos)
                }
            }
            return
        }

        // A new dispatched placement supersedes all earlier ID assumptions, even if this action later fails validation.
        invalidatePrediction()
        if (!running || !CrystalAuraTriggerer.running || !SubmoduleCrystalDestroyer.enabled) {
            return
        }

        val target = ModuleCrystalAura.targetTracker.target ?: return
        val crystalPos = targetPos.topCenter
        if (CrystalAuraDamageOptions.approximateExplosionDamage(crystalPos, RequestingSubmodule.DESTROY) == null) {
            return
        }

        val (rotation, _) = raytraceBox(
            player.eyePosition,
            AABB(targetPos).inflate(0.5, 0.0, 0.5).setMaxY(targetPos.y + 2.0),
            range = SubmoduleCrystalDestroyer.range.toDouble(),
            wallsRange = SubmoduleCrystalDestroyer.wallsRange.toDouble(),
        ) ?: return

        val now = System.currentTimeMillis()
        val action = PendingPrediction(++generation, targetPos, snapshotHighestId(), now)
        val existingEntities = buildMap {
            offsetRange.forEach { offset ->
                val id = action.highestId.toLong() + offset
                if (id in 1L..Int.MAX_VALUE.toLong()) {
                    world.getEntity(id.toInt())?.let { entity ->
                        put(
                            id.toInt(),
                            if (entity is EndCrystal) PredictedEntityKind.END_CRYSTAL else PredictedEntityKind.OTHER
                        )
                    }
                }
            }
        }

        val transition = planPrediction(
            action,
            action.generation,
            targetPos,
            offsetRange,
            now,
            MAX_PREDICTION_AGE_MS,
            existingEntities
        )
        pendingPrediction = transition.pending

        when (val outcome = transition.outcome) {
            is PredictionOutcome.Dispatch -> {
                predictionTargetId = target.id
                dispatchPredictedAttack(rotation.normalize(), outcome.entityIds)
            }
            is PredictionOutcome.AttackExisting -> {
                // Existing entities use the complete normal target validation, damage, rotation, and attack chain.
                val crystal = world.getEntity(outcome.entityId) as? EndCrystal ?: return
                SubmoduleCrystalDestroyer.tick(crystal)
            }
            is PredictionOutcome.Reject -> Unit
        }
    }

    private fun dispatchPredictedAttack(rotation: Rotation, entityIds: List<Int>) {
        Rotate.sendRotation(rotation)
        try {
            val swingMode = SubmoduleCrystalDestroyer.swingMode
            if (!swingAlways) {
                swingMode.attackSwing()
            }

            entityIds.forEach { id ->
                if (swingAlways) {
                    swingMode.attackSwing()
                }

                network.send(ServerboundAttackPacket(id))
                SubmoduleCrystalDestroyer.postAttackHandlers.forEach { it.attacked(id) }
            }
        } finally {
            Rotate.rotateBack()
        }

        SubmoduleCrystalDestroyer.chronometer.reset()
    }

    private fun invalidatePrediction(expectedGeneration: Long? = null) {
        if (!mc.isSameThread) {
            mc.execute { invalidatePrediction(expectedGeneration) }
            return
        }

        if (expectedGeneration != null && pendingPrediction?.generation != expectedGeneration) {
            return
        }

        generation++
        pendingPrediction = null
        predictionTargetId = null
    }

    private fun reset() {
        invalidatePrediction()
        synchronized(highestIdMonitor) {
            highestId = 0
        }
        if (!mc.isSameThread) {
            mc.execute { rebuildHighestId() }
        } else {
            rebuildHighestId()
        }
    }

    private fun rebuildHighestId() {
        mc.level?.entitiesForRendering()?.forEach {
            recordHighestId(it.id)
        }
    }

    private fun snapshotHighestId(): Int = synchronized(highestIdMonitor) {
        highestId
    }

    private fun recordHighestId(id: Int) {
        synchronized(highestIdMonitor) {
            highestId = max(id, highestId)
        }
    }

    @Suppress("unused")
    private val targetLossHandler = handler<GameTickEvent> {
        val action = pendingPrediction ?: return@handler
        val target = ModuleCrystalAura.targetTracker.target
        val age = System.currentTimeMillis() - action.createdAtMs
        if (target == null || target.id != predictionTargetId || age < 0L || age > MAX_PREDICTION_AGE_MS) {
            invalidatePrediction(action.generation)
        }
    }

    @Suppress("unused")
    private val entitySpawnHandler = handler<PacketEvent> {
        when (val packet = it.packet) {
            // TODO: I guess this packet is merged into EntitySpawnS2CPacket
//            is ExperienceOrbSpawnS2CPacket -> highestId = max(packet.entityId, highestId)
            is ClientboundAddEntityPacket -> {
                recordHighestId(packet.id)
                val action = pendingPrediction
                if (action != null && packet.id in action.dispatchedIds && packet.type != EntityTypes.END_CRYSTAL) {
                    invalidatePrediction(action.generation)
                }
            }
            is ClientboundLoginPacket -> recordHighestId(packet.playerId)
            is ClientboundRemoveEntitiesPacket -> {
                val action = pendingPrediction
                if (action != null && packet.entityIds.any { it in action.dispatchedIds }) {
                    invalidatePrediction(action.generation)
                }
            }
        }
    }

    @Suppress("unused")
    val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

}
