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

import net.ccbluex.liquidbounce.event.EngineRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.*
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.putVertex
import net.ccbluex.liquidbounce.render.shaders.ColoredPrimitiveShader
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.math.times

/**
 * Rotations module
 *
 * Allows you to see server-sided rotations.
 */

object ModuleRotations : Module("Rotations", Category.RENDER) {

    val showRotationVector by boolean("ShowRotationVector", false)

    val renderHandler = handler<EngineRenderEvent> {
        if (!showRotationVector) {
            return@handler
        }

        val serverRotation = RotationManager.serverRotation ?: return@handler

        val vertexFormat = PositionColorVertexFormat()

        vertexFormat.initBuffer(2)

        val camera = mc.gameRenderer.camera

        val eyeVector = Vec3(0.0, 0.0, 1.0)
            .rotatePitch((-Math.toRadians(camera.pitch.toDouble())).toFloat())
            .rotateYaw((-Math.toRadians(camera.yaw.toDouble())).toFloat()) + Vec3(camera.pos) + Vec3(0.0, 0.0, -1.0)

        vertexFormat.putVertex { this.position = eyeVector; this.color = Color4b.WHITE }
        vertexFormat.putVertex { this.position = eyeVector + Vec3(serverRotation.rotationVec * 2.0); this.color = Color4b.WHITE }

        RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, VertexFormatRenderTask(vertexFormat, PrimitiveType.LineStrip, ColoredPrimitiveShader, state = GlRenderState(lineWidth = 2.0f, lineSmooth = true)))
    }

}
