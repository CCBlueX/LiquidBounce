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

import com.google.common.collect.Ordering
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBoxes
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.minecraft.block.BlockState
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.world.World
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ProtectionViewer module
 *
 * Allows you to view protection zones and helps you place protection blocks correctly.
 * Useful on survival servers with protection plugins (e.g., ProtectedStones).
 */
object ModuleProtectionViewer : ClientModule("ProtectionViewer", Category.RENDER) {

    private val protectionBlocks by blocks("Protection Blocks", mutableSetOf(Blocks.EMERALD_BLOCK)).onChange {
        if (running) {
            onDisabled(); onEnabled()
        }
        it
    }

    private object ProtectionRadiusConfig : Configurable("Protection Radius") {
        val xRadius by int("RadiusX", 20, 1..256, "blocks")
        val zRadius by int("RadiusZ", 20, 1..256, "blocks")
        val yRadius by int("RadiusY", 383, 1..383, "blocks") // effectively "full height" default
    }

    private object RenderConfig : Configurable("Renderer") {
        val renderLimit by int("RenderLimit", 16, 3..50, "zones")
        val requireHoldingProtectionBlock by boolean("RequireHoldingProtectionBlock", false)
        val highlightRadius by float("HighlightRadius", 3.0f, 1.0f..16.0f, "blocks")
        val indicatorSnapToY by boolean("IndicatorSnapToY", false)

        object Colors : Configurable("Colors") {
            val boundaryFill by color("BoundaryHighlight", Color4b(0, 255, 0, 51))
            val boundaryOutline by color("BoundaryOutline", Color4b(0, 255, 0, 255))
            val centerOutline by color("CenterOutline", Color4b(0, 255, 207, 255))
            val indicatorOutline by color("IndicatorOutline", Color4b(255, 240, 0, 255))
        }

        init {
            tree(Colors)
        }
    }

    init {
        treeAll(ProtectionRadiusConfig, RenderConfig)
    }

    private object ProtectionBlockTracker : AbstractBlockLocationTracker.BlockPos2State<Unit>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): Unit? =
            if (state.block in protectionBlocks) Unit else null
    }

    override fun onEnabled() {
        ChunkScanner.subscribe(ProtectionBlockTracker)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(ProtectionBlockTracker)
    }

    private fun isHoldingProtBlock(): Boolean {
        val player = mc.player ?: return false
        val main = player.mainHandStack.item
        val off = player.offHandStack.item
        return protectionBlocks.any { blockItem ->
            val item = blockItem.asItem()
            item == main || item == off
        }
    }

    private fun snapAxisToGrid(value: Int, origin: Int, step: Int): Int {
        val stepsFromOrigin = ((value - origin).toDouble() / step).roundToInt()
        return origin + stepsFromOrigin * step
    }

    private fun nearestCentersByDistance(
        centers: Sequence<BlockPos>, limit: Int, playerPos: Vec3d
    ): List<BlockPos> {
        if (limit <= 0) return emptyList()
        val compareBySquaredDist = Comparator<BlockPos> { a, b ->
            fun squaredDist(p: BlockPos): Double {
                val dx = (p.x + 0.5) - playerPos.x

                val dz = (p.z + 0.5) - playerPos.z
                return dx * dx + dz * dz
            }
            squaredDist(a).compareTo(squaredDist(b))
        }
        return Ordering.from(compareBySquaredDist).leastOf(centers.iterator(), limit)
    }

    private fun computeBounds(centers: List<BlockPos>, world: World): ArrayList<Box> {
        val out = ArrayList<Box>(centers.size)
        with(ProtectionRadiusConfig) {
            for (c in centers) {
                val minY = max(c.y - yRadius, world.bottomY)
                val maxY = min(c.y + yRadius, world.topYInclusive)
                out += Box(
                    (c.x - xRadius).toDouble(),
                    minY.toDouble(),
                    (c.z - zRadius).toDouble(),
                    (c.x + xRadius + 1).toDouble(),
                    (maxY + 1).toDouble(),
                    (c.z + zRadius + 1).toDouble()
                )
            }
        }
        return out
    }

    private fun findHighlightIndex(bounds: List<Box>, playerPos: Vec3d, highlightRadius: Float): Int? {
        val radiusSquared = (highlightRadius * highlightRadius).toDouble()
        for ((i, b) in bounds.withIndex()) {
            if (b.contains(playerPos)) return null
            if (b.squaredMagnitude(playerPos) <= radiusSquared) return i
        }
        return null
    }

    private fun WorldRenderEnvironment.renderZones(
        bounds: List<Box>, centers: List<BlockPos>, highlightIndex: Int?, cameraPosNegated: Vec3d
    ) {
        val colors = RenderConfig.Colors
        val viewBounds = ArrayList<Box>(bounds.size)
        val centerBoxes = ArrayList<Box>(centers.size)
        for (i in bounds.indices) {
            viewBounds += bounds[i].offset(cameraPosNegated)
            centerBoxes += Box(centers[i]).offset(cameraPosNegated)
        }

        drawBoxes {
            for (b in viewBounds) drawBox(b, Color4b.TRANSPARENT, colors.boundaryOutline)
            for (c in centerBoxes) drawBox(c, Color4b.TRANSPARENT, colors.centerOutline)



            if (highlightIndex != null && highlightIndex in viewBounds.indices) {
                val highlighted = viewBounds[highlightIndex]
                RenderSystem.enableDepthTest()
                RenderSystem.depthMask(false)
                RenderSystem.enablePolygonOffset()
                RenderSystem.polygonOffset(1f, 1f)
                try {
                    drawBox(highlighted, colors.boundaryFill, Color4b.TRANSPARENT)
                } finally {
                    RenderSystem.disablePolygonOffset()
                    RenderSystem.depthMask(true)
                    RenderSystem.disableDepthTest()
                }
            }
        }
    }

    private fun WorldRenderEnvironment.renderIndicator(
        centers: List<BlockPos>, cameraPosNegated: Vec3d
    ) {
        if (centers.isEmpty()) return
        val player = mc.player ?: return
        val world = mc.world ?: return

        val playerBlockPos = player.blockPos
        val refCenter = centers.first()

        val stepX = 2 * ProtectionRadiusConfig.xRadius + 1
        val stepZ = 2 * ProtectionRadiusConfig.zRadius + 1

        val snappedX = snapAxisToGrid(value = playerBlockPos.x, origin = refCenter.x, step = stepX)
        val snappedZ = snapAxisToGrid(value = playerBlockPos.z, origin = refCenter.z, step = stepZ)
        val snappedY = (if (RenderConfig.indicatorSnapToY) refCenter.y else playerBlockPos.y).coerceIn(
            world.bottomY,
            world.topYInclusive
        )

        val indicatorPos = BlockPos(snappedX, snappedY, snappedZ)
        val indicatorBox = Box(indicatorPos).offset(cameraPosNegated)
        drawBoxes {
            drawBox(indicatorBox, Color4b.TRANSPARENT, RenderConfig.Colors.indicatorOutline)
        }
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { e ->
        if (ProtectionBlockTracker.isEmpty()) return@handler
        val holdingProtBlock = isHoldingProtBlock()
        if (RenderConfig.requireHoldingProtectionBlock && !holdingProtBlock) return@handler

        val world = mc.world ?: return@handler
        val player = mc.player ?: return@handler

        val centers = nearestCentersByDistance(
            centers = ProtectionBlockTracker.allPositions(),
            limit = RenderConfig.renderLimit,
            playerPos = player.pos,
        )
        if (centers.isEmpty()) return@handler

        val bounds = computeBounds(centers, world)
        val highlightIndex = findHighlightIndex(
            bounds, playerPos = player.pos, highlightRadius = RenderConfig.highlightRadius
        )

        renderEnvironmentForWorld(e.matrixStack) {
            val cameraPosNegated = mc.entityRenderDispatcher.camera.pos.negate()
            renderZones(bounds, centers, highlightIndex, cameraPosNegated)
            if (holdingProtBlock) {
                renderIndicator(centers, cameraPosNegated)
            }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        if (running) {
            onDisabled(); onEnabled()
        }
    }
}
