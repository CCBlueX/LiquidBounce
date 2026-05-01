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

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawLine
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.render.drawLegacy2DMarker
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.entity.cameraDistanceSq
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.math.KeyedAabb
import net.ccbluex.liquidbounce.utils.math.mergeIntersectingAabbsSweep
import net.ccbluex.liquidbounce.utils.math.toVec3f
import net.ccbluex.liquidbounce.utils.math.worldToLocal
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.item.ItemEntity
import net.minecraft.world.entity.projectile.arrow.AbstractArrow.Pickup
import net.minecraft.world.entity.projectile.arrow.Arrow
import net.minecraft.world.entity.projectile.arrow.SpectralArrow
import net.minecraft.world.entity.projectile.arrow.ThrownTrident
import net.minecraft.world.phys.AABB

/**
 * ItemESP module
 *
 * Allows you to see dropped items through walls.
 */

object ModuleItemESP : ClientModule("ItemESP", ModuleCategories.RENDER) {

    override val baseKey: String
        get() = "${ConfigSystem.KEY_PREFIX}.module.itemEsp"

    private val filter by enumChoice("Filter", Filter.BLACKLIST)
    private val items by items("Items", itemSortedSetOf())
    private val maximumDistance by float("MaximumDistance", 128F, 1F..512F)

    val showTracers by boolean("Tracers", false)

    private object ShowArrows : ToggleableValueGroup(this, "ShowArrows", true) {
        val regularArrows by boolean("RegularArrows", true)
        val spectralArrows by boolean("SpectralArrows", true)
        val arrowsWithEffects by boolean("ArrowsWithEffects", true)
    }

    init {
        tree(ShowArrows)
    }

    private val showTridents by boolean("ShowTridents", true)

    private val modes = choices("Mode", 0) {
        arrayOf(
            GlowMode,
//            OutlineMode,
            BoxMode,
            Legacy2DMode,
        )
    }
    private val colorMode = choices("ColorMode", 0) {
        arrayOf(
            GenericStaticColorMode(it, Color4b(255, 179, 72, 255)),
            GenericRainbowColorMode(it)
        )
    }

    // showTracers
    @Suppress("unused")
    private val tracerRenderHandler = handler<WorldRenderEvent> { event ->
        // Check if the tracer option is enabled
        if (!showTracers) return@handler

        renderEnvironmentForWorld(event.matrixStack) {
            val eyeVector = Vec3f.eyeVector(camera)

            val entities = world.entitiesForRendering()
            for (entity in entities) {
                if (!shouldRender(entity)) continue

                val color = getColor()

                // Interpolating the position (motion smoothing)
                val pos = relativeToCamera(entity.interpolateCurrentPosition(event.partialTicks)).toVec3f()

                drawLine(
                    argb = color.argb,
                    p1 = eyeVector,
                    p2 = pos,
                )
            }
        }
    }

    private object BoxMode : Mode("Box") {

        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val box = AABB(-0.125, 0.125, -0.125, 0.125, 0.375, 0.125)
        private val mergeIntersecting by boolean("MergeIntersecting", false)

        private val entities = mutableListOf<Entity>()

        override fun disable() {
            entities.clear()
            super.disable()
        }

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            entities.clear()
            world.entitiesForRendering().filterTo(entities, ::shouldRender)
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (entities.isEmpty()) return@handler

            val matrixStack = event.matrixStack

            val color = getColor()
            val faceColor = color.with(a = 50)
            val outlineColor = color.with(a = 100)

            renderEnvironmentForWorld(matrixStack) {
                if (!mergeIntersecting) {
                    for (entity in entities) {
                        val pos = entity.interpolateCurrentPosition(event.partialTicks)

                        withPositionRelativeToCamera(pos) {
                            drawBox(box, faceColor, outlineColor)
                        }
                    }
                    return@renderEnvironmentForWorld
                }

                val mergedBoxes = mergeIntersectingAabbsSweep(
                    entities.mapToArray { entity ->
                        val pos = entity.interpolateCurrentPosition(event.partialTicks)
                        KeyedAabb(box.move(pos), color)
                    }.asList()
                )

                for ((mergedBox, _) in mergedBoxes) {
                    val (origin, localBox) = mergedBox.worldToLocal()
                    withPositionRelativeToCamera(origin) {
                        drawBox(localBox, faceColor, outlineColor)
                    }
                }
            }
        }
    }

    private object Legacy2DMode : Mode("Legacy2D") {
        override val parent: ModeValueGroup<Mode>
            get() = modes

        private val scale by float("Scale", 0.1f, 0.02f..0.3f)
        private val yOffset by float("YOffset", 0f, -1f..1f)
        private val backgroundAlpha by int("BackgroundAlpha", 150, 0..255)

        private val entities = mutableListOf<Entity>()

        override fun disable() {
            entities.clear()
            super.disable()
        }

        @Suppress("unused")
        private val tickHandler = handler<GameTickEvent> {
            entities.clear()
            world.entitiesForRendering().filterTo(entities, ::shouldRender)
        }

        @Suppress("unused")
        private val renderHandler = handler<WorldRenderEvent> { event ->
            if (entities.isEmpty()) return@handler

            val color = getColor().argb
            val backgroundColor = Color4b.BLACK.with(a = backgroundAlpha).argb

            renderEnvironmentForWorld(event.matrixStack) {
                for (entity in entities) {
                    val pos = entity.interpolateCurrentPosition(event.partialTicks).add(0.0, yOffset.toDouble(), 0.0)

                    drawLegacy2DMarker(
                        pos = pos,
                        entityHeight = entity.boundingBox.ysize,
                        scale = scale,
                        foregroundArgb = color,
                        backgroundArgb = backgroundColor
                    )
                }
            }
        }
    }

    object GlowMode : Mode("Glow") {
        override val parent: ModeValueGroup<Mode>
            get() = modes
    }

    object OutlineMode : Mode("Outline") {
        override val parent: ModeValueGroup<Mode>
            get() = modes
    }

    fun shouldRender(entity: Entity?) : Boolean {
        if (entity == null) return false

        val distanceSq = entity.eyePosition.cameraDistanceSq()
        if (distanceSq > maximumDistance.sq()) return false

        return when (entity) {
            is ItemEntity -> filter(entity.item.item, items)

            is ThrownTrident -> showTridents

            // arrow checks
            // The server never sends the actual pickupType of arrows fired
            // from Infinity-enchanted bows to clients. :(
            // Therefore, those arrows are still rendered as collectible, even though they shouldn't be.
            // The same applies to tridents thrown and arrows fired by players in Creative mode.

            // However, it's not completely useless:
            // arrows shot by mobs such as skeletons and pillagers are not rendered.
            is Arrow if ShowArrows.running && entity.pickup == Pickup.ALLOWED ->
                if (entity.color == -1) ShowArrows.regularArrows else ShowArrows.arrowsWithEffects

            is SpectralArrow if ShowArrows.running && entity.pickup == Pickup.ALLOWED ->
                ShowArrows.spectralArrows

            else -> false
        }
    }

    fun getColor() = this.colorMode.activeMode.getColor(null)
}
