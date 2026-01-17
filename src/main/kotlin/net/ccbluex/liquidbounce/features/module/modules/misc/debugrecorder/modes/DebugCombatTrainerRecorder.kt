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

import net.ccbluex.liquidbounce.deeplearn.data.CombatSample
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleDebugRecorder
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.lastPos
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.network.protocol.game.ServerboundInteractPacket
import net.minecraft.sounds.SoundEvents
import net.minecraft.sounds.SoundSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.monster.Slime
import java.util.UUID
import kotlin.random.Random

/**
 * Simulates scenarios where the player is training to hit a target.
 */
object DebugCombatTrainerRecorder : ModuleDebugRecorder.DebugRecorderMode<CombatSample>("CombatTrainer") {

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
            tickUntil { target == null }
            isFirstRun = false

            chat("✧ Starting training...")
        } else {
            tickUntil {
                val target = target ?: return@tickUntil true

                val next = RotationManager.currentRotation ?: player.rotation
                val current = RotationManager.previousRotation ?: player.lastRotation
                val previous = previous.apply {
                    previous = current
                }
                val distance = player.squaredBoxedDistanceTo(target).toFloat()

                recordPacket(
                    CombatSample(
                        currentVector = current.directionVector,
                        previousVector = previous.directionVector,
                        targetVector = Rotation.lookingAt(
                            point = target.box.center,
                            from = player.eyePosition
                        ).directionVector,
                        velocityDelta = current.rotationDeltaTo(next).toVec2f(),
                        playerDiff = player.position().subtract(player.lastPos),
                        targetDiff = target.position().subtract(target.lastPos),
                        age = target.tickCount,
                        hurtTime = target.hurtTime,
                        distance = distance
                    )
                )

                false
            }

            chat("✧ Recorded ${packets.size} samples")
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ServerboundInteractPacket) {
            val targetEntity = target ?: return@handler

            if (packet.entityId == targetEntity.id) {
                world.removeEntity(targetEntity.id, Entity.RemovalReason.DISCARDED)
                target = null
                event.cancelEvent()
            }
        }
    }

    /**
     * Spawns a slime entity about 2.0 - 3.0 blocks away from the player,
     * in a random direction and at a different height.
     */
    fun spawn(): LivingEntity {
        val slime = Slime(EntityType.SLIME, world)
        slime.setUUID(UUID.randomUUID())

        val distance = Random.nextDouble() * 0.9 + 2.0

        // Spawn at least in view range of the player
        val direction = Rotation(
            player.yRot + Random.nextDouble(-65.0, 65.0).toFloat(),
            Random.nextDouble(-20.0, 10.0).toFloat()
        ).directionVector * distance

        val position = player.eyePosition.add(direction)

        slime.setPos(position)

        world.addEntity(slime)

        // Play sound at position
        world.playLocalSound(
            position.x,
            position.y,
            position.z,
            SoundEvents.EXPERIENCE_ORB_PICKUP,
            SoundSource.NEUTRAL,
            1f,
            1f,
            false,
        )

        return slime
    }

}
