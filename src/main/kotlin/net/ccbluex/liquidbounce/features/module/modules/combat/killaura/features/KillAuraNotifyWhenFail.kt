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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing.enabled
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing.mode
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.kotlin.component1
import net.ccbluex.liquidbounce.utils.kotlin.component2
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.sound.SoundEvents
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

internal object KillAuraNotifyWhenFail {

    internal val failedHits = ArrayDeque<ObjectLongMutablePair<Vec3d>>()
    var failedHitsIncrement = 0

    object Box : Choice("Box") {
        override val parent: ChoiceConfigurable<Choice>
            get() = mode

        val fadeSeconds by int("Fade", 4, 1..10, "secs")

        val color by color("Color", Color4b(255, 179, 72, 255))
        val colorRainbow by boolean("Rainbow", false)
    }

    object Sound : Choice("Sound") {
        override val parent: ChoiceConfigurable<Choice>
            get() = mode

        val volume by float("Volume", 50f, 0f..100f)
        val pitch by float("Pitch", 0.8f, 0f..2f)

    }

    private val boxFadeSeconds
        get() = 50 * Box.fadeSeconds

    fun notifyForFailedHit(entity: Entity, rotation: Rotation) {
        failedHitsIncrement++

        when (mode.activeChoice) {
            Box -> {
                val centerDistance = entity.box.center.subtract(player.eyePos).length()
                val boxSpot = player.eyePos.add(rotation.directionVector.multiply(centerDistance))

                failedHits.add(ObjectLongMutablePair(boxSpot, 0L))
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
        if (failedHits.isEmpty() || (!enabled || !Box.isSelected)) {
            failedHits.clear()
            return
        }

        with(failedHits.iterator()) {
            while (hasNext()) {
                val pair = next()
                val newValue = pair.valueLong() + 1L
                if (newValue >= boxFadeSeconds) {
                    remove()
                    continue
                }
                pair.value(newValue)
            }
        }

        val markedBlocks = failedHits

        val base = if (Box.colorRainbow) rainbow() else Box.color

        val box = Box(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

        renderEnvironmentForWorld(matrixStack) {
            for ((pos, opacity) in markedBlocks) {
                val fade = (255 + (0 - 255) * opacity.toDouble() / boxFadeSeconds.toDouble()).toInt()

                val baseColor = base.with(a = fade)
                val outlineColor = base.with(a = fade)

                withPositionRelativeToCamera(pos) {
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
