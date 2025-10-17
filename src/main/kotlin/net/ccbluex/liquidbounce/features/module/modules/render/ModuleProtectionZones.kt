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
import com.google.common.collect.Sets
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.minecraft.block.Block
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
 * ProtectionZones module
 *
 * Allows you to see areas protected by protection blocks and suggests optimal placement spots.
 */
object ModuleProtectionZones : ClientModule("ProtectionZones", Category.RENDER) {

    private val DEFAULT_ZONE_FILL = Color4b(0, 255, 0, 51)
    private val DEFAULT_ZONE_OUTLINE = Color4b(0, 255, 0, 255)
    private val DEFAULT_CENTER_OUTLINE = Color4b(0, 255, 207, 255)
    private val DEFAULT_INDICATOR_OUTLINE = Color4b(255, 240, 0, 255)
    private val DEFAULT_INDICATOR_FILL = Color4b(255, 240, 0, 51)
    private const val HIGHLIGHT_RADIUS: Float = 5.0f

    private val protBlocks by blocks(
        "ProtectionBlocks",
        Sets.newConcurrentHashSet<Block>().apply { add(Blocks.EMERALD_BLOCK) },
    ).onChange {
        if (running) {
            onDisabled()
            onEnabled()
        }
        it
    }

    private object Radius : Configurable("ProtectionRadius") {
        val x by int("RadiusX", 20, 1..256, "blocks")
        val z by int("RadiusZ", 20, 1..256, "blocks")
        val y by int("RadiusY", 383, 1..383, "blocks")
    }

    private object Renderer : Configurable("Renderer") {
        val renderLimit by int("RenderLimit", 16, 3..50, "zones")
        val holdBlockToRender by boolean("HoldBlockToRender", false)

        object ProtectionColors : Configurable("ProtectionColors") {
            val zoneFill by color("ZoneFill", DEFAULT_ZONE_FILL)
            val zoneOutline by color("ZoneOutline", DEFAULT_ZONE_OUTLINE)
            val centerZoneOutline by color("CenterZoneOutline", DEFAULT_CENTER_OUTLINE)
        }

        object IndicatorColors : Configurable("IndicatorColors") {
            val indicatorOutline by color("IndicatorOutline", DEFAULT_INDICATOR_OUTLINE)
            val indicatorFill by color("IndicatorFill", DEFAULT_INDICATOR_FILL)
        }

        init {
            treeAll(ProtectionColors, IndicatorColors)
        }
    }

    private object Indicator : Configurable("PlacementIndicator") {
        val snapY by boolean("SnapToY", false)
    }

    init {
        treeAll(Radius, Indicator, Renderer)
    }

    private object BlockTracker : AbstractBlockLocationTracker.BlockPos2State<Block>() {
        override fun getStateFor(pos: BlockPos, state: BlockState): Block? =
            state.block?.takeIf { it in protBlocks }
    }

    override fun onEnabled() {
        ChunkScanner.subscribe(BlockTracker)
    }

    override fun onDisabled() {
        ChunkScanner.unsubscribe(BlockTracker)
    }

    private fun isHoldingProtBlock(): Boolean {
        val player = mc.player ?: return false
        val main = player.mainHandStack.getBlock()
        val off = player.offHandStack.getBlock()
        return (main != null && main in protBlocks)
            || (off != null && off in protBlocks)
    }

    private fun snapToGrid(value: Int, origin: Int, step: Int): Int {
        val stepsFromOrigin = ((value - origin).toDouble() / step).roundToInt()
        return origin + stepsFromOrigin * step
    }

    private fun nearestCenters(
        centers: Sequence<BlockPos>, limit: Int, playerPos: Vec3d
    ): List<BlockPos> {
        if (limit <= 0) return emptyList()

        return Ordering.from<BlockPos> { a, b ->
            fun squaredDist(p: BlockPos): Double {
                val dx = (p.x + 0.5) - playerPos.x
                val dz = (p.z + 0.5) - playerPos.z
                return dx * dx + dz * dz
            }
            squaredDist(a).compareTo(squaredDist(b))
        }.leastOf(centers.iterator(), limit)
    }

    private fun computeZones(centers: List<BlockPos>, world: World): Array<Box> {
        return centers.mapToArray { c ->
            val minY = max(c.y - Radius.y, world.bottomY)
            val maxY = min(c.y + Radius.y, world.topYInclusive)
            Box(
                (c.x - Radius.x).toDouble(),
                minY.toDouble(),
                (c.z - Radius.z).toDouble(),
                (c.x + Radius.x + 1).toDouble(),
                (maxY + 1).toDouble(),
                (c.z + Radius.z + 1).toDouble()
            )
        }
    }

    private fun findHighlightIndex(zones: Array<Box>, playerPos: Vec3d): Int? {
        val r2 = (HIGHLIGHT_RADIUS * HIGHLIGHT_RADIUS).toDouble()
        for ((i, b) in zones.withIndex()) {
            if (b.contains(playerPos)) return null
            if (b.squaredMagnitude(playerPos) <= r2) return i
        }
        return null
    }

    private fun WorldRenderEnvironment.drawZones(
        zones: Array<Box>, centers: List<BlockPos>, highlightIndex: Int?, camOffset: Vec3d
    ) {
        val colors = Renderer.ProtectionColors
        val viewZones = ArrayList<Box>(zones.size)
        val centerBoxes = ArrayList<Box>(centers.size)
        for (i in zones.indices) {
            viewZones += zones[i].offset(camOffset)
            centerBoxes += Box(centers[i]).offset(camOffset)
        }

        for (b in viewZones) drawBox(b, outlineColor = colors.zoneOutline)
        for (c in centerBoxes) drawBox(c, outlineColor = colors.centerZoneOutline)

        if (highlightIndex != null && highlightIndex in viewZones.indices) {
            val highlighted = viewZones[highlightIndex]
            drawBox(highlighted, faceColor = colors.zoneFill)
        }
    }

    private fun WorldRenderEnvironment.drawIndicator(
        centers: List<BlockPos>, zones: Array<Box>, camOffset: Vec3d
    ) {
        if (centers.isEmpty()) return
        val player = mc.player ?: return
        val world = mc.world ?: return

        val playerBlockPos = player.blockPos
        val refCenter = centers.first()

        val stepX = 2 * Radius.x + 1
        val stepZ = 2 * Radius.z + 1

        val snappedX = snapToGrid(value = playerBlockPos.x, origin = refCenter.x, step = stepX)
        val snappedZ = snapToGrid(value = playerBlockPos.z, origin = refCenter.z, step = stepZ)
        val snappedY = (if (Indicator.snapY) refCenter.y else playerBlockPos.y).coerceIn(
            world.bottomY,
            world.topYInclusive
        )

        val indicatorPos = BlockPos(snappedX, snappedY, snappedZ)
        val indicatorCenter = indicatorPos.toCenterPos()
        if (zones.any { it.contains(indicatorCenter) }) return
        val indicatorBox = Box(indicatorPos).offset(camOffset)

        drawBox(
            indicatorBox,
            faceColor = Renderer.IndicatorColors.indicatorFill,
            outlineColor = Renderer.IndicatorColors.indicatorOutline,
        )
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { e ->
        if (BlockTracker.isEmpty()) return@handler
        val holdingProt = isHoldingProtBlock()
        if (Renderer.holdBlockToRender && !holdingProt) return@handler

        val world = mc.world ?: return@handler
        val player = mc.player ?: return@handler

        val centers = nearestCenters(
            centers = BlockTracker.allPositions(),
            limit = Renderer.renderLimit,
            playerPos = player.pos,
        )
        if (centers.isEmpty()) return@handler

        val zones = computeZones(centers, world)
        val highlightIndex = findHighlightIndex(zones, playerPos = player.pos)

        renderEnvironmentForWorld(e.matrixStack) {
            startBatch()
            val camOffset = mc.entityRenderDispatcher.camera.pos.negate()
            drawZones(zones, centers, highlightIndex, camOffset)
            if (holdingProt) {
                drawIndicator(centers, zones, camOffset)
            }
            commitBatch()
        }
    }
}
