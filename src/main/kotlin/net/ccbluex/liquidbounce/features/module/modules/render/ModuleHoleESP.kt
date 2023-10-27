/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.PlayerTickEvent
import net.ccbluex.liquidbounce.event.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.block.MovableRegionScanner
import net.ccbluex.liquidbounce.utils.block.Region
import net.ccbluex.liquidbounce.utils.block.WorldChangeNotifier
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3i

/**
 * HoleESP module
 *
 * Detects and displays safe spots for Crystal PvP.
 */

object ModuleHoleESP : Module("HoleESP", Category.RENDER) {

    private val modes = choices("Mode", Box, arrayOf(Box))

    var horizontalDistance by int("HorizontalScanDistance", 16, 4..100)
    var verticalDistance by int("VerticalScanDistance", 16, 4..100)

    val flattenMovement by boolean("FlattenMovement", true)

    val holes = HashMap<BlockPos, HoleQuality>()
    val movableRegionScanner = MovableRegionScanner()

    private object Box : Choice("Box") {

        override val parent: ChoiceConfigurable
            get() = modes

        private val outline by boolean("Outline", true)

        private val box = Box(0.0, 0.0, 0.0, 1.0, 1.0, 1.0)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val markedBlocks = holes.entries

            renderEnvironmentForWorld(matrixStack) {
                for ((pos, quality) in markedBlocks) {
                    val vec3 = Vec3(pos)

                    val baseColor = quality.baseColor
                    val outlineColor = quality.outlineColor

                    withPosition(vec3) {
                        withColor(baseColor) {
                            drawSolidBox(box)
                        }

                        if (outline) {
                            withColor(outlineColor) {
                                drawOutlinedBox(box)
                            }
                        }
                    }
                }
            }
        }

    }

    val movementHandler = handler<PlayerTickEvent> { event ->
        this.updateScanRegion()
    }

    private fun flatten(pos: BlockPos): BlockPos {
        if (!this.flattenMovement) {
            return pos
        }

        val flattenXZ = this.horizontalDistance < 8
        val flattenY = this.verticalDistance < 5

        val maskXZ = if (flattenXZ) 3.inv() else 0.inv()
        val maskY = if (flattenY) 3.inv() else 0.inv()

        return BlockPos(pos.x and maskXZ, pos.y and maskY, pos.z and maskXZ)
    }

    private fun updateScanRegion() {
        synchronized(this.holes) {
            val changedAreas = this.movableRegionScanner.moveRegion(
                Region.quadAround(
                    player.blockPos,
                    this.horizontalDistance,
                    this.verticalDistance
                )
            )

            val region = this.movableRegionScanner.currentRegion

            // Remove blocks out of the area
            holes.entries.removeIf { it.key !in region }

            changedAreas?.forEach(this::updateRegion)
        }
    }

    private fun updateRegion(region: Region) {
        val world = world

        region.forEachCoordinate { x, y, z ->
            val pos = BlockPos(x, y, z)
            val blockState = world.getBlockState(pos)

            if (!blockState.isAir && blockState.getCollisionShape(world, pos).boundingBoxes.any { it.maxY >= 1 }) {
                return@forEachCoordinate
            }

            // Can you actually go inside that hole?
            if (arrayOf(pos.up(1), pos.up(2)).any { !world.getBlockState(it).getCollisionShape(world, it).isEmpty }) {
                return@forEachCoordinate
            }

            val positionsToScan = arrayOf(
                BlockPos(x + 1, y, z),
                BlockPos(x - 1, y, z),
                BlockPos(x, y, z + 1),
                BlockPos(x, y, z - 1),
                BlockPos(x, y - 1, z)
            )

            var unsafeBlocks = 0

            for (scanPos in positionsToScan) {
                val scanState = world.getBlockState(scanPos)

                val isUnsafe = when (scanState.block) {
                    Blocks.BEDROCK -> false
                    Blocks.OBSIDIAN -> true
                    else -> return@forEachCoordinate
                }

                unsafeBlocks += if (isUnsafe) 1 else 0
            }

            val holeQuality = when (unsafeBlocks) {
                0 -> HoleQuality.SAFE
                in 1..4 -> HoleQuality.MEDIOCRE
                else -> HoleQuality.UNSAFE
            }

            this.holes[pos] = holeQuality
        }
    }

    override fun enable() {
        this.movableRegionScanner.clearRegion()

        updateScanRegion()

        WorldChangeNotifier.subscribe(InvalidationHook)
    }

    override fun disable() {
        WorldChangeNotifier.unsubscribe(InvalidationHook)

        holes.clear()
    }

    object InvalidationHook : WorldChangeNotifier.WorldChangeSubscriber {
        override fun invalidate(region: Region, rescan: Boolean) {
            // Check if the region intersects. Otherwise calling region.intersection would be unsafe
            if (!region.intersects(movableRegionScanner.currentRegion)) {
                return
            }

            val intersection = region.intersection(movableRegionScanner.currentRegion)

            val rescanRegion = Region(
                intersection.from.subtract(Vec3i(1, 1, 1)),
                intersection.to.add(Vec3i(1, 1, 1))
            )

            synchronized(holes) {
                holes.entries.removeIf { it.key in rescanRegion }

                if (rescan) {
                    updateRegion(rescanRegion)
                }
            }
        }

        override fun invalidateEverything() {
            synchronized(movableRegionScanner) {
                movableRegionScanner.clearRegion()
            }
            synchronized(holes) {
                holes.clear()
            }
        }

    }

    enum class HoleQuality(r: Int, g: Int, b: Int) {
        SAFE(0x20, 0xC2, 0x06),
        MEDIOCRE(0xD5, 0x96, 0x00),
        UNSAFE(0xD7, 0x09, 0x09);

        val baseColor: Color4b = Color4b(r, g, b, 50)
        val outlineColor: Color4b = Color4b(r, g, b, 100)
    }

}
