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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PlayerPostTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.block.MovableRegionScanner
import net.ccbluex.liquidbounce.utils.block.Region
import net.ccbluex.liquidbounce.utils.block.WorldChangeNotifier
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.minecraft.block.Blocks
import net.minecraft.util.math.*
import kotlin.math.max

/**
 * HoleESP module
 *
 * Detects and displays safe spots for Crystal PvP.
 */
@IncludeModule
object ModuleHoleESP : Module("HoleESP", Category.RENDER) {

    private val modes = choices("Mode", GlowingPlane, arrayOf(BoxChoice, GlowingPlane))

    var horizontalDistance by int("HorizontalScanDistance", 16, 4..100)
    var verticalDistance by int("VerticalScanDistance", 16, 4..100)

    val flattenMovement by boolean("FlattenMovement", true)

    val holes = HashMap<BlockPos, HoleQuality>()
    val movableRegionScanner = MovableRegionScanner()

    val distanceFade by float("DistanceFade", 0.3f, 0f..1f)

    private object BoxChoice : Choice("Box") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        private val outline by boolean("Outline", true)

        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val markedBlocks = holes.entries

            renderEnvironmentForWorld(matrixStack) {
                for ((pos, quality) in markedBlocks) {
                    val fade = calculateFade(pos)
                    val baseColor = quality.baseColor.fade(fade)
                    val outlineColor = quality.outlineColor.fade(fade)

                    withPositionRelativeToCamera(pos.toVec3d()) {
                        withColor(baseColor) {
                            drawSolidBox(FULL_BOX)
                        }

                        if (outline) {
                            withColor(outlineColor) {
                                drawOutlinedBox(FULL_BOX)
                            }
                        }
                    }
                }
            }
        }

    }

    private object GlowingPlane : Choice("GlowingPlane") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        val outline by boolean("Outline", true)

        val glowHeightSetting by float("GlowHeight", 0.7f, 0f..1f)

        @Suppress("unused")
        val renderHandler = handler<WorldRenderEvent> { event ->
            val matrixStack = event.matrixStack
            val markedBlocks = holes.entries

            val glowHeight = glowHeightSetting.toDouble()
            renderEnvironmentForWorld(matrixStack) {
                withDisabledCull {
                    for ((pos, quality) in markedBlocks) {
                        val fade = calculateFade(pos)

                        val baseColor = quality.baseColor.fade(fade)
                        val transparentColor = baseColor.alpha(0)
                        val outlineColor = quality.outlineColor.fade(fade)

                        withPositionRelativeToCamera(pos.toVec3d()) {
                            withColor(baseColor) {
                                drawSideBox(FULL_BOX, Direction.DOWN)
                            }

                            if (outline) {
                                withColor(outlineColor) {
                                    drawSideBox(FULL_BOX, Direction.DOWN, onlyOutline = true)
                                }
                            }

                            drawGradientSides(glowHeight, baseColor, transparentColor, FULL_BOX)
                        }
                    }
                }
            }
        }
    }

    @Suppress("unused")
    val movementHandler = handler<PlayerPostTickEvent> { event ->
        this.updateScanRegion()
    }

    private fun calculateFade(pos: BlockPos): Float {
        if (distanceFade == 0f)
            return 1f

        val verticalDistanceFraction = (player.pos.y - pos.y) / verticalDistance
        val horizontalDistanceFraction =
            Vec3d(player.pos.x - pos.x, 0.0, player.pos.z - pos.z).length() / horizontalDistance

        val fade = (1 - max(verticalDistanceFraction, horizontalDistanceFraction)) / distanceFade

        return fade.coerceIn(0.0, 1.0).toFloat()
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

            changedAreas.forEach(this::updateRegion)
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
        WorldChangeNotifier.subscribe(InvalidationHook)

        this.movableRegionScanner.clearRegion()

        updateScanRegion()

    }

    override fun disable() {
        WorldChangeNotifier.unsubscribe(InvalidationHook)
        holes.clear()
    }

    object InvalidationHook : WorldChangeNotifier.WorldChangeSubscriber {
        override fun invalidate(region: Region, rescan: Boolean) {
            // Check if the region intersects. Otherwise, calling region.intersection would be unsafe
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
