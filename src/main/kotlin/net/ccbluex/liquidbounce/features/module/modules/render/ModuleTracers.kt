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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.MathHelper
import java.awt.Color

/**
 * Tracers module
 *
 * Draws a line to every entity a certain radius.
 */

object ModuleTracers : ClientModule("Tracers", Category.RENDER) {

    private val modes = choices("ColorMode", 0) {
        arrayOf(
            DistanceColor,
            GenericStaticColorMode(it, Color4b(0, 160, 255, 255)),
            GenericRainbowColorMode(it)
        )
    }

    private object DistanceColor : GenericColorMode<LivingEntity>("Distance") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        val useViewDistance by boolean("UseViewDistance", true)
        val customViewDistance by float("CustomViewDistance", 128.0F, 1.0F..512.0F)

        override fun getColor(param: LivingEntity): Color4b = throw NotImplementedError()
    }

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        if (RenderedEntities.isEmpty()) {
            return@handler
        }

        val matrixStack = event.matrixStack

        val useDistanceColor = DistanceColor.isSelected

        val viewDistance = 16.0F * MathHelper.SQUARE_ROOT_OF_TWO *
            (if (DistanceColor.useViewDistance) {
                mc.options.viewDistance.value.toFloat()
            } else {
                DistanceColor.customViewDistance
            })
        val camera = mc.gameRenderer.camera

        renderEnvironmentForWorld(matrixStack) {
            val eyeVector = Vec3(0.0, 0.0, 1.0)
                .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
                .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat())

            longLines {
                for (entity in RenderedEntities) {
                    val color = if (useDistanceColor) {
                        val dist = player.distanceTo(entity) * 2.0F
                        Color4b(
                            Color.getHSBColor(
                                (dist.coerceAtMost(viewDistance) / viewDistance) * (120.0f / 360.0f),
                                1.0f,
                                1.0f
                            )
                        )
                    } else if (entity is PlayerEntity && FriendManager.isFriend(entity.gameProfile.name)) {
                        Color4b.BLUE
                    } else {
                        EntityTaggingManager.getTag(entity).color ?: modes.activeChoice.getColor(entity)
                    }

                    val pos = relativeToCamera(entity.interpolateCurrentPosition(event.partialTicks)).toVec3()

                    withColor(color) {
                        drawLines(eyeVector, pos, pos, pos + Vec3(0f, entity.height, 0f))
                    }
                }
            }
        }

    }
}
