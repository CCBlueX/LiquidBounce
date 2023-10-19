package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.QuickAccess.player
import net.ccbluex.liquidbounce.utils.client.enforced
import net.ccbluex.liquidbounce.utils.client.moveKeys
import net.ccbluex.liquidbounce.utils.client.opposite
import net.ccbluex.liquidbounce.utils.client.pressedOnKeyboard
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.client.option.KeyBinding
import kotlin.math.cos
import kotlin.math.sin

object ScaffoldZitterFeature {
    object Off : Choice("Off") {
        override val parent: ChoiceConfigurable
            get() = ModuleScaffold.zitterModes
    }

    object Teleport : Choice("Teleport") {
        override val parent: ChoiceConfigurable
            get() = ModuleScaffold.zitterModes

        private val speed by float("Speed", 0.13f, 0.1f..0.3f)
        private val strength by float("Strength", 0.05f, 0f..0.2f)
        private val groundOnly by boolean("GroundOnly", true)
        private var zitterDirection = false

        val repeatable =
            repeatable {
                if (player.isOnGround || !groundOnly) {
                    player.strafe(speed = speed.toDouble())
                    val yaw = Math.toRadians(player.yaw + if (zitterDirection) 90.0 else -90.0)
                    player.velocity.x -= sin(yaw) * strength
                    player.velocity.z += cos(yaw) * strength
                    zitterDirection = !zitterDirection
                }
            }
    }

    object Smooth : Choice("Smooth") {
        override val parent: ChoiceConfigurable
            get() = ModuleScaffold.zitterModes

        private val zitterDelay by int("Delay", 100, 0..500)
        private val groundOnly by boolean("GroundOnly", true)
        private val zitterTimer = Chronometer()
        private var zitterDirection = false

        val repeatable =
            repeatable {
                if (!player.isOnGround && groundOnly) {
                    return@repeatable
                }

                val pressedOnKeyboardKeys = moveKeys.filter { it.pressedOnKeyboard }

                when (pressedOnKeyboardKeys.size) {
                    0 -> {
                        moveKeys.forEach {
                            it.enforced = null
                        }
                    }

                    1 -> {
                        val key = pressedOnKeyboardKeys.first()
                        val possible = moveKeys.filter { it != key && it != key.opposite }
                        zitter(possible)
                        key.opposite!!.enforced = false
                        key.enforced = true
                    }

                    2 -> {
                        zitter(pressedOnKeyboardKeys)
                        moveKeys.filter { pressedOnKeyboardKeys.contains(it) }.forEach {
                            it.opposite!!.enforced = false
                        }
                    }
                }
                if (zitterTimer.hasElapsed(zitterDelay.toLong())) {
                    zitterDirection = !zitterDirection
                    zitterTimer.reset()
                }
            }

        private fun zitter(first: List<KeyBinding>) {
            if (zitterDirection) {
                first.first().enforced = true
                first.last().enforced = false
            } else {
                first.first().enforced = false
                first.last().enforced = true
            }
        }
    }
}
