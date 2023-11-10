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

package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleDisabler
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.drawSolidBox
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.utils.drawBoxOutlineNew
import net.ccbluex.liquidbounce.render.utils.drawBoxSide
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.render.withPosition
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

/**
 * A target tracker to choose the best enemy to attack
 */
class TargetRenderer(module: Module) : ToggleableConfigurable(module, "TargetRendering", true) {

    val appearance = choices(module, "Mode", Legacy, arrayOf(Legacy))

    fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {
        ((appearance.activeChoice) as TargetRenderAppearance).render(env, entity, partialTicks)
    }

    object Legacy : TargetRenderAppearance("Legacy") {
        override val parent: ChoiceConfigurable
            get() = this.parent

        val size by float("Size", 0.5f, 0.1f..2f)

        val height by float("Height", 0.1f, 0.02f..2f)

        val color by color("Color", Color4b(0x007CFF64, true))

        val extraYOffset by float("ExtraYOffset", 0.1f, 0f..1f)
        override fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {
            val box = Box(
                -size.toDouble(), 0.0, -size.toDouble(),
                size.toDouble(), height.toDouble(), size.toDouble()
            )

            val pos = entity.interpolateCurrentPosition(partialTicks) + Vec3(0.0, entity.height.toDouble() + extraYOffset.toDouble(), 0.0)
            with(env) {
                withColor(color) {
                    withPosition(pos) {
                        drawSolidBox(box)
                    }
                }
            }
        }
    }

}

abstract class TargetRenderAppearance(name: String):  Choice(name) {
    open fun render(env: RenderEnvironment, entity: Entity, partialTicks: Float) {}
}
