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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleESP
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.Entity

/**
 * Nametags module
 *
 * Makes player name tags more visible and adds useful information.
 */

object ModuleNametags : Module("Nametags", Category.RENDER) {
    val items by boolean("Items", true)

    object Health : ToggleableConfigurable(this, "Health", true) {
        val fromScoreboard by boolean("FromScoreboard", false)

    }

    object HealthColor : ToggleableConfigurable(this, "HealthColor", true) {
        val greenHealth by int("Green", 12, -1..40)
        val yellowHealth by int("Yellow", 8, -1..40)
        val redHealth by int("Red", -1, -1..40)
    }

    init {
        tree(Health)
        tree(HealthColor)
    }


    val ping by boolean("Ping", true)
    val distance by boolean("Distance", false)

    val border by boolean("Border", true)
    val scale by float("Scale", 2F, 0.25F..4F)

    val maximumDistance by float("MaximumDistance", 100F, 1F..256F)

    val fontRenderer: FontRenderer
        get() = Fonts.DEFAULT_FONT.get()

    @Suppress("unused")
    val overlayRenderHandler = handler<OverlayRenderEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) { event ->
        renderEnvironmentForGUI {
            val nametagRenderer = NametagRenderer()

            try {
                drawNametags(nametagRenderer, event.tickDelta)
            } finally {
                nametagRenderer.commit(this)
            }
        }
    }

    private fun RenderEnvironment.drawNametags(nametagRenderer: NametagRenderer, tickDelta: Float) {
        val nametagsToRender = collectAndSortNametagsToRender(tickDelta)

        nametagsToRender.forEachIndexed { index, (pos, nametagInfo) ->
            // We want nametags that are closer to the player to be rendered above nametags that are further away.
            val renderZ = index / nametagsToRender.size.toFloat()

            nametagRenderer.drawNametag(this, nametagInfo, Vec3(pos.x, pos.y, renderZ))
        }
    }

    /**
     * Collects all entities that should be rendered, gets the screen position, where the name tag should be displayed,
     * add what should be rendered ([NametagInfo]). The nametags are sorted in order of rendering.
     */
    private fun collectAndSortNametagsToRender(tickDelta: Float): List<Pair<Vec3, NametagInfo>> {
        val nametagsToRender = mutableListOf<Pair<Vec3, NametagInfo>>()

        for (entity in ModuleESP.findRenderedEntities()) {
            val nametagPos = entity.interpolateCurrentPosition(tickDelta)
                .add(0.0, entity.getEyeHeight(entity.pose) + 0.55, 0.0)

            if (entity.distanceTo(mc.cameraEntity) > maximumDistance) continue

            val screenPos = WorldToScreen.calculateScreenPos(nametagPos) ?: continue

            val nametagInfo = NametagInfo.createForEntity(entity)

            nametagsToRender.add(Pair(screenPos, nametagInfo))
        }

        nametagsToRender.sortByDescending { it.first.z }

        return nametagsToRender
    }

    /**
     * Should [ModuleNametags] render nametags above this [entity]?
     */
    @JvmStatic
    fun shouldRenderNametag(entity: Entity) = entity.shouldBeShown()
}
