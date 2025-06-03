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

import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.mob.SlimeEntity
import net.minecraft.sound.SoundCategory
import net.minecraft.sound.SoundEvents
import java.util.*
import kotlin.random.Random

/**
 * Simulates scenarios where the player is training to hit a target.
 */
object MinaraiTrainer : ModuleDebugRecorder.DebugRecorderMode<TrainingData>("MinaraiTrainer") {

    private var isFirstRun = true

    private var target: LivingEntity? = null

    override fun enable() {
        isFirstRun = true
        super.enable()
    }

    override fun disable() {
        val target = target ?: return
        world.removeEntity(target.id, Entity.RemovalReason.DISCARDED)
        super.disable()
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        var previous = RotationManager.currentRotation ?: player.rotation

        target = spawn()
        if (isFirstRun) {
            // We wait until the player has hit the slime entity for the first time,
            // then we record the data and spawn a new slime entity.
            waitUntil { target == null }
            isFirstRun = false

            chat("✧ Starting training...")
        } else {
            waitUntil {
                val target = target ?: return@waitUntil true

                val next = RotationManager.currentRotation ?: player.rotation
                val current = RotationManager.previousRotation ?: player.lastRotation
                val previous = previous.apply {
                    previous = current
                }
                val distance = player.squaredBoxedDistanceTo(target).toFloat()

                recordPacket(TrainingData(
                    currentVector = current.directionVector,
                    previousVector = previous.directionVector,
                    targetVector = Rotation.lookingAt(point = target.box.center, from = player.eyePos).directionVector,
                    velocityDelta = current.rotationDeltaTo(next).toVec2f(),
                    playerDiff = player.pos.subtract(player.prevPos),
                    targetDiff = target.pos.subtract(target.prevPos),
                    age = target.age,
                    hurtTime = target.hurtTime,
                    distance = distance
                ))

                false
            }

            chat("✧ Recorded ${packets.size} samples")
        }
    }

    @Suppress("unused")
    private val attackEntity = handler<AttackEntityEvent> { event ->
        val attackEntity = event.entity
        val targetEntity = target ?: return@handler

        if (attackEntity == targetEntity) {
            world.removeEntity(targetEntity.id, Entity.RemovalReason.DISCARDED)
            target = null
            event.cancelEvent()
        }
    }

    /**
     * Spawns a slime entity about 2.0 - 3.0 blocks away from the player,
     * in a random direction and at a different height.
     */
    fun spawn(): LivingEntity {
        val slime = SlimeEntity(EntityType.SLIME, world)
        slime.uuid = UUID.randomUUID()

        val distance = Random.nextDouble() * 0.9 + 2.0

        // Spawn at least in view range of the player
        val direction = Rotation(
            player.yaw + Random.nextDouble(-65.0, 65.0).toFloat(),
            Random.nextDouble(-20.0, 10.0).toFloat()
        ).directionVector * distance

        val position = player.eyePos.add(direction)

        slime.setPosition(position)

        world.addEntity(slime)

        // Play sound at position
        world.playSound(
            position.x,
            position.y,
            position.z,
            SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
            SoundCategory.NEUTRAL,
            1f,
            1f,
            false
        )

        return slime
    }

}
