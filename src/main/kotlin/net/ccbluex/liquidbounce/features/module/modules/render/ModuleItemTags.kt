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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.GUIRenderEnvironment
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.kotlin.forEachWithSelf
import net.ccbluex.liquidbounce.utils.kotlin.proportionOfValue
import net.ccbluex.liquidbounce.utils.kotlin.valueAtProportion
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.math.average
import net.ccbluex.liquidbounce.utils.math.sq
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
object ModuleItemTags : ClientModule("ItemTags", Category.RENDER) {

    override val baseKey: String
        get() = "liquidbounce.module.itemTags"

    private val clusterSizeMode = choices("ClusterSizeMode", ClusterSizeMode.Static,
        arrayOf(ClusterSizeMode.Static, ClusterSizeMode.Distance))
    private val scale by float("Scale", 1.5F, 0.25F..4F)
    private val renderY by float("RenderY", 0F, -2F..2F)
    private val maximumDistance by float("MaximumDistance", 128F, 1F..256F)

    private sealed class ClusterSizeMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = clusterSizeMode

        abstract fun size(entity: ItemEntity): Float

        object Static : ClusterSizeMode("Static") {
            private val size = float("Size", 1F, 0.1F..32F)
            override fun size(entity: ItemEntity): Float = size.get()
        }

        object Distance : ClusterSizeMode("Distance") {
            private val size by floatRange("Size", 1F..16F, 0.1F..32.0F)
            private val range by floatRange("Range", 32F..64F, 1F..256F)
            private val curve by curve("Curve", Easing.LINEAR)

            override fun size(entity: ItemEntity): Float {
                val playerDistance = player.distanceTo(entity)
                return size.valueAtProportion(curve.transform(range.proportionOfValue(playerDistance)))
            }
        }
    }

    private val fontRenderer
        get() = FontManager.FONT_RENDERER

    private var itemEntities: Map<Vec3d, List<ItemStack>> = emptyMap()

    override fun disable() {
        itemEntities = emptyMap()
    }

    @Suppress("unused", "UNCHECKED_CAST")
    private val tickHandler = handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        val maxDistSquared = maximumDistance.sq()

        itemEntities = (world.entities.filter {
            it is ItemEntity && it.squaredDistanceTo(player) < maxDistSquared
        } as List<ItemEntity>).cluster()
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        itemEntities = emptyMap()
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        renderEnvironmentForGUI {
            fontRenderer.withBuffers { buf ->
                itemEntities.mapNotNull { (center, items) ->
                    val renderPos = WorldToScreen.calculateScreenPos(center.add(0.0, renderY.toDouble(), 0.0))
                        ?: return@mapNotNull null
                    renderPos to items
                }.forEachWithSelf { (center, items), i, self ->
                    withMatrixStack {
                        val z = 1000.0F * i / self.size
                        drawItemTags(items, Vec3(center.x, center.y, z), buf)
                    }
                }
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
            Color4b(0, 0, 0, 128).toARGB()
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
    private fun List<ItemEntity>.cluster(): Map<Vec3d, List<ItemStack>> {
        if (isEmpty()) {
            return emptyMap()
        }

        val groups = arrayListOf<Set<ItemEntity>>()
        val visited = hashSetOf<ItemEntity>()

        for (entity in this) {
            if (entity in visited) continue

            val radiusSquared = clusterSizeMode.activeChoice.size(entity).sq()

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
                entities.map { it.box.center }.average(),
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
