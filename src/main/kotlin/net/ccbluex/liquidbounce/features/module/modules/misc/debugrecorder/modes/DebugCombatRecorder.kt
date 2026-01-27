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

package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import net.ccbluex.fastutil.mapToIntArray
import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.isBlockAction
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.raytracing.findEntityInCrosshair
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.AABB

/**
 * Records combat behavior
 */
object DebugCombatRecorder : ModuleDebugRecorder.DebugRecorderMode<CombatSample>("Combat") {

    private var targetTracker = tree(TargetTracker(
        // Start tracking target that we look at the closest
        defaultPriority = TargetPriority.DIRECTION,

        // Start tracking when 10 blocks away
        rangeValue =  FloatValueProvider("Range", 10f, 7f..12f)
    ))
    private var previous: Rotation = Rotation(0f, 0f)

    private val fightMap = Int2ObjectOpenHashMap<Fight>()
    private val trainingCollection = Int2ObjectOpenHashMap<MutableList<CombatSample>>()

    private var targetEntityId: Int? = null

    private class Fight {
        var ticks: Int = 0
    }

    private val doNotTrack
        get() = player.abilities.mayfly || player.isSpectator ||
            player.isDeadOrDying || player.abilities.flying

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (doNotTrack) {
            return@handler
        }

        if (interaction.isDestroying || player.isUsingItem && !player.isBlockAction) {
            reset()
            return@handler
        }

        val next = RotationManager.currentRotation ?: player.rotation
        val current = RotationManager.previousRotation ?: player.lastRotation
        val previous = previous.apply {
            previous = current
        }
        val targets = targetTracker.targets()

        for (target in targets) {
            val targetRotation = Rotation.lookingAt(point = target.eyePosition, from = player.eyePosition)

            if (targetEntityId != target.id) {
                // Check if we are moving towards the target
                if (next.angleTo(targetRotation) >= current.angleTo(targetRotation)) {
                    fightMap.remove(target.id)
                    trainingCollection.remove(target.id)
                    continue
                }
            }

            val fight = fightMap.computeIfAbsent(target.id) { Fight() }
            val buffer = trainingCollection.computeIfAbsent(target.id) { mutableListOf() }

            buffer.add(CombatSample(
                currentVector = current.directionVector,
                previousVector = previous.directionVector,
                targetVector = targetRotation.directionVector,
                velocityDelta = current.rotationDeltaTo(next).toVec2f(),
                playerDiff = player.position().subtract(player.lastPos),
                targetDiff = target.position().subtract(target.lastPos),
                age = fight.ticks,
                hurtTime = target.hurtTime,
                distance = player.squaredBoxedDistanceTo(target).toFloat()
            ))

            fight.ticks++
        }

        // Drop from [startingVector] and [trainingCollection] if target is not present anymore
        val targetIds = IntOpenHashSet(targets.mapToIntArray { it.id })

        fightMap.keys.retainAll(targetIds)
        trainingCollection.keys.retainAll(targetIds)
    }

    @Suppress("unused")
    private val attackHandler = sequenceHandler<AttackEntityEvent> { event ->
        val entity = event.entity as? LivingEntity ?: return@sequenceHandler
        val entityId = entity.id

        // Lock the sequence to prevent multiple recordings
        if (targetEntityId != null) {
            return@sequenceHandler
        }
        targetEntityId = entity.id

        // Wait until entity is not in combat
        var inactivity = 0
        var buffer: MutableList<CombatSample>? = null
        tickUntil {
            if (entity.isDeadOrDying || entity.isRemoved || doNotTrack) {
                return@tickUntil true
            }

            val rotation = RotationManager.currentRotation ?: player.rotation
            val distance = player.eyePosition.distanceTo(entity.eyePosition) + 1.0
            debugParameter("Distance") { distance }
            val raytraceTarget = findEntityInCrosshair(distance, rotation) { e ->
                e == entity
            }

            if (raytraceTarget?.entity == null) {
                inactivity++
                debugParameter("Inactivity") { inactivity }
                return@tickUntil inactivity > 20
            } else {
                buffer = trainingCollection[entityId]
                inactivity = 0
            }

            return@tickUntil false
        }

        targetEntityId = null
        trainingCollection.remove(entity.id)

        val sampleBuffer = buffer ?: return@sequenceHandler
        sampleBuffer.forEach(::recordPacket)
        chat("Recorded ${sampleBuffer.size} samples for ${entity.name.string}".asText())
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        renderEnvironmentForWorld(matrixStack) {
            startBatch()
            targetTracker.targets().forEach { entity ->
                val pos = entity.interpolateCurrentPosition(event.partialTicks)
                val eyePos = pos.add(0.0, entity.eyeHeight.toDouble(), 0.0)
                val box = AABB(
                    0.0,
                    entity.eyeHeight.toDouble(),
                    0.0,
                    0.0,
                    entity.eyeHeight.toDouble(),
                    0.0
                ).inflate(0.1)

                val color = if (targetEntityId == entity.id) {
                    Color4b.GREEN
                } else if (fightMap.contains(entity.id)) {
                    Color4b.YELLOW
                } else {
                    Color4b.RED
                }

                withPositionRelativeToCamera(pos) {
                    drawBox(
                        box,
                        color.with(a = 50),
                        color.with(a = 150)
                    )
                }
            }
            commitBatch()
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        when (packet) {
            is ServerboundUseItemOnPacket -> reset()
        }
    }

    override fun disable() {
        reset()
        super.disable()
    }

    private fun reset() {
        targetEntityId = null
        fightMap.clear()
        trainingCollection.clear()
    }

}
