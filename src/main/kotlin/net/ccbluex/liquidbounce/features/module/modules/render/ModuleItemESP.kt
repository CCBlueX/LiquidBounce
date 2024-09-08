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
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.util.math.Box

/**
 * ItemESP module
 *
 * Allows you to see dropped items through walls.
 */
@IncludeModule
object ModuleItemESP : Module("ItemESP", Category.RENDER) {

    override val translationBaseKey: String
        get() = "liquidbounce.module.itemEsp"


    private val modes = choices("Mode", OutlineMode, arrayOf(GlowMode, OutlineMode, BoxMode))
    private val colorMode = choices<GenericColorMode<Any?>>(
        "ColorMode",
        { it.choices[0] },
        { arrayOf(GenericStaticColorMode(it, Color4b(255, 179, 72, 255)), GenericRainbowColorMode(it)) }
    )

    private object BoxMode : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val box = Box(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack

            val base = getColor()
            val baseColor = base.alpha(50)
            val outlineColor = base.alpha(100)

            val filtered = world.entities.filter(::shouldRender)

            renderEnvironmentForWorld(matrixStack) {
                BoxRenderer.drawWith(this) {
                    for (entity in filtered) {
                        val pos = entity.interpolateCurrentPosition(event.partialTicks)

                        withPositionRelativeToCamera(pos) {
                            drawBox(box, baseColor, outlineColor)
                        }
                    }
                }
            }
        }
    }

    object GlowMode : Choice("Glow") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes
    }

    object OutlineMode : Choice("Outline") {
        override val parent: ChoiceConfigurable<Choice>
            get() = modes
    }

    fun shouldRender(it: Entity?) = it is ItemEntity || it is ArrowEntity

    fun getColor() = this.colorMode.activeChoice.getColor(null)
}
