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
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.math.Easing
import net.ccbluex.liquidbounce.utils.math.minus
import net.minecraft.core.Direction
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape

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
        val box = blockState.getShape(this.world, blockPos, CollisionContext.of(mc.cameraEntity!!))
        val finalPosition = (if (sideOnly) flatBox(box, side) else box.bounds()).move(blockPos)
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

        val translatedPosition = renderPosition - (mc.entityRenderDispatcher
            .camera?.position() ?: return@handler)

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

    private fun flatBox(shape: VoxelShape, side: Direction) = when (side) {
        Direction.UP -> shape.boxWithBoundsY(shape.max(Direction.Axis.Y))
        Direction.DOWN -> shape.boxWithBoundsY(shape.min(Direction.Axis.Y))
        Direction.NORTH -> shape.boxWithBoundsZ(shape.min(Direction.Axis.Z))
        Direction.SOUTH -> shape.boxWithBoundsZ(shape.max(Direction.Axis.Z))
        Direction.WEST -> shape.boxWithBoundsX(shape.min(Direction.Axis.X))
        Direction.EAST -> shape.boxWithBoundsX(shape.max(Direction.Axis.X))
    }

    private fun VoxelShape.boxWithBoundsX(x: Double) = AABB(
        x,
        min(Direction.Axis.Y),
        min(Direction.Axis.Z),
        x,
        max(Direction.Axis.Y),
        max(Direction.Axis.Z)
    )

    private fun VoxelShape.boxWithBoundsY(y: Double) = AABB(
        min(Direction.Axis.X),
        y,
        min(Direction.Axis.Z),
        max(Direction.Axis.X),
        y,
        max(Direction.Axis.Z)
    )

    private fun VoxelShape.boxWithBoundsZ(z: Double) = AABB(
        min(Direction.Axis.X),
        min(Direction.Axis.Y),
        z,
        max(Direction.Axis.X),
        max(Direction.Axis.Y),
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
