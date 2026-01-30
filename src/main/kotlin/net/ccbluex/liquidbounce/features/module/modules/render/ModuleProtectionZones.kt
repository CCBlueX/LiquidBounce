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

import com.google.common.collect.Ordering
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.fastutil.synchronized
import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * ProtectionZones module
 *
 * Allows you to see areas protected by protection blocks and suggests optimal placement spots.
 */
object ModuleProtectionZones : ClientModule("ProtectionZones", ModuleCategories.RENDER) {

    private val DEFAULT_ZONE_FILL = Color4b(0, 255, 0, 51)
    private val DEFAULT_ZONE_OUTLINE = Color4b(0, 255, 0, 255)
    private val DEFAULT_CENTER_OUTLINE = Color4b(0, 255, 207, 255)
    private val DEFAULT_INDICATOR_OUTLINE = Color4b(255, 240, 0, 255)
    private val DEFAULT_INDICATOR_FILL = Color4b(255, 240, 0, 51)
    private const val HIGHLIGHT_RADIUS: Float = 5.0f

    private val protBlocks by blocks(
        "ProtectionBlocks",
        blockSortedSetOf(Blocks.EMERALD_BLOCK).synchronized(),
    ).onChange {
        if (running) {
            onDisabled()
            onEnabled()
        }
        it
    }

    private object Radius : ValueGroup("ProtectionRadius") {
        val x by int("RadiusX", 20, 1..256, "blocks")
        val z by int("RadiusZ", 20, 1..256, "blocks")
        val y by int("RadiusY", 383, 1..383, "blocks")
    }

    private object Renderer : ValueGroup("Renderer") {
        val renderLimit by int("RenderLimit", 16, 3..50, "zones")
        val holdBlockToRender by boolean("HoldBlockToRender", false)

        object ProtectionColors : ValueGroup("ProtectionColors") {
            val zoneFill by color("ZoneFill", DEFAULT_ZONE_FILL)
            val zoneOutline by color("ZoneOutline", DEFAULT_ZONE_OUTLINE)
            val centerZoneOutline by color("CenterZoneOutline", DEFAULT_CENTER_OUTLINE)
        }

        object IndicatorColors : ValueGroup("IndicatorColors") {
            val indicatorOutline by color("IndicatorOutline", DEFAULT_INDICATOR_OUTLINE)
            val indicatorFill by color("IndicatorFill", DEFAULT_INDICATOR_FILL)
        }

        init {
            treeAll(ProtectionColors, IndicatorColors)
        }
    }

    private object Indicator : ValueGroup("PlacementIndicator") {
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
        val main = player.mainHandItem.getBlock()
        val off = player.offhandItem.getBlock()
        return (main != null && main in protBlocks)
            || (off != null && off in protBlocks)
    }

    private fun snapToGrid(value: Int, origin: Int, step: Int): Int {
        val stepsFromOrigin = ((value - origin).toDouble() / step).roundToInt()
        return origin + stepsFromOrigin * step
    }

    private fun nearestCenters(
        centers: Sequence<BlockPos>, limit: Int, playerPos: Vec3
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

    private fun computeZones(centers: List<BlockPos>, world: Level): Array<AABB> {
        return centers.mapToArray { c ->
            val minY = max(c.y - Radius.y, world.minY)
            val maxY = min(c.y + Radius.y, world.maxY)
            AABB(
                (c.x - Radius.x).toDouble(),
                minY.toDouble(),
                (c.z - Radius.z).toDouble(),
                (c.x + Radius.x + 1).toDouble(),
                (maxY + 1).toDouble(),
                (c.z + Radius.z + 1).toDouble()
            )
        }
    }

    private fun findHighlightIndex(zones: Array<AABB>, playerPos: Vec3): Int? {
        val r2 = (HIGHLIGHT_RADIUS * HIGHLIGHT_RADIUS).toDouble()
        for ((i, b) in zones.withIndex()) {
            if (b.contains(playerPos)) return null
            if (b.distanceToSqr(playerPos) <= r2) return i
        }
        return null
    }

    private fun WorldRenderEnvironment.drawZones(
        zones: Array<AABB>, centers: List<BlockPos>, highlightIndex: Int?, camOffset: Vec3
    ) {
        val colors = Renderer.ProtectionColors
        val viewZones = ArrayList<AABB>(zones.size)
        val centerBoxes = ArrayList<AABB>(centers.size)
        for (i in zones.indices) {
            viewZones += zones[i].move(camOffset)
            centerBoxes += AABB(centers[i]).move(camOffset)
        }

        for (b in viewZones) drawBox(b, outlineColor = colors.zoneOutline)
        for (c in centerBoxes) drawBox(c, outlineColor = colors.centerZoneOutline)

        if (highlightIndex != null && highlightIndex in viewZones.indices) {
            val highlighted = viewZones[highlightIndex]
            drawBox(highlighted, faceColor = colors.zoneFill)
        }
    }

    private fun WorldRenderEnvironment.drawIndicator(
        centers: List<BlockPos>, zones: Array<AABB>, camOffset: Vec3
    ) {
        if (centers.isEmpty()) return
        val player = mc.player ?: return
        val world = mc.level ?: return

        val playerBlockPos = player.blockPosition()
        val refCenter = centers.first()

        val stepX = 2 * Radius.x + 1
        val stepZ = 2 * Radius.z + 1

        val snappedX = snapToGrid(value = playerBlockPos.x, origin = refCenter.x, step = stepX)
        val snappedZ = snapToGrid(value = playerBlockPos.z, origin = refCenter.z, step = stepZ)
        val snappedY = (if (Indicator.snapY) refCenter.y else playerBlockPos.y).coerceIn(
            world.minY,
            world.maxY
        )

        val indicatorPos = BlockPos(snappedX, snappedY, snappedZ)
        val indicatorCenter = indicatorPos.center
        if (zones.any { it.contains(indicatorCenter) }) return
        val indicatorBox = AABB(indicatorPos).move(camOffset)

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

        val world = mc.level ?: return@handler
        val player = mc.player ?: return@handler

        val centers = nearestCenters(
            centers = BlockTracker.allPositions(),
            limit = Renderer.renderLimit,
            playerPos = player.position(),
        )
        if (centers.isEmpty()) return@handler

        val zones = computeZones(centers, world)
        val highlightIndex = findHighlightIndex(zones, playerPos = player.position())

        renderEnvironmentForWorld(e.matrixStack) {
            startBatch()
            val camOffset = mc.entityRenderDispatcher.camera?.position()?.reverse() ?: return@handler
            drawZones(zones, centers, highlightIndex, camOffset)
            if (holdingProt) {
                drawIndicator(centers, zones, camOffset)
            }
            commitBatch()
        }
    }
}
