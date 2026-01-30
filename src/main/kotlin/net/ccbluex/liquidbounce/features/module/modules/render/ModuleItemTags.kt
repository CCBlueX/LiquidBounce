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
package net.ccbluex.liquidbounce.features.module.modules.render

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.CurveValue.Axis.Companion.axis
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.computedOn
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemAndComponents
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.entity.cameraDistance
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.item.PreferStackSize
import net.ccbluex.liquidbounce.utils.kotlin.toTypedArray
import net.ccbluex.liquidbounce.utils.math.average
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.minecraft.core.component.DataComponentPatch
import net.minecraft.core.component.DataComponents
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Vector2f

/**
 * ItemTags module
 *
 * Show the names and quantities of items in several boxes.
 */
object ModuleItemTags : ClientModule("ItemTags", ModuleCategories.RENDER) {

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val items by items("Items", itemSortedSetOf())

    private val backgroundColor by color("BackgroundColor", Color4b.DEFAULT_BG_COLOR)
    private val scale = curve(
        "Scale",
        mutableListOf(Vector2f(0f, 1f), Vector2f(200f, 1f)),
        xAxis = "Distance" axis 0f..200f,
        yAxis = "Scale" axis 0.25f..4f,
    )
    private val renderOffset by vec3d("RenderOffset", useLocateButton = false)
    private val rowLength by int("RowLength", 100, 1..100)
    private val preventOverlap by boolean("PreventOverlap", true)
    private val clusterEntities = curve(
        "ClusterEntities",
        mutableListOf(Vector2f(0f, 2f), Vector2f(64f, 16f), Vector2f(128f, 16f), Vector2f(200f, 24f)),
        xAxis = "Distance" axis 0f..200f,
        yAxis = "Size" axis 0.1F..32F,
    )

    private val mergeMode by enumChoice("MergeMode", MergeMode.BY_COMPONENTS)

    private object Shulker : ToggleableValueGroup(this, "Shulker", false) {
        val mergeStacks by boolean("MergeStacks", true)
        val showTitle by boolean("ShowTitle", true)
    }

    init {
        tree(Shulker)
    }

    private val itemStackComparator: Comparator<ItemStack> =
        PreferStackSize.PREFER_MORE.thenComparing { it.item.descriptionId }

    @Suppress("unused")
    private enum class MergeMode(
        override val tag: String,
        val merge: (stacks: Array<ItemStack>) -> Array<ItemStack>,
    ) : Tagged {
        /**
         * Nothing will be merged.
         */
        NONE("None", { stacks ->
            stacks.sortWith(itemStackComparator)
            stacks
        }),

        /**
         * [ItemStack]s with same [Item] will be merged.
         */
        BY_ITEM("ByItem", { stacks ->
            val map = Reference2ObjectOpenHashMap<Item, MutableList<ItemStack>>()
            for (stack in stacks) {
                map.computeIfAbsent(stack.item) { ObjectArrayList() }.add(stack)
            }

            map.values.mapToArray { stacks ->
                if (stacks.size == 1) {
                    stacks[0]
                } else {
                    ItemStack(stacks[0].item, stacks.sumOf { it.count })
                }
            }.apply {
                sortWith(itemStackComparator)
            }
        }),

        /**
         * [ItemStack]s with same [Item] and same [DataComponentPatch] will be merged.
         */
        BY_COMPONENTS("ByComponents", { stacks ->
            val map = Object2IntOpenHashMap<ItemAndComponents>()

            for (stack in stacks) {
                map.addTo(ItemAndComponents(stack), stack.count)
            }

            val iter = map.fastIterator()
            Array(map.size) {
                val entry = iter.next()
                entry.key.toItemStack(entry.intValue)
            }.apply {
                sortWith(itemStackComparator)
            }
        }),
    }

    private val itemEntities by computedOn<GameTickEvent, ObjectArrayList<ClusteredEntitiesRenderState>>(
        initialValue = ObjectArrayList()
    ) { _, groups ->
        @Suppress("UNCHECKED_CAST")
        val entities = world.entitiesForRendering().filter {
            it is ItemEntity && filter(it.item.item, items)
        } as List<ItemEntity>

        groups.clear()
        val visited = ReferenceOpenHashSet<ItemEntity>()
        for (entity in entities) {
            if (entity in visited) continue

            val distance = entity.position().cameraDistance().toFloat()
            val scale = scale.transform(distance)
            if (scale < 0.01f) continue

            val radiusSquared = clusterEntities.transform(distance).sq()

            // `entity` will also be added
            val group = entities.filter { other ->
                other !in visited && entity.distanceToSqr(other) < radiusSquared
            }

            visited.addAll(group)
            groups += ClusteredEntitiesRenderState(
                group,
                mergeMode.merge(group.mapToArray { it.item }).asList(),
                scale,
            )
        }

        groups
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
        for (result in itemEntities) {
            val worldPos = result.interpolateCurrentCenterPosition(event.tickDelta)
            val renderPos = WorldToScreen.calculateScreenPos(worldPos.add(renderOffset)) ?: continue

            event.context.drawItemStackList(result.stacks)
                .centerX(renderPos.x)
                .centerY(renderPos.y)
                .rectBackground(backgroundColor)
                .scale(result.scale)
                .rowLength(rowLength)
                .draw(preventOverlap)

            if (Shulker.enabled) {
                result.stacks.forEach { stack ->
                    val containerComponent = stack[DataComponents.CONTAINER] ?: return@forEach
                    val stacks = containerComponent.nonEmptyStream().toTypedArray()
                    if (stacks.isEmpty()) {
                        return@forEach
                    }

                    event.context.drawItemStackList(if (Shulker.mergeStacks) mergeMode.merge(stacks) else stacks)
                        .title(stack.hoverName.takeIf { Shulker.showTitle })
                        .centerX(renderPos.x)
                        .centerY(renderPos.y)
                        .rectBackground(backgroundColor)
                        .scale(result.scale)
                        .rowLength(rowLength)
                        .draw(preventOverlap)
                }
            }
        }
    }

    private class ClusteredEntitiesRenderState(
        @JvmField val entities: List<Entity>,
        @JvmField val stacks: List<ItemStack>,
        @JvmField val scale: Float,
    ) {
        fun interpolateCurrentCenterPosition(tickDelta: Float): Vec3 {
            return entities.map { entity ->
                entity.interpolateCurrentPosition(tickDelta)
            }.average()
        }
    }

}
