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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import org.apache.commons.lang3.tuple.MutablePair

internal object NotifyWhenFail : ToggleableConfigurable(ModuleKillAura, "NotifyWhenFail", false) {

    val mode = choices(ModuleKillAura, "Mode", Box, arrayOf(Box, Sound))

    internal var failedHits = arrayListOf<MutablePair<Vec3d, Long>>()

    object Box : Choice("Box") {
        override val parent: ChoiceConfigurable
            get() = mode

        val fadeSeconds by int("FadeSeconds", 4, 1..10)

        val color by color("Color", Color4b(255, 179, 72, 255))
        val colorRainbow by boolean("Rainbow", false)
    }

    object Sound : Choice("Sound") {
        override val parent: ChoiceConfigurable
            get() = mode

        val volume by float("Volume", 50f, 0f..100f)
        val pitch by float("Pitch", 0.8f, 0f..2f)

    }

    private val boxFadeSeconds
        get() = 50 * Box.fadeSeconds

    fun notifyForFailedHit(entity: Entity, rotation: Rotation) {
        if (!NotifyWhenFail.enabled) {
            return
        }

        when (mode.activeChoice) {
            Box -> {
                val centerDistance = entity.box.center.subtract(player.eyes).length()
                val boxSpot = player.eyes.add(rotation.rotationVec.multiply(centerDistance))

                failedHits.add(MutablePair(boxSpot, 0L))
            }

            Sound -> {
                // Maybe a custom sound would be better
                val pitch = Sound.pitch

                world.playSound(player, player.x, player.y, player.z, SoundEvents.UI_BUTTON_CLICK.value(),
                    player.soundCategory, Sound.volume / 100f, pitch
                )
            }
        }
    }

    internal fun renderFailedHits(matrixStack: MatrixStack) {
        if (failedHits.isEmpty() || (!NotifyWhenFail.enabled || !Box.isActive)) {
            failedHits.clear()
            return
        }

        failedHits.forEach { it.setRight(it.getRight() + 1) }
        failedHits = failedHits.filter { it.right <= boxFadeSeconds } as ArrayList<MutablePair<Vec3d, Long>>

        val markedBlocks = failedHits

        val base = if (Box.colorRainbow) rainbow() else Box.color

        val box = Box(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

        renderEnvironmentForWorld(matrixStack) {
            for ((pos, opacity) in markedBlocks) {
                val vec3 = Vec3(pos)

                val fade = (255 + (0 - 255) * opacity.toDouble() / boxFadeSeconds.toDouble()).toInt()

                val baseColor = base.alpha(fade)
                val outlineColor = base.alpha(fade)

                withPosition(vec3) {
                    withColor(baseColor) {
                        drawSolidBox(box)
                    }

                    withColor(outlineColor) {
                        drawOutlinedBox(box)
                    }
                }
            }
        }
    }

}
