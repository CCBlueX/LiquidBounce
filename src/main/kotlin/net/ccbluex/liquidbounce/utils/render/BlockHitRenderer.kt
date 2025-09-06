package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.BoxRenderer
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.drawBoxSide
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.ccbluex.liquidbounce.utils.block.boxWithBoundsX
import net.ccbluex.liquidbounce.utils.block.boxWithBoundsY
import net.ccbluex.liquidbounce.utils.block.boxWithBoundsZ
import net.ccbluex.liquidbounce.utils.math.Easing
import net.minecraft.block.ShapeContext
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.shape.VoxelShape

class BlockHitRenderer(
    module: ClientModule
) : ToggleableConfigurable(module, "BlockHitRenderer", true) {
    private val sideOnly by boolean("SideOnly", true)

    private val slideTime by int("SlideTime", 150, 1..1000, "ms")
    private val alpha by int("Alpha", 70, 0..255)
    private val outlineAlpha by int("OutlineAlpha", 150, 0..255)

    private val colorMode = choices(this, "ColorMode", 2) {
        arrayOf(
            GenericStaticColorMode(it, Color4b(68, 117, 255)),
            GenericRainbowColorMode(it),
            GenericSyncColorMode(it),
        )
    }

    val easing by easing("Easing", Easing.LINEAR)

    private var currentPosition: Box? = null
    private var previousPosition: Box? = null
    private var lastChange: Long = 0L

    fun render(
        enable: Boolean,
        event: WorldRenderEvent,
        hitResult: HitResult? = null,
        box: Box? = null,
        side: Direction? = null
    ) {
        if (!enable || (hitResult == null && box == null)) {
            resetPositions()
            return
        }

        val finalPosition = if (hitResult != null && hitResult is BlockHitResult) {
            val blockPos = hitResult.blockPos
            val blockState = world.getBlockState(blockPos)
            if (blockState.isAir || !world.worldBorder.contains(blockPos)) {
                resetPositions()
                return
            }
            val hitSide = hitResult.side
            val shape = blockState.getOutlineShape(world, blockPos, ShapeContext.of(mc.cameraEntity))
            (if (sideOnly) flatBox(shape, hitSide) else shape.boundingBox).offset(blockPos)
        } else {
            box ?: return
        }

        if (currentPosition != finalPosition) {
            previousPosition = currentPosition
            currentPosition = finalPosition
            lastChange = System.currentTimeMillis()
        }

        val renderPosition = if (previousPosition != null) {
            val factor = easing.getFactor(lastChange, System.currentTimeMillis(), slideTime.toFloat()).toDouble()
            val previousPosition = previousPosition!!
            Box(
                MathHelper.lerp(factor, previousPosition.minX, finalPosition.minX),
                MathHelper.lerp(factor, previousPosition.minY, finalPosition.minY),
                MathHelper.lerp(factor, previousPosition.minZ, finalPosition.minZ),
                MathHelper.lerp(factor, previousPosition.maxX, finalPosition.maxX),
                MathHelper.lerp(factor, previousPosition.maxY, finalPosition.maxY),
                MathHelper.lerp(factor, previousPosition.maxZ, finalPosition.maxZ)
            )
        } else {
            finalPosition
        }

        val translatedPosition = renderPosition.offset(mc.entityRenderDispatcher.camera.pos.negate())
        val baseColor = colorMode.activeChoice.getColor(Unit)
        val fillColor = baseColor.withAlpha(alpha)
        val outline = baseColor.withAlpha(outlineAlpha)

        renderEnvironmentForWorld(event.matrixStack) {
            if (side != null && sideOnly) {
                drawBoxSide(translatedPosition, side, fillColor, outline)
            } else {
                BoxRenderer.drawWith(this) {
                    drawBox(translatedPosition, fillColor, outline)
                }
            }
        }
    }

    private fun flatBox(shape: VoxelShape, side: Direction) = when (side) {
        Direction.UP -> shape.boxWithBoundsY(shape.getMax(Direction.Axis.Y))
        Direction.DOWN -> shape.boxWithBoundsY(shape.getMin(Direction.Axis.Y))
        Direction.NORTH -> shape.boxWithBoundsZ(shape.getMin(Direction.Axis.Z))
        Direction.SOUTH -> shape.boxWithBoundsZ(shape.getMax(Direction.Axis.Z))
        Direction.WEST -> shape.boxWithBoundsX(shape.getMin(Direction.Axis.X))
        Direction.EAST -> shape.boxWithBoundsX(shape.getMax(Direction.Axis.X))
    }

    fun resetPositions() {
        currentPosition = null
        previousPosition = null
    }
}
