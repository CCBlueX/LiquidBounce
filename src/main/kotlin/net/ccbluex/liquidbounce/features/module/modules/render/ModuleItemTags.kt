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

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.Fonts
import net.ccbluex.liquidbounce.render.GUIRenderEnvironment
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.font.FontRenderer
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.ItemEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Vec3d

private const val ITEM_SIZE: Int = 16
private const val ITEM_SCALE: Float = 1.0F
private const val BACKGROUND_PADDING: Int = 2

/**
 * ItemTags module
 *
 * Show the names and quantities of items in several boxes.
 */
object ModuleItemTags : Module("ItemTags", Category.RENDER) {

    override val translationBaseKey: String
        get() = "liquidbounce.module.itemTags"

    private val boxSize by float("BoxSize", 1.0F, 0.1F..10.0F)
    private val scale by float("Scale", 1.5F, 0.25F..4F)
    private val renderY by float("RenderY", 0.0F, -2.0F..2.0F)
    private val maximumDistance by float("MaximumDistance", 100F, 1F..256F)

    private val fontRenderer by lazy {
        Fonts.DEFAULT_FONT.get()
    }

    @Suppress("unused")
    val renderHandler = handler<OverlayRenderEvent> {
        val fontBuffers = FontRendererBuffers()

        renderEnvironmentForGUI {
            try {
                val maxDistSquared = maximumDistance * maximumDistance
                val clusters = world.entities
                    .filterIsInstance<ItemEntity>()
                    .filter {
                        it.squaredDistanceTo(player) < maxDistSquared
                    }
                    .clusterWithIn(boxSize.toDouble())
                    .mapNotNull { (center, items) ->
                        val renderPos = WorldToScreen.calculateScreenPos(center.add(0.0, renderY.toDouble(), 0.0))
                            ?: return@mapNotNull null
                        renderPos to items
                    }

                clusters.forEachIndexed { i, (center, items) ->
                    with(matrixStack) {
                        push()
                        try {
                            val z = 1000.0F * i / clusters.size
                            drawItemTags(items, Vec3(center.x, center.y, z), fontBuffers)
                        } finally {
                            pop()
                        }
                    }
                }
            } finally {
                fontBuffers.draw(fontRenderer)
            }
        }
    }

    @JvmStatic
    private fun GUIRenderEnvironment.drawItemTags(
        items: List<ItemStack>,
        pos: Vec3,
        fontBuffers: FontRendererBuffers,
    ) {
        val width = items.size * ITEM_SIZE
        val height = ITEM_SIZE

        val dc = DrawContext(
            mc,
            mc.bufferBuilders.entityVertexConsumers
        )

        val itemScale = ITEM_SCALE * scale
        dc.matrices.translate(pos.x, pos.y, 0.0F)
        dc.matrices.scale(itemScale, itemScale, 1.0F)
        dc.matrices.translate(-width / 2f, -height / 2f, pos.z)

        // draw background
        dc.fill(
            -BACKGROUND_PADDING,
            -BACKGROUND_PADDING,
            width + BACKGROUND_PADDING,
            height + BACKGROUND_PADDING,
            Color4b(0, 0, 0, 128).toRGBA()
        )

        val c = fontRenderer.size
        val fontScale = 1.0F / (c * 0.15F) * scale

        // sync x pos between item and count
        fun scale(f: Int) = f * itemScale / fontScale

        matrixStack.push()
        matrixStack.translate(pos.x, pos.y, pos.z)
        matrixStack.scale(fontScale, fontScale, 1.0F)
        matrixStack.translate(-scale(width) / 2f, -scale(height) / 2f, 1000.0F)

        // render stacks
        items.forEachIndexed { index, stack ->
            val leftX = index * ITEM_SIZE
            dc.drawItem(
                stack,
                leftX,
                0,
            )

            if (stack.count > 1) {
                val text = fontRenderer.process(stack.count.toString().asText())

                fontRenderer.draw(
                    text,
                    scale(leftX + ITEM_SIZE) - fontRenderer.getStringWidth(text),
                    scale(ITEM_SIZE) - fontRenderer.height,
                    shadow = true,
                )
            }
        }

        fontRenderer.commit(fontBuffers)
        matrixStack.pop()
    }

    @JvmStatic
    private fun List<ItemEntity>.clusterWithIn(radius: Double): Map<Vec3d, List<ItemStack>> {
        val groups = arrayListOf<MutableSet<ItemEntity>>()
        val visited = hashSetOf<ItemEntity>()

        val radiusSquared = radius * radius
        for (entity in this) {
            if (entity in visited) continue

            // `entity` will also be added
            val group = this.filterTo(hashSetOf()) { other ->
                other !in visited && entity.squaredDistanceTo(other) < radiusSquared
            }

            visited.addAll(group)
            groups.add(group)
        }

        return groups.associate { entities ->
            Pair(
                // Get the center pos of all entities
                entities.map { it.box.center }.reduce(Vec3d::add).multiply(1.0 / entities.size),
                // Merge stacks with same item, order by count desc
                entities.groupBy {
                    it.stack.item
                }.map { (item, entities) ->
                    ItemStack(item, entities.sumOf { it.stack.count })
                }.sortedByDescending {
                    it.count
                },
            )
        }
    }

}
