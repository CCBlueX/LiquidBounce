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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiAFK.CustomMode.Rotate.angle
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiAFK.CustomMode.Rotate.ignoreOpenInventory
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiAFK.CustomMode.Rotate.rotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.enforced
import net.minecraft.client.option.KeyBinding
import net.minecraft.util.Hand
import org.apache.commons.lang3.RandomUtils
import kotlin.random.Random

/**
 * AntiAFK module
 *
 * Prevents you from being kicked for AFK.
 */

object ModuleAntiAFK : Module("AntiAFK", Category.PLAYER) {

    private val modes = choices(
        "Mode", RandomMode, arrayOf(
            OldMode, RandomMode, CustomMode
        )
    )

    private object OldMode : Choice("Old") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            mc.options.forwardKey.enforced = true
            wait { 10 }
            player.yaw += 180f
        }
    }

    private object RandomMode : Choice("Random") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {

            when (RandomUtils.nextInt(0, 6)) {
                0 -> {
                    if (player.isOnGround) {
                        player.jump()
                    }
                }

                1 -> {
                    if (!player.handSwinging) {
                        player.swingHand(Hand.MAIN_HAND)
                    }
                }

                2 -> {
                    val key = randomKeyBind()
                    key.enforced = true
                    wait { RandomUtils.nextInt(3, 7) }
                    key.enforced = false
                }

                3 -> {
                    player.inventory.selectedSlot = RandomUtils.nextInt(0, 9)
                }

                4 -> {
                    player.yaw += RandomUtils.nextFloat(0f, 360f) - 180f
                }

                5 -> {
                    player.pitch = (RandomUtils.nextFloat(0f, 10f) - 5f + player.pitch).coerceIn(-90f, 90f)
                }
            }
            wait { RandomUtils.nextInt(4, 7) }
        }
    }

    private fun randomKeyBind(): KeyBinding {
        return when (RandomUtils.nextInt(0, 3)) {
            0 -> mc.options.rightKey
            1 -> mc.options.leftKey
            2 -> mc.options.backKey
            else -> mc.options.forwardKey
        }
    }

    private object CustomMode : Choice("Custom") {
        override val parent: ChoiceConfigurable
            get() = modes

        private object Rotate : ToggleableConfigurable(ModuleAntiAFK, "Rotate", true) {
            val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)
            val rotationsConfigurable = tree(RotationsConfigurable())
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

        val swingRepeatable = repeatable {
            if (Swing.enabled && !player.handSwinging) {
                wait { Swing.delay }
                player.swingHand(Hand.MAIN_HAND)
            }
        }
        val repeatable = repeatable {
            if (move) {
                mc.options.forwardKey.isPressed = true
            }

            if (jump && player.isOnGround) {
                player.jump()
            }

            if (Rotate.enabled) {
                wait { Rotate.delay }
                val serverRotation = RotationManager.serverRotation
                val pitchRandomization = Random.nextDouble(-5.0, 5.0).toFloat()
                RotationManager.aimAt(
                    Rotation(
                        serverRotation.yaw + angle, (serverRotation.pitch + pitchRandomization).coerceIn(-90f, 90f)
                    ), ignoreOpenInventory, rotationsConfigurable
                )
            }

        }
    }
}
