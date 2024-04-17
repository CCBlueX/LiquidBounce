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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.murdermystery.ModuleMurderMystery
import net.ccbluex.liquidbounce.render.GenericColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.combat.EntityTaggingManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import java.awt.Color
import kotlin.math.sqrt

/**
 * Tracers module
 *
 * Draws a line to every entity a certain radius.
 */

object ModuleTracers : Module("Tracers", Category.RENDER) {

    private val modes = choices<GenericColorMode<LivingEntity>>(
        "ColorMode",
        { DistanceColor },
        {
            arrayOf(
                DistanceColor,
                GenericStaticColorMode(it, Color4b(0, 160, 255, 255)),
                GenericRainbowColorMode(it)
            )
        }
    )

    private object DistanceColor : GenericColorMode<LivingEntity>("Distance") {
        override val parent: ChoiceConfigurable<*>
            get() = modes

        val useViewDistance by boolean("UseViewDistance", true)
        val customViewDistance by float("CustomViewDistance", 128.0F, 1.0F..512.0F)

        override fun getColor(param: LivingEntity): Color4b = throw NotImplementedError()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        val matrixStack = event.matrixStack

        val useDistanceColor = DistanceColor.isActive

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
                .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat())

            for (entity in filteredEntities) {
                if (entity !is LivingEntity) {
                    continue
                }

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
                    EntityTaggingManager.getTag(entity).color ?: modes.activeChoice.getColor(entity) ?: continue
                }

                val pos = relativeToCamera(entity.interpolateCurrentPosition(event.partialTicks)).toVec3()

                withColor(color) {
                    drawLines(eyeVector, pos, pos, pos + Vec3(0f, entity.height, 0f))
                }
            }
        }

    }

    @JvmStatic
    fun shouldRenderTrace(entity: Entity) = entity.shouldBeShown()
}
