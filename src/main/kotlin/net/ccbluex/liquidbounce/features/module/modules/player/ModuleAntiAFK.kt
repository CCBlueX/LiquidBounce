/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiAFK.Custom.Rotate.angle
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.client.option.KeyBinding
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Hand
import org.apache.commons.lang3.RandomUtils

/**
 * AntiAFK module
 *
 * Prevents you from being kicked for AFK.
 */

object ModuleAntiAFK : Module("AntiAFK", Category.PLAYER) {

    private val modes = choices(
        "Mode",
        Random,
        arrayOf(
            Old,
            Random,
            Custom
        )
    )

    private object Old : Choice("Old") {

        override val parent: ChoiceConfigurable
            get() = modes

        override fun disable() {
            if (!InputUtil.isKeyPressed(mc.window.handle, mc.options.keyForward.boundKey.code)) {
                mc.options.keyForward.isPressed = false
            }
        }

        val repeatable = repeatable {
            mc.options.keyForward.isPressed = true
            wait(10)
            player.yaw += 180f
        }
    }

    private object Random : Choice("Random") {

        override val parent: ChoiceConfigurable
            get() = modes

        var delay = 500L
        var shouldMove = false
        val timer = Chronometer()

        override fun disable() {
            mc.options.keyRight.isPressed = false
            mc.options.keyLeft.isPressed = false
            mc.options.keyBack.isPressed = false
            mc.options.keyForward.isPressed = false
        }

        val repeatable = repeatable {
            randomKeyBind()!!.isPressed = shouldMove

            if (!timer.hasElapsed(delay)) {
                return@repeatable
            }

            shouldMove = false
            delay = 500L

            when (RandomUtils.nextInt(0, 6)) {
                0 -> {
                    if (player.isOnGround) {
                        player.jump()
                    }
                    timer.reset()
                }
                1 -> {
                    if (!player.handSwinging) {
                        player.swingHand(Hand.MAIN_HAND)
                    }
                    timer.reset()
                }
                2 -> {
                    delay = RandomUtils.nextInt(0, 1000).toLong()
                    shouldMove = true
                    timer.reset()
                }
                3 -> {
                    player.inventory.selectedSlot = RandomUtils.nextInt(0, 9)
                    timer.reset()
                }
                4 -> {
                    player.yaw += RandomUtils.nextFloat(-180f, 180f)
                    timer.reset()
                }
                5 -> {
                    if (player.pitch <= -90f || player.pitch >= 90) {
                        player.pitch = 0f
                    }
                    player.pitch += RandomUtils.nextFloat(-10f, 10f)
                    timer.reset()
                }
            }
        }
    }

    private fun randomKeyBind(): KeyBinding? {
        when (RandomUtils.nextInt(0, 4)) {
            0 -> {
                return mc.options.keyRight
            }
            1 -> {
                return mc.options.keyLeft
            }
            2 -> {
                return mc.options.keyBack
            }
            3 -> {
                return mc.options.keyForward
            }
            else -> {
                return null
            }
        }
    }

    private object Custom : Choice("Custom") {
        override val parent: ChoiceConfigurable
            get() = modes

        private object Rotate : ToggleableConfigurable(ModuleAntiAFK, "Rotate", true) {
            val delay by int("Delay", 5, 0..20)
            val angle by float("Angle", 1f, -180f..180f)
        }

        private object Swing : ToggleableConfigurable(ModuleAntiAFK, "Swing", true) {
            val delay by int("Delay", 5, 0..20)
        }

        init {
            tree(Rotate)
            tree(Swing)
        }

        val jump by boolean("Jump", true)
        val move by boolean("Move", true)

        override fun disable() {
            if (!InputUtil.isKeyPressed(mc.window.handle, mc.options.keyForward.boundKey.code)) {
                mc.options.keyForward.isPressed = false
            }
        }

        val repeatable = repeatable {
            if (move) {
                mc.options.keyForward.isPressed = true
            }

            if (jump && player.isOnGround) {
                player.jump()
            }

            if (Rotate.enabled) {
                wait(Rotate.delay)
                player.yaw += angle
                if (player.pitch <= -90f || player.pitch >= 90) {
                    player.pitch = 0f
                }
                player.pitch += RandomUtils.nextFloat(0f, 1f) * 2 - 1
            }

            if (Swing.enabled && !player.handSwinging) {
                wait(Swing.delay)
                player.swingHand(Hand.MAIN_HAND)
            }
        }
    }
}
