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
package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.combat.CombatController
import net.ccbluex.liquidbounce.deeplearn.combat.CombatModels
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.AngleSmooth
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.minecraft.world.entity.LivingEntity
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sign

/**
 * Aims the way the model decides. [fallback] aims whenever it does not: without a model, while the fight
 * history fills up, and on the way back to the camera.
 */
private const val VERTICAL_TOLERANCE = 8f

class AiAngleSmooth(
    parent: ModeValueGroup<*>,
    private val fallback: AngleSmooth,
) : AngleSmooth("AI", parent, listOf("Minarai")) {
    private val speed by float("Speed", 1f, 0.5f..1.5f)
    private val maxTurn by float("MaxTurn", 60f, 10f..180f)
    private val randomness by float("Randomness", 0.5f, 0f..1f)
    private val prediction by int("Prediction", 2, 0..4, "ticks")

    /**
     * Share of the vertical miss closed per tick beyond [VERTICAL_TOLERANCE]. The model learned from 1.9+ fights,
     * where nobody follows an opponent knocked high into the air; 1.8 combos need it.
     */
    private val verticalAssist by float("VerticalAssist", 0f, 0f..1f)
    private var lastSpeed = 1f
    private var fellBack = true
    private var notified: String? = null

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        val live = (rotationTarget.entity as? LivingEntity)?.let { entity ->
            CombatController.randomness = randomness
            CombatController.lead = prediction
            CombatController.decide(entity, currentRotation)
        }
        if (live == null || !live.heads.aim) {
            val style = CombatController.style
            // Without a decision the history may just be filling up, which is only worth a message if it never ends
            if (live != null || !DeepLearningEngine.isInitialized || !CombatModels.available(style)) {
                notify(CombatModels.describe(style))
            }
            fellBack = true
            return fallback.process(rotationTarget, currentRotation, targetRotation)
        }
        notified = null
        fellBack = false
        val yaw = (live.decision.yaw * speed).coerceIn(-maxTurn, maxTurn)
        var pitch = live.decision.pitch * speed
        val missed = targetRotation.pitch - (currentRotation.pitch + pitch)
        if (abs(missed) > VERTICAL_TOLERANCE) {
            pitch += (missed - sign(missed) * VERTICAL_TOLERANCE) * verticalAssist
        }
        pitch = pitch.coerceIn(-maxTurn, maxTurn)
        lastSpeed = max(abs(yaw), abs(pitch)).coerceAtLeast(1f)
        return Rotation(currentRotation.yaw + yaw, (currentRotation.pitch + pitch).coerceIn(-90f, 90f))
    }

    override fun calculateTicks(currentRotation: Rotation, targetRotation: Rotation): Int {
        if (fellBack) {
            return fallback.calculateTicks(currentRotation, targetRotation)
        }
        val delta = currentRotation.rotationDeltaTo(targetRotation)
        return ceil(max(abs(delta.deltaYaw), abs(delta.deltaPitch)) / lastSpeed).toInt().coerceAtLeast(1)
    }

    private fun notify(status: String) {
        if (notified != status) {
            notified = status
            chat(markAsError(ModuleKillAura.message("aiNotReady", status, fallback.name)))
        }
    }
}
