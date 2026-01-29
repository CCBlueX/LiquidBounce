/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.fastutil.Pool
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.utils.combat.shouldBeShown
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.ccbluex.liquidbounce.utils.entity.cameraDistanceSq
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.renderer.entity.state.EntityRenderState
import org.joml.Vector2f
import org.joml.Vector2fc

/**
 * Nametags module
 *
 * Makes player name tags more visible and adds useful information.
 */
object ModuleNametags : ClientModule("Nametags", ModuleCategories.RENDER) {

    init {
        tree(NametagTextFormatter)
        tree(NametagEquipment)
        tree(NametagEnchantmentRenderer)
    }

    internal val border by boolean("Border", true)
    internal val scale = curve(
        "Scale",
        mutableListOf(Vector2f(0f, 1f), Vector2f(200f, 1f)),
        xAxis = "Distance" axis 0f..200f,
        yAxis = "Scale" axis 0.25f..4f,
    )
    internal val drawnEnchantmentAreas = mutableListOf<Vector2fc>()

    val fontRenderer
        get() = FontManager.FONT_RENDERER

    private val nametagPool = Pool(::NametagRenderState, NametagRenderState::reset)

    private val nametagsToRender = mutableListOf<NametagRenderState>()

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        nametagPool.recycleAll(nametagsToRender)
        nametagsToRender.clear()
    }

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        RenderedEntities.onUpdated(::collectAndSortNametagsToRender)
    }

    @Suppress("unused")
    private val overlayRenderHandler = handler<OverlayRenderEvent>(priority = FIRST_PRIORITY) { event ->
        if (nametagsToRender.isEmpty()) {
            return@handler
        }

        event.context.drawNametags(event.tickDelta)
    }

    private fun GuiGraphics.drawNametags(tickDelta: Float) {
        drawnEnchantmentAreas.clear()

        for (nametagInfo in nametagsToRender) {
            val (x, y) = nametagInfo.calculateScreenPos(tickDelta) ?: continue

            drawNametag(nametagInfo, x, y)
        }
    }

    /**
     * Collects all entities that should be rendered, gets the screen position, where the name tag should be displayed,
     * add what should be rendered ([NametagRenderState]). The nametags are sorted in order of rendering.
     */
    private fun collectAndSortNametagsToRender() {
        nametagPool.recycleAll(nametagsToRender)
        nametagsToRender.clear()
        for (entity in RenderedEntities) {
            val distance = entity.position().cameraDistance().toFloat()
            val scale = scale.transform(distance)
            if (scale > 0.01f) {
                val nametag = nametagPool.borrow()
                nametag.update(entity, scale)
                nametagsToRender += nametag
            }
        }
        nametagsToRender.sortWith(NAMETAG_COMPARATOR)
    }

    private val NAMETAG_COMPARATOR: Comparator<NametagRenderState> = Comparator.comparingDouble { nametag ->
        nametag.entity!!.position().cameraDistanceSq()
    }

    fun shouldRenderVanillaNametag(state: EntityRenderState): Boolean {
        return !running || !((state as EntityRenderStateAddition).`liquid_bounce$getEntity`()
            ?: return true).shouldBeShown()
    }

}
