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
package net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.deeplearn.DeepLearningEngine
import net.ccbluex.liquidbounce.deeplearn.ModelHolster.models
import net.ccbluex.liquidbounce.deeplearn.data.TrainingData
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.AngleSmooth
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.NoneAngleSmooth
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.entity.lastRotation
import net.ccbluex.liquidbounce.utils.entity.prevPos
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import kotlin.time.DurationUnit
import kotlin.time.measureTimedValue

/**
 * Record using
 * - [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiCombatRecorder]
 * - [net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.MinaraiTrainer]
 * and then train a model - after that you will be able to use it with
 * [net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.impl.MinaraiAngleSmooth].
 */
class MinaraiAngleSmooth(
    parent: ChoiceConfigurable<*>,
    val fallback: AngleSmooth
) : AngleSmooth("Minarai", parent) {

    private val choices = choices("Model", 0) { local ->
        models.onChanged { _ ->
            local.choices = models.choices
        }

        models.choices.toTypedArray()
    }

    private class OutputMultiplier : Configurable("OutputMultiplier") {
        var yawMultiplier by float("Yaw", 1.5f, 0.5f..2f)
        var pitchMultiplier by float("Pitch", 1f, 0.5f..2f)
    }

    private var correctionMode = choices(this, "Correction") {
        arrayOf(
            /**
             * Works best with the model, as it allows for the most natural movement.
             */
            InterpolationAngleSmooth(it, 2..5, 2..5, 95..100),
            /**
             * Not recommended to use this one, as it completely eliminates any acceleration
             * effects from the model.
             */
            LinearAngleSmooth(it,
                horizontalTurnSpeed = 5f..5f,
                verticalTurnSpeed = 5f..5f
            ),
            NoneAngleSmooth(it)
        )
    }

    private val outputMultiplier = tree(OutputMultiplier())

    companion object {
        private const val UNSUPPORTED_NOTIFICATION_TIME = 5000L
        private val notificationChronometer = Chronometer()
    }

    override fun process(
        rotationTarget: RotationTarget,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Rotation {
        if (!DeepLearningEngine.isInitialized) {
            if (notificationChronometer.hasElapsed(UNSUPPORTED_NOTIFICATION_TIME)) {
                chat(markAsError(translation("liquidbounce.unsupportedDeepLearning")))
                chat(markAsError(translation(
                    "liquidbounce.rotationSystem.angleSmooth.minarai.fallback",
                    fallback.name
                )))
                notificationChronometer.reset()
            }

            return fallback.process(rotationTarget, currentRotation, targetRotation)
        }

        val entity = rotationTarget.entity as? LivingEntity
        val prevRotation = RotationManager.previousRotation ?: player.lastRotation
        val totalDelta = currentRotation.rotationDeltaTo(targetRotation)
        val velocityDelta = prevRotation.rotationDeltaTo(currentRotation)

        ModuleDebug.debugParameter(this, "DeltaYaw", totalDelta.deltaYaw)
        ModuleDebug.debugParameter(this, "DeltaPitch", totalDelta.deltaPitch)

        val input = TrainingData(
            currentVector = currentRotation.directionVector,
            previousVector = prevRotation.directionVector,
            targetVector = targetRotation.directionVector,
            velocityDelta = velocityDelta.toVec2f(),

            playerDiff = player.pos.subtract(player.prevPos),
            targetDiff = entity?.let { entity.pos.subtract(entity.prevPos) } ?: Vec3d.ZERO,

            hurtTime = entity?.let {entity.hurtTime } ?: 10,
            distance = entity?.let { player.squaredBoxedDistanceTo(entity).toFloat() } ?: 3f,
            age = 0
        )

        val (output, time) = measureTimedValue {
            choices.activeChoice.predict(input.asInput)
        }
        ModuleDebug.debugParameter(this, "Output [0]", output[0])
        ModuleDebug.debugParameter(this, "Output [1]", output[1])
        ModuleDebug.debugParameter(this, "Time", "${time.toString(DurationUnit.MILLISECONDS, 2)} ms")

        val modelOutput = Rotation(
            currentRotation.yaw + output[0] * outputMultiplier.yawMultiplier,
            currentRotation.pitch + output[1] * outputMultiplier.pitchMultiplier
        )

        return correctionMode.activeChoice.process(
            rotationTarget,
            modelOutput,
            targetRotation
        )
    }

    override fun calculateTicks(
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Int {
        // TODO: Implement correctly
        return 0
    }

}
