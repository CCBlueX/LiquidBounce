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
 */

package net.ccbluex.liquidbounce.utils.aiming.point.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.Vec3d
import java.security.SecureRandom
import kotlin.math.abs

internal class Gaussian(parent: EventListener) : ToggleableConfigurable(parent, "Gaussian", false) {

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

    var currentOffset: Vec3d = Vec3d.ZERO
    private var targetOffset: Vec3d = Vec3d.ZERO

    val yawFactor by floatRange("YawOffset", 0f..0f, 0.0f..1.0f)
    val pitchFactor by floatRange("PitchOffset", 0f..0f, 0.0f..1.0f)
    val chance by int("Chance", 100, 0..100, "%")
    val speed by floatRange("Speed", 0.1f..0.2f, 0.01f..1f)
    val tolerance by float("Tolerance", 0.05f, 0.01f..0.1f)

    private inner class Dynamic : ToggleableConfigurable(this, "Dynamic", false) {
        val hurtTime by int("HurtTime", 10, 0..10)
        val yawFactor by float("YawFactor", 0f, 0f..10f, "x")
        val pitchFactor by float("PitchFactor", 0f, 0f..10f, "x")
        val speed by floatRange("Speed", 0.5f..0.75f, 0.01f..1f)
        val tolerance by float("Tolerance", 0.1f, 0.01f..0.1f)
    }

    private val dynamic = tree(Dynamic())

    private fun interpolate(start: Double, end: Double, f: Double) = start + (end - start) * f

    fun factorCheck(): Boolean {
        return yawFactor.random() > 0.0f && pitchFactor.random() > 0.0f && chance > 0
    }

    private fun gaussianHasReachedTarget(vec1: Vec3d, vec2: Vec3d, tolerance: Float): Boolean {
        return abs(vec1.x - vec2.x) < tolerance &&
            abs(vec1.y - vec2.y) < tolerance &&
            abs(vec1.z - vec2.z) < tolerance
    }

    @Suppress("CognitiveComplexMethod")
    fun updateGaussianOffset(entity: Any?) {
        val dynamicCheck = dynamic.enabled && entity is LivingEntity && entity.hurtTime >= dynamic.hurtTime

        val yawFactor =
            if (dynamicCheck && dynamic.yawFactor > 0f) {
                (yawFactor.random() + player.sqrtSpeed * dynamic.yawFactor)
            } else {
                yawFactor.random()
            }.toDouble()

        val pitchFactor =
            if (dynamicCheck && dynamic.pitchFactor > 0f) {
                (pitchFactor.random() + player.sqrtSpeed * dynamic.pitchFactor)
            } else {
                pitchFactor.random()
            }.toDouble()

        if (gaussianHasReachedTarget(
                currentOffset,
                targetOffset,
                if (dynamicCheck) dynamic.tolerance else tolerance
            )
        ) {
            if (random.nextInt(100) <= chance) {
                targetOffset = Vec3d(
                    random.nextGaussian(MEAN_X, STDDEV_X) * yawFactor,
                    random.nextGaussian(MEAN_Y, STDDEV_Y) * pitchFactor,
                    random.nextGaussian(MEAN_Z, STDDEV_Z) * yawFactor
                )
            }
        } else {
            currentOffset = Vec3d(
                interpolate(
                    currentOffset.x,
                    targetOffset.x,
                    if (dynamicCheck) dynamic.speed.random().toDouble() else speed.random().toDouble()
                ),
                interpolate(
                    currentOffset.y,
                    targetOffset.y,
                    if (dynamicCheck) dynamic.speed.random().toDouble() else speed.random().toDouble()
                ),
                interpolate(
                    currentOffset.z,
                    targetOffset.z,
                    if (dynamicCheck) dynamic.speed.random().toDouble() else speed.random().toDouble()
                )
            )
        }
    }

}
