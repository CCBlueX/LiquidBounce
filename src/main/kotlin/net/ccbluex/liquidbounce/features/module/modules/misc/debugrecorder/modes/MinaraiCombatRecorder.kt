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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.render.BoxRenderer
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.FloatValueProvider
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.combat.TargetPriority
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.kotlin.mapIntSet
import net.minecraft.entity.LivingEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.util.math.Box

/**
 * Records combat behavior
 */
object MinaraiCombatRecorder : ModuleDebugRecorder.DebugRecorderMode<TrainingData>("MinaraiCombat") {

    private var targetTracker = tree(TargetTracker(
        // Start tracking target that we look at the closest
        defaultPriority = TargetPriority.DIRECTION,

        // Start tracking when 10 blocks away
        rangeValue =  FloatValueProvider("Range", 10f, 7f..12f)
    ))
    private var previous: Rotation = Rotation(0f, 0f)

    private val fightMap = Int2ObjectOpenHashMap<Fight>()
    private val trainingCollection = Int2ObjectOpenHashMap<MutableList<TrainingData>>()

    private var targetEntityId: Int? = null

    private data class Fight(
        var ticks: Int = 0
    )

    private inline fun <V> Int2ObjectOpenHashMap<V>.getOrPut(key: Int, valueProvider: () -> V): V {
        return get(key) ?: valueProvider().also { put(key, it) }
    }

    private val doNotTrack
        get() = player.abilities.allowFlying || player.isSpectator ||
            player.isDead || player.abilities.flying

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (doNotTrack) {
            return@tickHandler
        }

        if (interaction.isBreakingBlock || player.isUsingItem && !player.isBlockAction) {
            reset()
            return@tickHandler
        }

        val next = RotationManager.currentRotation ?: player.rotation
        val current = RotationManager.previousRotation ?: player.lastRotation
        val previous = previous.apply {
            previous = current
        }
        val targets = targetTracker.targets()

        for (target in targets) {
            val targetRotation = Rotation.lookingAt(point = target.eyePos, from = player.eyePos)

            if (targetEntityId != target.id) {
                // Check if we are moving towards the target
                if (next.angleTo(targetRotation) >= current.angleTo(targetRotation)) {
                    fightMap.remove(target.id)
                    trainingCollection.remove(target.id)
                    continue
                }
            }

            val fight = fightMap.getOrPut(target.id, ::Fight)
            val buffer = trainingCollection.getOrPut(target.id, ::ArrayList)

            buffer.add(TrainingData(
                currentVector = current.directionVector,
                previousVector = previous.directionVector,
                targetVector = targetRotation.directionVector,
                velocityDelta = current.rotationDeltaTo(next).toVec2f(),
                playerDiff = player.pos.subtract(player.prevPos),
                targetDiff = target.pos.subtract(target.prevPos),
                age = fight.ticks,
                hurtTime = target.hurtTime,
                distance = player.squaredBoxedDistanceTo(target).toFloat()
            ))

            fight.ticks++
        }

        // Drop from [startingVector] and [trainingCollection] if target is not present anymore
        val targetIds = targets.mapIntSet { it.id }

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
        var buffer: MutableList<TrainingData>? = null
        waitUntil {
            if (entity.isDead || entity.isRemoved || doNotTrack) {
                return@waitUntil true
            }

            val rotation = RotationManager.currentRotation ?: player.rotation
            val distance = player.eyePos.distanceTo(entity.eyePos) + 1.0
            ModuleDebug.debugParameter(this, "Distance", distance)
            val raytraceTarget = raytraceEntity(distance, rotation) { e ->
                e == entity
            }

            if (raytraceTarget?.entity == null) {
                inactivity++
                ModuleDebug.debugParameter(this, "Inactivity", inactivity)
                return@waitUntil inactivity > 20
            } else {
                buffer = trainingCollection[entityId]
                inactivity = 0
            }

            return@waitUntil false
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
            BoxRenderer.drawWith(this) {
                targetTracker.targets().forEach { entity ->
                    val pos = entity.interpolateCurrentPosition(event.partialTicks)
                    val eyePos = pos.add(0.0, entity.standingEyeHeight.toDouble(), 0.0)
                    val box = Box(
                        0.0,
                        entity.standingEyeHeight.toDouble(),
                        0.0,
                        0.0,
                        entity.standingEyeHeight.toDouble(),
                        0.0
                    ).expand(0.1)

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
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        when (packet) {
            is PlayerInteractBlockC2SPacket -> reset()
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
