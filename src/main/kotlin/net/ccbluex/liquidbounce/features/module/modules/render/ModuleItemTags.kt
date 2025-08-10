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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.render.newDrawContext
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.kotlin.forEachWithSelf
import net.ccbluex.liquidbounce.utils.kotlin.proportionOfValue
import net.ccbluex.liquidbounce.utils.kotlin.valueAtProportion
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.math.average
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Vec3d
import java.util.*

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

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val items by items("Items", hashSetOf())

    private val backgroundColor by color("BackgroundColor", Color4b(Int.MIN_VALUE, hasAlpha = true))

    private val clusterSizeMode = choices("ClusterSizeMode", ClusterSizeMode.Static,
        arrayOf(ClusterSizeMode.Static, ClusterSizeMode.Distance))
    private val scale by float("Scale", 1.5F, 0.25F..4F)
    private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)
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

    private var itemEntities by computedOn<GameTickEvent, Map<Vec3d, List<ItemStack>>>(
        initialValue = emptyMap()
    ) { _, _ ->
        val cameraPos = (mc.cameraEntity ?: player).pos
        val maxDistSquared = maximumDistance.sq()

        @Suppress("UNCHECKED_CAST")
        (world.entities.filter {
            it is ItemEntity && it.squaredDistanceTo(cameraPos) < maxDistSquared && filter(it.stack.item, items)
        } as List<ItemEntity>).cluster()
    }

    override fun onDisabled() {
        itemEntities = emptyMap()
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        itemEntities = emptyMap()
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> {
        renderEnvironmentForGUI {
            itemEntities.mapNotNull { (center, items) ->
                val renderPos = WorldToScreen.calculateScreenPos(center.add(renderOffset))
                    ?: return@mapNotNull null
                renderPos to items
            }.forEachWithSelf { (center, items), i, self ->
                val z = 1000.0F * i / self.size
                drawItemTags(items, center.copy(z = z))
            }
        }
    }

    @JvmStatic
    private fun drawItemTags(
        items: List<ItemStack>,
        pos: Vec3,
    ) {
        val width = items.size * ITEM_SIZE
        val height = ITEM_SIZE

        val dc = newDrawContext()

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
            backgroundColor.toARGB()
        )

        // render stacks
        items.forEachIndexed { index, stack ->
            val leftX = index * ITEM_SIZE
            dc.drawItem(
                stack,
                leftX,
                0,
            )
            dc.drawStackOverlay(mc.textRenderer, stack, leftX, 0)
        }
    }

    @JvmStatic
    private fun List<ItemEntity>.cluster(): Map<Vec3d, List<ItemStack>> {
        if (this.isEmpty()) {
            return emptyMap()
        }

        val groups = mutableListOf<Set<ItemEntity>>()
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
                entities.mergeStacks(),
            )
        }
    }

    /**
     * Merge stacks with same item, order by count desc
     */
    @JvmStatic
    private fun Set<ItemEntity>.mergeStacks(): List<ItemStack> {
        val map = IdentityHashMap<Item, MutableList<ItemStack>>()
        for (itemEntity in this) {
            map.getOrPut(itemEntity.stack.item, ::mutableListOf).add(itemEntity.stack)
        }
        val result = ArrayList<ItemStack>(map.size)
        map.values.forEach { stacks ->
            if (stacks.size == 1) {
                result.add(stacks[0])
            } else {
                result.add(ItemStack(stacks[0].item, stacks.sumOf { it.count }))
            }
        }
        result.sortByDescending { it.count }
        return result
    }

}
