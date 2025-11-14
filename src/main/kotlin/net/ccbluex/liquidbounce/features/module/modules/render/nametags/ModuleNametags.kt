/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.GUIRenderEnvironment
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.math.sq
import org.joml.Vector2fc

/**
 * Nametags module
 *
 * Makes player name tags more visible and adds useful information.
 */
object ModuleNametags : ClientModule("Nametags", Category.RENDER) {
    internal val show by multiEnumChoice("Show", NametagShowOptions.entries)
    val scale by float("Scale", 2F, 0.25F..4F)
    private val maximumDistance by float("MaximumDistance", 100F, 1F..256F)

    internal val batchRenderMode by enumChoice("BatchRenderMode", BatchRenderMode.EACH)

    internal enum class BatchRenderMode(override val choiceName: String) : NamedChoice {
        FULL("Full"),
        EACH("Each"),
    }

    internal val drawnEnchantmentAreas = mutableListOf<Vector2fc>()

    val fontRenderer
        get() = FontManager.FONT_RENDERER

    private val nametagsToRender = mutableListOf<Nametag>()

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        nametagsToRender.clear()
    }

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        RenderedEntities.onUpdate(::collectAndSortNametagsToRender)
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent>(priority = FIRST_PRIORITY) { event ->
        if (nametagsToRender.isEmpty()) {
            return@handler
        }

        renderEnvironmentForGUI(event) {
            drawNametags(event.tickDelta)
        }
    }

    private fun GUIRenderEnvironment.drawNametags(tickDelta: Float) {
        drawnEnchantmentAreas.clear()
        nametagsToRender.forEach { it.calculateScreenPos(tickDelta) }

        val filteredNameTags = nametagsToRender.filterTo(mutableListOf()) { it.screenPos != null }
        if (filteredNameTags.isEmpty()) {
            return
        }

        val nametagsCount = filteredNameTags.size.toFloat()

        if (batchRenderMode == BatchRenderMode.FULL) startBatch()
        filteredNameTags.forEachIndexed { index, nametagInfo ->
            val pos = nametagInfo.screenPos!!

            // We want nametags that are closer to the player to be rendered above nametags that are further away.
            val renderZ = 0.01f + index / nametagsCount * 1000.0F

            drawNametag(nametagInfo, pos.copy(z = renderZ))
        }
        if (batchRenderMode == BatchRenderMode.FULL) commitBatch()
    }

    /**
     * Collects all entities that should be rendered, gets the screen position, where the name tag should be displayed,
     * add what should be rendered ([Nametag]). The nametags are sorted in order of rendering.
     */
    private fun collectAndSortNametagsToRender() {
        nametagsToRender.clear()
        val maximumDistanceSquared = maximumDistance.sq()

        for (entity in RenderedEntities) {
            if (entity.squaredDistanceTo(mc.cameraEntity) > maximumDistanceSquared) {
                continue
            }

            nametagsToRender += Nametag(entity)
        }
        nametagsToRender.sortWith(NAMETAG_COMPARATOR)
    }

    private val NAMETAG_COMPARATOR = Comparator.comparingDouble<Nametag> { nametag ->
        nametag.entity.squaredDistanceTo(mc.cameraEntity)
    }

}
