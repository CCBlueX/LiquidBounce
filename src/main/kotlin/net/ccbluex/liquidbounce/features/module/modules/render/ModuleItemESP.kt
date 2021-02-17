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

import net.ccbluex.liquidbounce.event.LiquidBounceRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Choice
import net.ccbluex.liquidbounce.features.module.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.renderer.engine.*
import net.ccbluex.liquidbounce.renderer.engine.utils.drawBox
import net.ccbluex.liquidbounce.renderer.engine.utils.drawBoxOutline
import net.ccbluex.liquidbounce.renderer.utils.rainbow
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.util.math.Box

object ModuleItemESP : Module("ItemESP", Category.RENDER) {
    private val colorRedValue = int("R", 255, 0..255)
    private val colorGreenValue = int("G", 179, 0..255)
    private val colorBlueValue = int("B", 72, 0..255)
    private val colorRainbow = boolean("Rainbow", false)

    private object ModeConfigurable : ChoiceConfigurable(this, "Mode", "YPort", {
        BoxMode
    })

    init {
        tree(ModeConfigurable)
    }

    private object BoxMode : Choice("Box", ModeConfigurable) {
        val box = run {
            val task = drawBox(Box(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125), Color4b.WHITE)

            task.storageType = VBOStorageType.Static

            task
        }
        val boxOutline = run {
            val task = drawBoxOutline(Box(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125), Color4b.WHITE)

            task.storageType = VBOStorageType.Static

            task
        }

        val tickHandler = handler<LiquidBounceRenderEvent> {
            val base = if (colorRainbow.value) rainbow() else Color4b(
                colorRedValue.value,
                colorGreenValue.value,
                colorBlueValue.value,
                255
            )

            val baseColor = Color4b(base.r, base.g, base.b, 50)
            val outlineColor = Color4b(base.r, base.g, base.b, 100)


            val filtered = mc.world!!.entities.filter { en -> en is ItemEntity || en is ArrowEntity }

            val renderTask = InstancedColoredPrimitiveRenderTask(filtered.size, this.box)
            val outlineRenderTask = InstancedColoredPrimitiveRenderTask(filtered.size, this.boxOutline)

            for (entity in filtered) {
                val pos = Vec3(
                    entity.lastRenderX + (entity.x - entity.lastRenderX) * it.tickDelta,
                    entity.lastRenderY + (entity.y - entity.lastRenderY) * it.tickDelta,
                    entity.lastRenderZ + (entity.z - entity.lastRenderZ) * it.tickDelta
                )

                renderTask.instance(pos, baseColor)
                outlineRenderTask.instance(pos, outlineColor)
            }

            RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, renderTask)
            RenderEngine.enqueueForRendering(RenderEngine.CAMERA_VIEW_LAYER, outlineRenderTask)
        }

    }

}
