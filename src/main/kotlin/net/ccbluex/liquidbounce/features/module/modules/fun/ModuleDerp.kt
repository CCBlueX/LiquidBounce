/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.utils.aiming.data.Orientation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationEngine
import net.ccbluex.liquidbounce.utils.aiming.tracking.RotationTracker
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random

/**
 * Derp module
 *
 * Makes it look as if you were derping around.
 */
object ModuleDerp : Module("Derp", Category.FUN) {

    private val yawMode = choices("Yaw", YawSpin,
        arrayOf(YawStatic, YawOffset, YawRandom, YawJitter, YawSpin))
    private val pitchMode = choices("Pitch", PitchStatic,
        arrayOf(PitchStatic, PitchOffset, PitchRandom))
    private val safePitch by boolean("SafePitch", true)
    private val notDuringSprint by boolean("NotDuringSprint", true)

    // DO NOT USE TREE TO MAKE SURE THAT THE ROTATIONS ARE NOT CHANGED
    private val rotationEngine = RotationEngine(this)

    val repeatable = repeatable {
        if (notDuringSprint && (mc.options.sprintKey.isPressed || player.isSprinting)) {
            return@repeatable
        }

        val yaw = (yawMode.activeChoice as YawChoice).yaw
        val pitch = (pitchMode.activeChoice as PitchChoice).pitch.let {
            if (safePitch) {
                it.coerceIn(-90f..90f)
            } else {
                it
            }
        }

        RotationManager.aimAt(RotationTracker.withFixedAngle(rotationEngine, Orientation(yaw, pitch)), Priority.NOT_IMPORTANT,
            this@ModuleDerp)
    }

    private object YawStatic : YawChoice("Static") {

        val yawValue by float("Yaw", 0f, -180f..180f, "°")

        override val yaw: Float
            get() = yawValue

    }

    private object YawOffset : YawChoice("Offset") {

        val yawOffsetValue by float("Offset", 0f, -180f..180f, "°")

        override val yaw: Float
            get() = player.yaw + yawOffsetValue

    }

    private object YawRandom : YawChoice("Random") {
        override val yaw: Float
            get() = (-180f..180f).random().toFloat()

    }

    private object YawJitter : YawChoice("Jitter") {

        override var yaw = 0.0f

        val yawForwardTicks by int("ForwardTicks", 2, 0..100, "ticks")
        val yawBackwardTicks by int("BackwardTicks", 2, 0..100, "ticks")

        @Suppress("unused")
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

    private object YawSpin : YawChoice("Spin") {

        override var yaw = 0.0f

        val yawSpinSpeed by int("Speed", 50, -70..70, "°/tick")

        @Suppress("unused")
        val repeatable = repeatable {
            yaw += yawSpinSpeed
            waitTicks(1)
        }

    }

    private object PitchStatic : PitchChoice("Static") {

        override val pitch: Float
            get() = pitchValue

        val pitchValue by float("Pitch", -90f, -180f..180f, "°")

    }

    private object PitchOffset : PitchChoice("Offset") {

        override val pitch: Float
            get() = player.pitch + pitchOffsetValue

        val pitchOffsetValue by float("Offset", 0f, -180f..180f, "°")

    }

    private object PitchRandom : PitchChoice("Random") {

        override val parent: ChoiceConfigurable<*>
            get() = pitchMode

        override val pitch: Float
            get() = if (safePitch) (-90..90).random().toFloat() else (-180..180).random().toFloat()

    }

    abstract class YawChoice(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = yawMode
        abstract val yaw: Float
    }

    abstract class PitchChoice(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = pitchMode
        abstract val pitch: Float
    }



}
