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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.engine.memory.IndexBuffer
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.VertexFormatComponentDataType
import net.ccbluex.liquidbounce.render.engine.memory.putVertex
import net.ccbluex.liquidbounce.render.shaders.ColoredPrimitiveShader
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.minecraft.entity.Entity
import net.minecraft.entity.player.PlayerEntity
import java.awt.Color
import kotlin.math.sqrt

/**
 * Traces module
 *
 * Draws a line to every entity a certain radius.
 */

object ModuleTraces : Module("Traces", Category.RENDER) {

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

    val renderHandler = handler<EngineRenderEvent> { event ->
        val useDistanceColor = DistanceColor.isActive

        val baseColor = when {
            RainbowColor.isActive -> rainbow()
            StaticColor.isActive -> StaticColor.color
            else -> null
        }

        val viewDistance =
            (if (DistanceColor.useViewDistance) mc.options.viewDistance.toFloat() else DistanceColor.customViewDistance) * 16 * sqrt(
                2.0
            )
        val player = mc.player!!
        val filteredEntities = world.entities.filter(this::shouldRenderTrace)
        val camera = mc.gameRenderer.camera

        if (filteredEntities.isEmpty()) {
            return@handler
        }

        val vertexFormat = PositionColorVertexFormat()

        vertexFormat.initBuffer(filteredEntities.size * 3)

        val indexBuffer = IndexBuffer(filteredEntities.size * 2 * 2, VertexFormatComponentDataType.GlUnsignedShort)

        val eyeVector = Vec3(0.0, 0.0, 1.0).rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
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
            } else if (entity is PlayerEntity && FriendManager.isFriend(entity.toString())) {
                Color4b(0, 0, 255)
            } else {
                baseColor!!
            }

            val x = (entity.lastRenderX + (entity.x - entity.lastRenderX) * event.tickDelta)
            val y = (entity.lastRenderY + (entity.y - entity.lastRenderY) * event.tickDelta)
            val z = (entity.lastRenderZ + (entity.z - entity.lastRenderZ) * event.tickDelta)

            val v0 = vertexFormat.putVertex { this.position = eyeVector; this.color = color }
            val v1 = vertexFormat.putVertex { this.position = Vec3(x, y, z); this.color = color }
            val v2 = vertexFormat.putVertex { this.position = Vec3(x, y + entity.height, z); this.color = color }

            indexBuffer.index(v0)
            indexBuffer.index(v1)
            indexBuffer.index(v1)
            indexBuffer.index(v2)
        }

        val renderTask = VertexFormatRenderTask(
            vertexFormat,
            PrimitiveType.Lines,
            ColoredPrimitiveShader,
            indexBuffer = indexBuffer,
            state = GlRenderState(lineWidth = 1.0f, lineSmooth = true)
        )

        RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER_WITHOUT_BOBBING, renderTask)
    }

    @JvmStatic
    fun shouldRenderTrace(entity: Entity) = entity.shouldBeShown()
}
