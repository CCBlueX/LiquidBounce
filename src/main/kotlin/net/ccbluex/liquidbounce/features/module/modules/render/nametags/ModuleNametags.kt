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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleESP
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.render.LiquidBounceFonts
import net.minecraft.entity.Entity

/**
 * Nametags module
 *
 * Makes player name tags more visible and adds useful information.
 */

object ModuleNametags : Module("Nametags", Category.RENDER) {
    val health by boolean("Health", true)
    val ping by boolean("Ping", true)
    val distance by boolean("Distance", false)

    val border by boolean("Border", true)
    val scale by float("Scale", 2F, 1F..4F)

    val fontRenderer: FontRenderer
        get() = LiquidBounceFonts.DEFAULT_FONT

    val renderHandler =
        handler<WorldRenderEvent>(priority = -100) { event ->
            val matrixStack = event.matrixStack

            renderEnvironmentForWorld(matrixStack) {
                val nametagRenderer = NametagRenderer()

                try {
                    drawNametags(nametagRenderer, event.partialTicks)
                } finally {
                    nametagRenderer.commit(this)
                }
            }
        }

    private fun RenderEnvironment.drawNametags(
        nametagRenderer: NametagRenderer,
        tickDelta: Float,
    ) {
        for (entity in ModuleESP.findRenderedEntities()) {
            val nametagPos =
                entity
                    .interpolateCurrentPosition(tickDelta)
                    .add(Vec3(0.0F, entity.getEyeHeight(entity.pose) + 0.55F, 0.0F))

            val text = NametagTextFormatter(entity).format()

            nametagRenderer.drawNametag(this, text, nametagPos)
        }
    }

    /**
     * Should [ModuleNametags] render nametags above this [entity]?
     */
    @JvmStatic
    fun shouldRenderNametag(entity: Entity) = entity.shouldBeShown()
}
