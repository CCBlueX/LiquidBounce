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

package net.ccbluex.liquidbounce.utils.aiming.point.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.aiming.point.PointInsideBox
import net.ccbluex.liquidbounce.utils.entity.horizontalSpeed
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.ccbluex.liquidbounce.utils.math.equals
import net.minecraft.util.Mth.lerp
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.Vec3
import java.security.SecureRandom

internal class PointProcessorGaussian(parent: EventListener) : PointProcessor(parent, "Gaussian", false) {

    companion object {

        /**
         * The gaussian distribution values for the offset.
         */
        private const val STDDEV_Z = 0.24453708645460387
        private const val MEAN_X = 0.00942273861037109
        private const val STDDEV_X = 0.23319837528201348
        private const val MEAN_Y = -0.30075078007595923
        private const val STDDEV_Y = 0.3492437109081718
        private const val MEAN_Z = 0.013282929419023442

        private val random = SecureRandom()

    }

    private var currentOffset: Vec3 = Vec3.ZERO
    private var targetOffset: Vec3 = Vec3.ZERO

    private val yawFactor by floatRange("YawOffset", 0f..0f, 0.0f..1.0f)
    private val pitchFactor by floatRange("PitchOffset", 0f..0f, 0.0f..1.0f)
    private val chance by int("Chance", 100, 0..100, "%")
    private val speed by floatRange("Speed", 0.1f..0.2f, 0.01f..1f)
    private val tolerance by float("Tolerance", 0.05f, 0.01f..0.1f)

    private inner class Dynamic : ToggleableValueGroup(this, "Dynamic", false) {
        val hurtTime by int("HurtTime", 10, 0..10)
        val yawFactor by float("YawFactor", 0f, 0f..10f, "x")
        val pitchFactor by float("PitchFactor", 0f, 0f..10f, "x")
        val speed by floatRange("Speed", 0.5f..0.75f, 0.01f..1f)
        val tolerance by float("Tolerance", 0.1f, 0.01f..0.1f)
    }

    private val dynamic = tree(Dynamic())

    @Suppress("CognitiveComplexMethod")
    fun updateGaussianOffset(entity: Any?) {
        val dynamicCheck = dynamic.enabled && entity is LivingEntity && entity.hurtTime >= dynamic.hurtTime

        val yawFactor =
            if (dynamicCheck && dynamic.yawFactor > 0f) {
                (yawFactor.random() + player.horizontalSpeed * dynamic.yawFactor)
            } else {
                yawFactor.random()
            }.toDouble()

        val pitchFactor =
            if (dynamicCheck && dynamic.pitchFactor > 0f) {
                (pitchFactor.random() + player.horizontalSpeed * dynamic.pitchFactor)
            } else {
                pitchFactor.random()
            }.toDouble()

        if (currentOffset.equals(
                targetOffset,
                if (dynamicCheck) dynamic.tolerance.toDouble() else tolerance.toDouble()
            )
        ) {
            if (random.nextInt(100) <= chance) {
                targetOffset = Vec3(
                    random.nextGaussian(MEAN_X, STDDEV_X) * yawFactor,
                    random.nextGaussian(MEAN_Y, STDDEV_Y) * pitchFactor,
                    random.nextGaussian(MEAN_Z, STDDEV_Z) * yawFactor
                )
            }
        } else {
            currentOffset = Vec3(
                lerp(
                    if (dynamicCheck) dynamic.speed.random().toDouble() else speed.random().toDouble(),
                    currentOffset.x,
                    targetOffset.x
                ),
                lerp(
                    if (dynamicCheck) dynamic.speed.random().toDouble() else speed.random().toDouble(),
                    currentOffset.y,
                    targetOffset.y
                ),
                lerp(
                    if (dynamicCheck) dynamic.speed.random().toDouble() else speed.random().toDouble(),
                    currentOffset.z,
                    targetOffset.z
                )
            )
        }
    }

    override fun process(point: PointInsideBox): PointInsideBox {
        if (yawFactor.random() > 0.0f && pitchFactor.random() > 0.0f && chance > 0) {
            updateGaussianOffset(point)
        }

        return point + currentOffset
    }

}
