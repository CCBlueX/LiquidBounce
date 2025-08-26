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

import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.entity.Entity
import kotlin.math.abs

/**
 * Nametags module
 *
 * Makes player name tags more visible and adds useful information.
 */
@Suppress("MagicNumber")
object ModuleNametags : ClientModule("Nametags", Category.RENDER) {
    internal val show by multiEnumChoice("Show", NametagShowOptions.entries)
    val scale by float("Scale", 2F, 0.25F..4F)
    private val maximumDistance by float("MaximumDistance", 100F, 1F..256F)

    internal val drawnEnchantmentAreas = mutableListOf<Pair<Float, Float>>()

    val fontRenderer
        get() = FontManager.FONT_RENDERER

    private val nametagsToRender by computedOn<GameTickEvent, MutableList<Nametag>>(
        initialValue = mutableListOf()
    ) { _, list ->
        list.clear()
        collectAndSortNametagsToRender(list)
        list
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        nametagsToRender.clear()
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        nametagsToRender.clear()
    }

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent>(priority = FIRST_PRIORITY) { event ->
        if (nametagsToRender.isEmpty()) {
            return@handler
        }

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

        drawnEnchantmentAreas.clear()

        nametagsToRender.forEach { it.calculatePosition(tickDelta) }
        val filteredNameTags = nametagsToRender.filter { it.position != null }
        val nametagsCount = filteredNameTags.size.toFloat()


        val sortedTags = filteredNameTags.sortedBy { tag ->
            tag.entity.squaredDistanceTo(mc.cameraEntity)
        }

        sortedTags.forEachIndexed { index, nametagInfo ->
            val pos = nametagInfo.position!!

            // We want nametags that are closer to the player to be rendered above nametags that are further away.
            val renderZ = index / nametagsCount * 1000.0F

            nametagRenderer.drawNametag(this, nametagInfo, Vec3(pos.x, pos.y, renderZ))
        }
    }

    /**
     * Collects all entities that should be rendered, gets the screen position, where the name tag should be displayed,
     * add what should be rendered ([Nametag]). The nametags are sorted in order of rendering.
     */
    private fun collectAndSortNametagsToRender(list: MutableList<Nametag>) {
        val maximumDistanceSquared = maximumDistance.sq()

        for (entity in RenderedEntities) {
            if (entity.squaredDistanceTo(mc.cameraEntity) > maximumDistanceSquared) {
                continue
            }

            list += Nametag(entity)
        }

        list.sortByDescending { abs(it.entity.z - player.pos.z) }
    }

    /**
     * Should [ModuleNametags] render nametags above this [entity]?
     */
    @JvmStatic
    fun shouldRenderNametag(entity: Entity) = entity.shouldBeShown()
}
