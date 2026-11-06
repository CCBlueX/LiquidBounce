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
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.aiming.RotationTarget
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.processors.anglesmooth.FactorAngleSmooth
import net.ccbluex.liquidbounce.utils.kotlin.random
import net.minecraft.world.phys.Vec2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

class SmoothAngleSmooth(parent: ModeValueGroup<*>) : FactorAngleSmooth("Smooth", parent) {

    private val inMode by enumChoice("In", EasingType.LINEAR)
    private val outMode by enumChoice("Out", EasingType.LINEAR)

    private val horizontalTurnSpeed by floatRange("HorizontalTurnSpeed", 180f..180f,
        0.0f..180f)
    private val verticalTurnSpeed by floatRange("VerticalTurnSpeed", 180f..180f,
        0.0f..180f)

    override fun calculateFactors(
        rotationTarget: RotationTarget?,
        currentRotation: Rotation,
        targetRotation: Rotation
    ): Vec2 {
        val rotationDifference = currentRotation.rotationDeltaLengthTo(targetRotation).coerceAtMost(180f)

        val (horizontalTurnSpeed, verticalTurnSpeed) = if (rotationTarget != null) {
            horizontalTurnSpeed.random() to verticalTurnSpeed.random()
        } else {
            horizontalTurnSpeed.start to verticalTurnSpeed.start
        }

        val horizontalFactor = computeFactor(rotationDifference, horizontalTurnSpeed)
        val verticalFactor = computeFactor(rotationDifference, verticalTurnSpeed)

        return Vec2(horizontalFactor, verticalFactor)
    }

    private fun computeFactor(rotationDifference: Float, turnSpeed: Float): Float {
        val t = rotationDifference / 180f
        val factor = easeInOut(t)

        return factor * turnSpeed
    }

    private fun easeInOut(t: Float): Float {
        return if (t < 0.5f) {
            0.5f * inMode.easeIn(t * 2f)
        } else {
            0.5f + 0.5f * outMode.easeOut(t * 2f - 1f)
        }
    }

    enum class EasingType(override val tag: String) : Tagged {
        LINEAR("Linear") {
            override fun easeIn(t: Float) = t
        },
        EASE("Ease") {
            override fun easeIn(t: Float) = t * t * (3f - 2f * t)
        },
        STEP("Step") {
            override fun easeIn(t: Float) = if (t >= 1f) 1f else 0f
        },
        CUBIC("Cubic") {
            override fun easeIn(t: Float) = t * t * t
        },
        QUAD("Quad") {
            override fun easeIn(t: Float) = t * t
        },
        QUART("Quart") {
            override fun easeIn(t: Float) = t * t * t * t
        },
        QUINT("Quint") {
            override fun easeIn(t: Float) = t * t * t * t * t
        },
        POLY("Poly") {
            override fun easeIn(t: Float) = t.pow(6)
        },
        SINE("Sine") {
            override fun easeIn(t: Float) = 1f - cos(t * kotlin.math.PI.toFloat() / 2f)
        },
        CIRC("Circ") {
            override fun easeIn(t: Float) = 1f - sqrt(1f - t * t)
        },
        EXPO("Expo") {
            override fun easeIn(t: Float) = if (t == 0f) 0f else 2f.pow(10f * (t - 1f))
        },
        BACK("Back") {
            override fun easeIn(t: Float): Float {
                val c1 = 1.70158f
                val c3 = c1 + 1f
                return c3 * t * t * t - c1 * t * t
            }
        },
        BOUNCE("Bounce") {
            override fun easeIn(t: Float) = 1f - bounceOut(1f - t)

            private fun bounceOut(x: Float): Float {
                val n1 = 7.5625f
                val d1 = 2.75f
                return when {
                    x < 1f / d1 -> n1 * x * x
                    x < 2f / d1 -> n1 * (x - 1.5f / d1).pow(2) + 0.75f
                    x < 2.5f / d1 -> n1 * (x - 2.25f / d1).pow(2) + 0.9375f
                    else -> n1 * (x - 2.625f / d1).pow(2) + 0.984375f
                }
            }
        },
        ELASTIC("Elastic") {
            override fun easeIn(t: Float): Float {
                if (t == 0f) return 0f
                if (t == 1f) return 1f
                val c4 = (2f * kotlin.math.PI / 3f)
                return -2f.pow(10f * t - 10f) * sin((t * 10f - 10.75f) * c4)
            }
        },
        BEZIER("Bezier") {
            override fun easeIn(t: Float): Float {
                return cubicBezier(t, 0.25f, 0.1f, 0.25f, 1.0f)
            }
        },
        CIRCLE("Circle") {
            override fun easeIn(t: Float) = 1f - sqrt(1f - t * t)
        },
        SIN("Sin") {
            override fun easeIn(t: Float) = 1f - cos(t * kotlin.math.PI.toFloat() / 2f)
        };

        abstract fun easeIn(t: Float): Float

        fun easeOut(t: Float): Float {
            return 1f - easeIn(1f - t)
        }

        companion object {
            private fun cubicBezier(t: Float, p1x: Float, p1y: Float, p2x: Float, p2y: Float): Float {
                var guess = t
                for (i in 0 until 8) {
                    val x = 3 * (1 - guess).pow(2) * guess * p1x + 3 * (1 - guess) * guess.pow(2) * p2x + guess.pow(3) - t
                    val dx = 3 * (1 - guess).pow(2) * p1x + 6 * (1 - guess) * guess * (p2x - p1x) + 3 * guess.pow(2) * (1 - p2x)
                    if (abs(dx) < 1e-6f) break
                    guess -= x / dx
                }
                return 3 * (1 - guess).pow(2) * guess * p1y + 3 * (1 - guess) * guess.pow(2) * p2y + guess.pow(3)
            }
        }
    }

}
