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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.BoxesRenderer
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.utils.rainbow
import net.ccbluex.liquidbounce.render.withPosition
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toVec3
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

        private val box = Box(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val base = if (colorRainbow) rainbow() else color
            val baseColor = base.alpha(50)
            val outlineColor = base.alpha(100)

            val filtered = world.entities.filter { it is ItemEntity || it is ArrowEntity }

            val boxRenderer = BoxesRenderer()

            renderEnvironmentForWorld(matrixStack) {
                for (entity in filtered) {
                    val pos = entity.interpolateCurrentPosition(event.partialTicks)

                    withPositionRelativeToCamera(pos) {
                        boxRenderer.drawBox(this, box, true)
                    }
                }


                boxRenderer.draw(this, baseColor, outlineColor)


            }
        }

    }

}
