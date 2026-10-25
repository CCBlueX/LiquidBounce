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

package net.ccbluex.liquidbounce.features.module.modules.render.trajectories

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.nameprotect.sanitizeForeignInput
import net.ccbluex.liquidbounce.features.module.modules.render.TimeUnit
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.engine.font.HorizontalAnchor
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorRenderState
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorRenderer
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorStyle
import net.ccbluex.liquidbounce.render.gui.indicator.IndicatorVisibility
import net.ccbluex.liquidbounce.render.gui.indicator.NoneIndicatorStyle
import net.ccbluex.liquidbounce.render.gui.indicator.TriangleIndicatorStyle
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.entity.simpleDisplayName
import net.ccbluex.liquidbounce.utils.math.toFixed
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfoRenderer
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.text.withFormat
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.util.FormattedCharSequence
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3

object TrajectoryDetailedInfoRenderer : ToggleableValueGroup(ModuleTrajectories, "ShowDetailedInfo", false) {
    private val showAt by enumChoice("ShowAt", ShowAt.ENTITY)

    private enum class ShowAt(
        override val tag: String,
    ) : Tagged {
        OWNER("Owner"),
        ENTITY("Entity"),
        LANDING("Landing");

        fun apply(
            renderer: TrajectoryInfoRenderer,
            result: TrajectoryInfoRenderer.SimulationResult,
        ): Vec3 = when (this) {
            OWNER -> renderer.simulationOwner.position()
            ENTITY -> result.positions.firstOrNull()
            LANDING -> result.positions.lastOrNull()
        } ?: renderer.simulationOwner.position()
    }

    private val item by boolean("Item", true)
    private val ownerName by boolean("OwnerName", true)
    private val distance by boolean("Distance", true)
    private val timeUnit by enumChoice("TimeUnit", TimeUnit.TICKS, aliases = listOf("DurationUnit"))
    private val color by color("Color", Color4b.WHITE)

    private val scale by float("Scale", 1F, 0.25F..4F)
    private val renderOffset by vec3d("RenderOffset", useLocateButton = false)

    /**
     * Indicators for targets that cannot be seen. The edge inset belongs here, since it only decides how far
     * inside the screen those are kept.
     */
    private object RenderOffscreen : ToggleableValueGroup(this, "RenderOffscreen", true) {
        val edgeInset by float("EdgeInset", 0f, 0f..0.9f)
    }

    init {
        tree(RenderOffscreen)
    }

    private class TrajectoryIndicatorState(
        override val pos: Vec3,
        override val indicatorColor: Color4b,
        override val edgeInset: Float,
        override val visibleAt: Collection<IndicatorVisibility>,
        private val icon: ItemStack,
        private val lines: List<FormattedCharSequence>,
        private val showItem: Boolean,
        private val scale: Float,
    ) : IndicatorRenderState {

        override val indicatorStyle: IndicatorStyle = OFFSCREEN_ARROW

        /**
         * A trajectory that is already on screen needs no arrow pointing at it.
         */
        override val inViewIndicatorStyle: IndicatorStyle = NoneIndicatorStyle

        /**
         * The block is drawn with its top center at the origin.
         */
        override fun measureTag(): BoundingBox2f {
            val width = infoBlockWidth(lines, showItem, scale)

            return BoundingBox2f(-width * 0.5F, 0.0F, width * 0.5F, infoBlockHeight(lines, showItem, scale))
        }

        override fun extractTag(ctx: GuiGraphicsExtractor) {
            ctx.drawInfoBlock(icon, lines, showItem, scale, indicatorColor)
        }

    }

    private fun buildLines(
        renderer: TrajectoryInfoRenderer,
        result: TrajectoryInfoRenderer.SimulationResult,
    ): List<FormattedCharSequence> = buildList {
        add(timeUnit.format(result.positions.size).withFormat())

        if (distance && result.positions.isNotEmpty()) {
            add("Dist: ${player.position().distanceTo(result.positions.last()).toFixed(1)}m".withFormat())
        }

        val displayOwner = renderer.displayOwner
        if (ownerName && displayOwner != null && displayOwner !== player) {
            add("Owner: ".withFormat() + displayOwner.simpleDisplayName.sanitizeForeignInput())
        }
    }

    val renderHandler = handler<OverlayRenderEvent> { event ->
        ModuleTrajectories.simulationResults.forEachIndexed { index, (renderer, result) ->
            val lines = buildLines(renderer, result)

            val worldPos = when {
                showAt === ShowAt.OWNER && renderer.simulationOwner === player -> when (renderer.type) {
                    // A trajectory this client predicts for itself has no meaningful world position to point at, so
                    // it stays a stack at the crosshair instead of becoming an indicator.
                    TrajectoryInfoRenderer.Type.HYPOTHETICAL -> ShowAt.LANDING.apply(renderer, result)
                    else -> {
                        val centerX = mc.window.guiScaledWidth * 0.5F
                        val centerY = mc.window.guiScaledHeight * 0.5F

                        event.context.pose().withPush {
                            translate(centerX + 50.0F, centerY + index * (mc.font.lineHeight + 1))
                            event.context.drawInfoBlock(renderer.icon, lines, item, scale, color)
                        }

                        return@forEachIndexed
                    }
                }

                else -> showAt.apply(renderer, result)
            }

            IndicatorRenderer.submit(TrajectoryIndicatorState(
                pos = worldPos.add(renderOffset),
                indicatorColor = color,
                edgeInset = RenderOffscreen.edgeInset,
                visibleAt = if (RenderOffscreen.enabled) IndicatorVisibility.entries else IN_VIEW_ONLY,
                icon = renderer.icon,
                lines = lines,
                showItem = item,
                scale = scale,
            ))
        }
    }

}

private const val ITEM_SIZE = 16.0F
private const val LINE_GAP = 1.0F

private val OFFSCREEN_ARROW = TriangleIndicatorStyle(width = 8.0F, height = 10.0F)

private val IN_VIEW_ONLY: Collection<IndicatorVisibility> = setOf(IndicatorVisibility.IN_VIEW)

/**
 * Draws [icon] and [lines] with the block's top centre at the current pose origin.
 */
private fun GuiGraphicsExtractor.drawInfoBlock(
    icon: ItemStack,
    lines: List<FormattedCharSequence>,
    showItem: Boolean,
    scale: Float,
    color: Color4b,
) {
    val fontRenderer = FontManager.FONT_RENDERER

    pose().withPush {
        scale(scale)

        var top = 0.0F
        if (showItem) {
            item(icon, -8, 0)
            top = ITEM_SIZE
        }

        translate(0.0F, top)
        scale(fontRenderer.scaleToVanillaFont)

        var y = 0.0F
        for (line in lines) {
            fontRenderer.draw(line) {
                this.color = color
                this.y = y
                horizontalAnchor = HorizontalAnchor.CENTER
                shadow = true
            }

            y += fontRenderer.height + LINE_GAP
        }
    }
}

private fun infoBlockWidth(lines: List<FormattedCharSequence>, showItem: Boolean, scale: Float): Float {
    val fontRenderer = FontManager.FONT_RENDERER
    val textWidth = lines.maxOfOrNull { fontRenderer.getStringWidth(it) } ?: 0.0F
    val contentWidth = maxOf(if (showItem) ITEM_SIZE else 0.0F, textWidth * fontRenderer.scaleToVanillaFont)

    return contentWidth * scale
}

private fun infoBlockHeight(lines: List<FormattedCharSequence>, showItem: Boolean, scale: Float): Float {
    val fontRenderer = FontManager.FONT_RENDERER
    val textHeight = lines.size * (fontRenderer.height + LINE_GAP) * fontRenderer.scaleToVanillaFont

    return (if (showItem) ITEM_SIZE + textHeight else textHeight) * scale
}
