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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import com.mojang.blaze3d.vertex.PoseStack
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.ObjectLongMutablePair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing.enabled
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraFailSwing.mode
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.entity.box
import net.minecraft.sounds.SoundEvents
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3

internal object KillAuraNotifyWhenFail {

    internal val failedHits = ObjectArrayList<ObjectLongMutablePair<Vec3>>()
    var failedHitsIncrement = 0

    object Box : Mode("Box") {
        override val parent: ModeValueGroup<Mode>
            get() = mode

        val fadeSeconds by int("Fade", 4, 1..10, "secs")

        val color by color("Color", Color4b(255, 179, 72, 255))
        val colorRainbow by boolean("Rainbow", false)
    }

    object Sound : Mode("Sound") {
        override val parent: ModeValueGroup<Mode>
            get() = mode

        val volume by float("Volume", 50f, 0f..100f)
        val pitch by float("Pitch", 0.8f, 0f..2f)

    }

    private val boxFadeSeconds
        get() = 50 * Box.fadeSeconds

    fun notifyForFailedHit(entity: Entity, rotation: Rotation) {
        failedHitsIncrement++

        when (mode.activeMode) {
            Box -> {
                val centerDistance = entity.box.center.subtract(player.eyePosition).length()
                val boxSpot = player.eyePosition.add(rotation.directionVector.scale(centerDistance))

                failedHits.add(ObjectLongMutablePair(boxSpot, 0L))
            }

            Sound -> {
                // Maybe a custom sound would be better
                val pitch = Sound.pitch

                world.playSound(player, player.x, player.y, player.z, SoundEvents.UI_BUTTON_CLICK.value(),
                    player.soundSource, Sound.volume / 100f, pitch
                )
            }
        }
    }

    internal fun renderFailedHits(matrixStack: PoseStack) {
        if (failedHits.isEmpty() || (!enabled || !Box.isSelected)) {
            failedHits.clear()
            return
        }

        failedHits.removeIf { pair ->
            val newValue = pair.valueLong() + 1L
            if (newValue >= boxFadeSeconds) {
                true
            } else {
                pair.value(newValue)
                false
            }
        }

        val markedBlocks = failedHits

        val base = if (Box.colorRainbow) rainbow() else Box.color

        renderEnvironmentForWorld(matrixStack) {
            startBatch()
            for ((pos, opacity) in markedBlocks) {
                val fade = (255 + (0 - 255) * opacity.toDouble() / boxFadeSeconds.toDouble()).toInt()

                val baseColor = base.with(a = fade)
                val outlineColor = base.with(a = fade)

                withPositionRelativeToCamera(pos) {
                    drawBox(
                        POINT_BOX,
                        baseColor,
                        outlineColor,
                    )
                }
            }
            commitBatch()
        }
    }

    private val POINT_BOX = AABB(0.0, 0.0, 0.0, 0.05, 0.05, 0.05)

}
