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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinLevelRenderer
import net.ccbluex.liquidbounce.render.drawBox
import net.ccbluex.liquidbounce.render.drawBoxSide
import net.ccbluex.liquidbounce.render.drawShape
import net.ccbluex.liquidbounce.render.drawShapeSide
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.render.withPositionRelativeToCamera
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext

/**
 * Block Outline module
 *
 * Changes the way Minecraft highlights blocks.
 *
 * TODO: Implement GUI Information Panel
 *
 * @see MixinLevelRenderer.cancelBlockOutline
 */
object ModuleBlockOutline : ClientModule("BlockOutline", ModuleCategories.RENDER, aliases = listOf("BlockOverlay")) {

    private val sideOnly by boolean("SideOnly", true)
    private val color by color("Color", Color4b(68, 117, 255, 70))
    private val outlineColor by color("Outline", Color4b(68, 117, 255, 150))

    private object Slide : ToggleableValueGroup(this, "Slide", true) {
        val time by int("Time", 150, 1..1000, "ms")
        val easing by easing("Easing", Easing.LINEAR)
    }

    init {
        tree(Slide)
    }

    private var currentPosition: AABB? = null
    private var previousPosition: AABB? = null
    private var lastChange = 0L

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        val target = mc.hitResult
        if (target !is BlockHitResult || target.type == HitResult.Type.MISS) {
            resetPositions()
            return@handler
        }

        val blockPos = target.blockPos
        val blockState = world.getBlockState(blockPos)
        if (blockState.isAir || !world.worldBorder.isWithinBounds(blockPos)) {
            resetPositions()
            return@handler
        }

        val side = target.direction
        val shape = blockState.getShape(this.world, blockPos, CollisionContext.of(mc.cameraEntity!!))
        if (shape.isEmpty) {
            resetPositions()
            return@handler
        }

        val singleBox = shape.toAabbs().singleOrNull()
        if (singleBox == null) {
            resetPositions()

            val localHitPos = target.location - blockPos

            renderEnvironmentForWorld(event.matrixStack) {
                withPositionRelativeToCamera(blockPos) {
                    if (sideOnly) {
                        drawShapeSide(shape, side, localHitPos, color, outlineColor)
                    } else {
                        drawShape(shape, color, outlineColor)
                    }
                }
            }
            return@handler
        }

        val finalPosition = (if (sideOnly) flatBox(singleBox, side) else singleBox).move(blockPos)
        if (currentPosition != finalPosition) {
            previousPosition = currentPosition
            currentPosition = finalPosition
            lastChange = System.currentTimeMillis()
        }

        val renderPosition = if (previousPosition != null && Slide.running) {
            val factor = Slide.easing.getFactor(lastChange, System.currentTimeMillis(), Slide.time.toFloat()).toDouble()

            val previousPosition = previousPosition!!
            AABB(
                Mth.lerp(factor, previousPosition.minX, finalPosition.minX),
                Mth.lerp(factor, previousPosition.minY, finalPosition.minY),
                Mth.lerp(factor, previousPosition.minZ, finalPosition.minZ),
                Mth.lerp(factor, previousPosition.maxX, finalPosition.maxX),
                Mth.lerp(factor, previousPosition.maxY, finalPosition.maxY),
                Mth.lerp(factor, previousPosition.maxZ, finalPosition.maxZ)
            )
        } else {
            finalPosition
        }

        val translatedPosition = renderPosition - event.camera.position()

        renderEnvironmentForWorld(event.matrixStack) {
            if (sideOnly) {
                drawBoxSide(
                    translatedPosition,
                    side,
                    color,
                    outlineColor,
                )
            } else {
                drawBox(
                    translatedPosition,
                    color,
                    outlineColor,
                )
            }
        }
    }

    private fun flatBox(box: AABB, side: Direction) = when (side) {
        Direction.UP -> boxWithBoundsY(box, box.maxY)
        Direction.DOWN -> boxWithBoundsY(box, box.minY)
        Direction.NORTH -> boxWithBoundsZ(box, box.minZ)
        Direction.SOUTH -> boxWithBoundsZ(box, box.maxZ)
        Direction.WEST -> boxWithBoundsX(box, box.minX)
        Direction.EAST -> boxWithBoundsX(box, box.maxX)
    }

    private fun boxWithBoundsX(box: AABB, x: Double) = AABB(
        x,
        box.minY,
        box.minZ,
        x,
        box.maxY,
        box.maxZ
    )

    private fun boxWithBoundsY(box: AABB, y: Double) = AABB(
        box.minX,
        y,
        box.minZ,
        box.maxX,
        y,
        box.maxZ
    )

    private fun boxWithBoundsZ(box: AABB, z: Double) = AABB(
        box.minX,
        box.minY,
        z,
        box.maxX,
        box.maxY,
        z
    )

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        resetPositions()
        lastChange = System.currentTimeMillis()
    }

    private fun resetPositions() {
        currentPosition = null
        previousPosition = null
    }

}
