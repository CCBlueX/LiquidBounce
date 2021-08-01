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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.RenderEngine
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.putVertex
import net.ccbluex.liquidbounce.render.utils.drawBoxNew
import net.ccbluex.liquidbounce.render.utils.drawBoxOutlineNew
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.utils.render.espBoxInstancedOutlineRenderTask
import net.ccbluex.liquidbounce.utils.render.espBoxInstancedRenderTask
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.util.math.Box

/**
 * ItemESP module
 *
 * Allows you to see dropped items through walls.
 */

object ModuleItemESP : Module("ItemESP", Category.RENDER) {

    override val translationBaseKey: String
        get() = "liquidbounce.module.itemEsp"

    private val color by color("Color", Color4b(255, 179, 72, 255))
    private val colorRainbow by boolean("Rainbow", false)

    private val modes = choices("Mode", BoxMode, arrayOf(BoxMode))

    private object BoxMode : Choice("Box") {

        override val parent: ChoiceConfigurable
            get() = modes

        val box = drawBoxNew(Box(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125), Color4b.WHITE)

        val boxOutline = drawBoxOutlineNew(Box(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125), Color4b.WHITE)

        val renderHandler = handler<EngineRenderEvent> { event ->
            val base = if (colorRainbow) rainbow() else color
            val baseColor = Color4b(base.r, base.g, base.b, 50)
            val outlineColor = Color4b(base.r, base.g, base.b, 100)

            val filtered = world.entities.filter { it is ItemEntity || it is ArrowEntity }

            val instanceBuffer = PositionColorVertexFormat()
            val instanceBufferOutline = PositionColorVertexFormat()

            instanceBuffer.initBuffer(filtered.size)
            instanceBufferOutline.initBuffer(filtered.size)

            for (entity in filtered) {
                val pos = Vec3(
                    entity.lastRenderX + (entity.x - entity.lastRenderX) * event.tickDelta,
                    entity.lastRenderY + (entity.y - entity.lastRenderY) * event.tickDelta,
                    entity.lastRenderZ + (entity.z - entity.lastRenderZ) * event.tickDelta
                )

                instanceBuffer.putVertex { this.position = pos; this.color = baseColor }
                instanceBufferOutline.putVertex { this.position = pos; this.color = outlineColor }
            }

            RenderEngine.enqueueForRendering(
                RenderEngine.CAMERA_VIEW_LAYER,
                espBoxInstancedRenderTask(instanceBuffer, box.first, box.second)
            )
            RenderEngine.enqueueForRendering(
                RenderEngine.CAMERA_VIEW_LAYER,
                espBoxInstancedOutlineRenderTask(instanceBufferOutline, boxOutline.first, boxOutline.second)
            )
        }

    }

}
