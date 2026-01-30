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
package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.random

/**
 * Derp module
 *
 * Makes it look as if you were derping around.
 */
object ModuleDerp : ClientModule("Derp", ModuleCategories.FUN) {

    private val yawMode = choices("Yaw", YawRandom,
        arrayOf(YawStatic, YawOffset, YawRandom, YawJitter, YawSpin))
    private val pitchMode = choices("Pitch", PitchRandom,
        arrayOf(PitchStatic, PitchOffset, PitchRandom))
    private val safePitch by boolean("SafePitch", true)
    private val notDuringSprint by boolean("NotDuringSprint", true)

    // DO NOT USE TREE TO MAKE SURE THAT THE ROTATIONS ARE NOT CHANGED
    private val rotations = RotationsValueGroup(this)

    val repeatable = tickHandler {
        if (notDuringSprint && (mc.options.keySprint.isDown || player.isSprinting)) {
            return@tickHandler
        }

        val yaw = yawMode.activeMode.yaw
        val pitch = pitchMode.activeMode.pitch.let {
            if (safePitch) {
                it.coerceIn(-90f..90f)
            } else {
                it
            }
        }

        RotationManager.setRotationTarget(rotations.toRotationTarget(Rotation(yaw, pitch)),
            Priority.NOT_IMPORTANT, this@ModuleDerp)
    }

    private object YawStatic : YawMode("Static") {

        val yawValue by float("Yaw", 0f, -180f..180f, "°")

        override val yaw: Float
            get() = yawValue

    }

    private object YawOffset : YawMode("Offset") {

        val yawOffsetValue by float("Offset", 0f, -180f..180f, "°")

        override val yaw: Float
            get() = player.yRot + yawOffsetValue

    }

    private object YawRandom : YawMode("Random") {
        override val yaw: Float
            get() = (-180f..180f).random()

    }

    private object YawJitter : YawMode("Jitter") {

        override var yaw = 0.0f

        val yawForwardTicks by int("ForwardTicks", 2, 0..100, "ticks")
        val yawBackwardTicks by int("BackwardTicks", 2, 0..100, "ticks")

        @Suppress("unused")
        val repeatable = tickHandler {
            repeat(yawForwardTicks) {
                yaw = player.yRot
                waitTicks(1)
            }

            repeat(yawBackwardTicks) {
                yaw = player.yRot + 180
                waitTicks(1)
            }
        }

    }

    private object YawSpin : YawMode("Spin") {

        override var yaw = 0.0f

        val yawSpinSpeed by int("Speed", 50, -70..70, "°/tick")

        @Suppress("unused")
        val repeatable = tickHandler {
            yaw += yawSpinSpeed
            waitTicks(1)
        }

    }

    private object PitchStatic : PitchMode("Static") {

        override val pitch: Float
            get() = pitchValue

        val pitchValue by float("Pitch", -90f, -180f..180f, "°")

    }

    private object PitchOffset : PitchMode("Offset") {

        override val pitch: Float
            get() = player.xRot + pitchOffsetValue

        val pitchOffsetValue by float("Offset", 0f, -180f..180f, "°")

    }

    private object PitchRandom : PitchMode("Random") {

        override val parent: ModeValueGroup<*>
            get() = pitchMode

        override val pitch: Float
            get() = if (safePitch) (-90f..90f).random() else (-180f..180f).random()

    }

    abstract class YawMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = yawMode
        abstract val yaw: Float
    }

    abstract class PitchMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
            get() = pitchMode
        abstract val pitch: Float
    }



}
