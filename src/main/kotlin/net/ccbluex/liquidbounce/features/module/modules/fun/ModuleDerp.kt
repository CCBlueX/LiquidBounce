/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

/**
 * Derp module
 *
 * Makes it look as if you were derping around.
 */
object ModuleDerp : Module("Derp", Category.FUN) {


    private var yaw = 0f
    private var pitch = 0f

    val rotation: FloatArray
        get() {
            if (yawMode.activeChoice == YawStatic) {
                yaw = YawStatic.yawValue
            } else if (yawMode.activeChoice == YawOffset) {
                yaw = player.yaw + YawOffset.yawOffsetValue
            }
            if (pitchMode.activeChoice == PitchStatic) {
                pitch = if (safePitch) PitchStatic.pitchValue.coerceIn(-90f, 90f) else PitchStatic.pitchValue
            } else if (pitchMode.activeChoice == PitchOffset) {
                pitch = if (safePitch) (player.pitch + PitchOffset.pitchOffsetValue).coerceIn(
                    -90f,
                    90f
                ) else player.pitch + PitchOffset.pitchOffsetValue
            }
            return floatArrayOf(yaw, pitch)
        }


    private val yawMode = choices("Yaw", YawStatic, arrayOf(YawStatic, YawOffset, YawRandom, YawJitter, YawSpin))
    private val pitchMode =
        choices("Pitch", PitchStatic, arrayOf(PitchStatic, PitchOffset, PitchRandom))
    private val safePitch by boolean("SafePitch", true)

    private object YawStatic : Choice("Static") {
        override val parent: ChoiceConfigurable
            get() = yawMode
        val yawValue by float("Yaw", 0f, -180f..180f)

    }

    private object YawOffset : Choice("Offset") {
        override val parent: ChoiceConfigurable
            get() = yawMode
        val yawOffsetValue by float("Offset", 0f, -180f..180f)
    }

    private object YawRandom : Choice("Random") {
        override val parent: ChoiceConfigurable
            get() = yawMode
        val repeatable = repeatable {
            yaw = ((Math.random() * 360) + -180).toFloat()
            waitTicks(1)
        }
    }

    private object YawJitter : Choice("Jitter") {
        override val parent: ChoiceConfigurable
            get() = yawMode
        val yawForwardTicks by int("ForwardTicks", 5, 0..100)
        val yawBackwardTicks by int("BackwardTicks", 5, 0..100)

        val repeatable = repeatable {
            repeat(yawForwardTicks) {
                yaw = player.yaw
                waitTicks(1)
            }
            repeat(yawBackwardTicks) {
                yaw = player.yaw + 180
                waitTicks(1)
            }
        }
    }

    private object YawSpin : Choice("Spin") {
        override val parent: ChoiceConfigurable
            get() = yawMode
        val yawSpinSpeed by int("Speed", 1, -70..70)
        val repeatable = repeatable {
            yaw += yawSpinSpeed
            waitTicks(1)
        }
    }

    private object PitchStatic : Choice("Static") {
        override val parent: ChoiceConfigurable
            get() = pitchMode
        val pitchValue by float("Pitch", 0f, -180f..180f)
    }

    private object PitchOffset : Choice("Offset") {
        override val parent: ChoiceConfigurable
            get() = pitchMode
        val pitchOffsetValue by float("Offset", 0f, -180f..180f)
    }

    private object PitchRandom : Choice("Random") {
        override val parent: ChoiceConfigurable
            get() = pitchMode
        val repeatable = repeatable {
            pitch = if (safePitch) ((Math.random() * 180) - 90).toFloat() else ((Math.random() * 360) - 180).toFloat()
            waitTicks(1)
        }
    }

}
