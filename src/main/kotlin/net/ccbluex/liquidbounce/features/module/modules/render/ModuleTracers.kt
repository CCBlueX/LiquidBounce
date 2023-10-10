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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import java.awt.Color
import kotlin.math.sqrt

/**
 * Tracers module
 *
 * Draws a line to every entity a certain radius.
 */

object ModuleTracers : Module("Tracers", Category.RENDER) {

    private val modes = choices(
        "ColorMode",
        DistanceColor,
        arrayOf(
            DistanceColor,
            StaticColor,
            RainbowColor
        )
    )

    private object DistanceColor : Choice("Distance") {

        override val parent: ChoiceConfigurable
            get() = modes

        val useViewDistance by boolean("UseViewDistance", true)
        val customViewDistance by float("CustomViewDistance", 128.0F, 1.0F..512.0F)
    }

    private object StaticColor : Choice("Static") {

        override val parent: ChoiceConfigurable
            get() = modes

        val color by color("Color", Color4b(0, 160, 255, 255))
    }

    private object RainbowColor : Choice("Rainbow") {
        override val parent: ChoiceConfigurable
            get() = modes
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        val useDistanceColor = DistanceColor.isActive

        val baseColor = when {
            RainbowColor.isActive -> rainbow()
            StaticColor.isActive -> StaticColor.color
            else -> null
        }

        val viewDistance =
            (if (DistanceColor.useViewDistance) mc.options.viewDistance.value.toFloat() else DistanceColor.customViewDistance) * 16 * sqrt(
                2.0
            )
        val filteredEntities = world.entities.filter(this::shouldRenderTrace)
        val camera = mc.gameRenderer.camera

        if (filteredEntities.isEmpty()) {
            return@handler
        }

        renderEnvironmentForWorld(matrixStack) {
            val eyeVector = Vec3(0.0, 0.0, 1.0)
                .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
                .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat()) + Vec3(camera.pos)

            for (entity in filteredEntities) {
                val dist = player.distanceTo(entity) * 2.0

                val color = if (useDistanceColor) {
                    Color4b(
                        Color.getHSBColor(
                            (dist.coerceAtMost(viewDistance) / viewDistance).toFloat() * (120.0f / 360.0f),
                            1.0f,
                            1.0f
                        )
                    )
                } else if (entity is PlayerEntity && FriendManager.isFriend(entity.gameProfile.name)) {
                    Color4b(0, 0, 255)
                } else {
                    ModuleMurderMystery.getColor(entity) ?: baseColor ?: continue
                }

                val pos = entity.interpolateCurrentPosition(event.partialTicks)

                withColor(color) {
                    drawLines(eyeVector, pos, Vec3(0f, entity.height, 0f))
                }
            }
        }

    }

    @JvmStatic
    fun shouldRenderTrace(entity: Entity) = entity.shouldBeShown()
}
