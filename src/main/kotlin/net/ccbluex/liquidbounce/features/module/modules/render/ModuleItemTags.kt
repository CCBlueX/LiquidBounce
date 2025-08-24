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

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.drawItemTags
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.kotlin.forEachWithSelf
import net.ccbluex.liquidbounce.utils.kotlin.mapArray
import net.ccbluex.liquidbounce.utils.kotlin.proportionOfValue
import net.ccbluex.liquidbounce.utils.kotlin.valueAtProportion
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.entity.Entity
import net.minecraft.entity.ItemEntity
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Vec3d

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
    private val scale by float("Scale", 1.5F, 0.25F..4F)
    private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)
    private val rowLength by int("RowLength", 100, 1..100)

    private val clusterSizeMode = choices("ClusterSizeMode", ClusterSizeMode.Static,
        arrayOf(ClusterSizeMode.Static, ClusterSizeMode.Distance))
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
            private val size by floatRange("Size", 1F..16F, 0.1F..32F)
            private val range by floatRange("Range", 32F..64F, 1F..256F)
            private val curve by curve("Curve", Easing.LINEAR)

            override fun size(entity: ItemEntity): Float {
                val playerDistance = player.distanceTo(entity)
                return size.valueAtProportion(curve.transform(range.proportionOfValue(playerDistance)))
            }
        }
    }

    private val itemEntities by computedOn<GameTickEvent, ObjectArrayList<ClusteredEntities>>(
        initialValue = ObjectArrayList()
    ) { _, clusteredEntities ->
        val cameraPos = (mc.cameraEntity ?: player).pos
        val maxDistSquared = maximumDistance.sq()

        @Suppress("UNCHECKED_CAST")
        val entities = world.entities.filter {
            it is ItemEntity && it.squaredDistanceTo(cameraPos) < maxDistSquared && filter(it.stack.item, items)
        } as List<ItemEntity>

        computeEntityClusters(entities, clusteredEntities)

        clusteredEntities
    }

    override fun onDisabled() {
        itemEntities.clear()
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        itemEntities.clear()
    }

    @Suppress("unused")
    private val renderHandler = handler<OverlayRenderEvent> { event ->
        renderEnvironmentForGUI {
            itemEntities.mapNotNull { result ->
                val worldPos = result.interpolateCurrentCenterPosition(event.tickDelta)
                val renderPos = WorldToScreen.calculateScreenPos(worldPos.add(renderOffset))
                    ?: return@mapNotNull null
                renderPos to result.stacks
            }.forEachWithSelf { (center, stacks), i, self ->
                val z = 1000.0F * i / self.size
                event.context.drawItemTags(
                    stacks = stacks,
                    centerPos = center.copy(z = z),
                    backgroundColor = backgroundColor.toARGB(),
                    scale = scale,
                    rowLength = rowLength
                )
            }
        }
    }

    private class ClusteredEntities(val entities: List<Entity>, val stacks: List<ItemStack>) {
        fun interpolateCurrentCenterPosition(tickDelta: Float): Vec3d {
            return entities.fold(Vec3d.ZERO) { acc, entity ->
                acc.add(entity.interpolateCurrentPosition(tickDelta))
            }.multiply(1.0 / entities.size)
        }
    }

    @JvmStatic
    private fun computeEntityClusters(entities: List<ItemEntity>, output: ObjectArrayList<ClusteredEntities>) {
        val groups = ObjectArrayList<List<ItemEntity>>()
        val visited = ReferenceOpenHashSet<ItemEntity>()

        for (entity in entities) {
            if (entity in visited) continue

            val radiusSquared = clusterSizeMode.activeChoice.size(entity).sq()

            // `entity` will also be added
            val group = entities.filter { other ->
                other !in visited && entity.squaredDistanceTo(other) < radiusSquared
            }

            visited.addAll(group)
            groups.add(group)
        }

        // Output
        output.clear()
        output.ensureCapacity(groups.size)
        groups.mapTo(output) {
            ClusteredEntities(it, it.mergeStacks())
        }
    }

    /**
     * Merge stacks with same item, order by count desc
     */
    @JvmStatic
    private fun List<ItemEntity>.mergeStacks(): List<ItemStack> {
        val map = Reference2ObjectOpenHashMap<Item, MutableList<ItemStack>>()
        for (itemEntity in this) {
            map.getOrPut(itemEntity.stack.item, ::mutableListOf)
                .add(itemEntity.stack)
        }
        val result = map.values.mapArray { stacks ->
            if (stacks.size == 1) {
                stacks[0]
            } else {
                ItemStack(stacks[0].item, stacks.sumOf { it.count })
            }
        }
        result.sortByDescending { it.count }
        return result.asList()
    }

}
