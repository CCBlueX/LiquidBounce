@file:Suppress("MemberVisibilityCanBePrivate", "LongParameterList", "LongMethod")

package net.ccbluex.liquidbounce.features.module.modules.render

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.GenericCustomColorMode
import net.ccbluex.liquidbounce.render.GenericRainbowColorMode
import net.ccbluex.liquidbounce.render.GenericStaticColorMode
import net.ccbluex.liquidbounce.render.GenericSyncColorMode
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Color4b.Companion.hslToRgb
import net.ccbluex.liquidbounce.render.renderEnvironmentForWorld
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.BufferRenderer
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.joml.Matrix4f
import java.util.Random
import kotlin.math.*

object ModuleLineGlyphs : ClientModule("LineGlyphs", Category.RENDER) {


    private val slowSpeed by boolean("Slow Speed", false)
    private val glyphCount by int("Glyphs Count", 70, 10..200)
    private val distance by int("Distance",1,1..20)
    private val colorMode = choices(this, "ColorMode") {
        arrayOf(
            GenericSyncColorMode(it),
            GenericCustomColorMode(it, Color4b.LIQUID_BOUNCE, Color4b.CYAN),
            GenericStaticColorMode(it, Color4b(0, 128, 255, 255)),
            GenericRainbowColorMode(it),
        )
    }
    private val random = Random(93882L)
    private val glyphVectorGenerators = mutableListOf<GlyphVectorGenerator>()

    private val random360X: Int get() = random.nextInt(4) * 90
    private val random360Y: Int get() = (random.nextInt(4) - 2) * 90

    override fun onEnabled() {
        glyphVectorGenerators.clear()
    }

    override fun onDisabled() {
        glyphVectorGenerators.clear()
    }

    private fun lineMovementSteps() = intArrayOf(0, 3)
    private fun lineStepsRange() = intArrayOf(7, 12)
    private fun spawnRange() = intArrayOf(6, 24, 0, 12)

    private fun maxGlyphCount() = glyphCount

    private fun Int.mod360(): Int = ((this % 360) + 360) % 360

    private fun getRandom90Rotation(previous: IntArray): IntArray {
        val newA = (previous[0] + if (random.nextBoolean()) 90 else -90).mod360()
        val newB = (previous[1] + if (random.nextBoolean()) 90 else -90).mod360()
        return intArrayOf(newA, newB)
    }
    private fun calcLineWidth(glyph: GlyphVectorGenerator): Float {
        val camera = mc.gameRenderer.camera
        val camX = camera.pos.x
        val camY = camera.pos.y
        val camZ = camera.pos.z

        val furthest = glyph.vectorList.maxByOrNull {
            val dx = it.x - camX
            val dy = it.y - camY
            val dz = it.z - camZ
            (dx * dx + dy * dy + dz * dz)
        } ?: return 1f

        val dx = furthest.x - camX
        val dy = furthest.y - camY
        val dz = furthest.z - camZ
        val dist = sqrt(dx * dx + dy * dy + dz * dz)
        val factor = 1.0 - dist / distance
        val clamped = max(min(factor.toFloat(), 1f), 0f)
        return 0.0001f + 3.0f * clamped
    }

    private fun offsetFromRotation(base: BlockPos, rotation: IntArray, step: Int): BlockPos {
        val yawRad = Math.toRadians(rotation[0].toDouble())
        val pitchRad = Math.toRadians(rotation[1].toDouble())
        val horizontalStep = step * cos(pitchRad)
        val xOffset = (-sin(yawRad) * horizontalStep).toInt()
        val yOffset = (sin(pitchRad) * step).toInt()
        val zOffset = (cos(yawRad) * horizontalStep).toInt()
        return BlockPos(base.x + xOffset, base.y + yOffset, base.z + zOffset)
    }

    private fun calculateMoveAdvance(totalTicks: Int, ticksRemaining: Int, partialTicks: Float): Float {
        val fraction = 1.0f - (ticksRemaining - partialTicks) / totalTicks
        return min(max(fraction, 0f), 1f)
    }


    private fun smoothLerpVectors(vectors: List<BlockPos>, moveAdvance: Float): List<Vec3d> {
        val temp = mutableListOf<Vec3d>()
        if (vectors.isEmpty()) return temp
        vectors.forEachIndexed { i, current ->
            val (x, y, z) = if (i == vectors.lastIndex && i > 0) {
                val previous = vectors[i - 1]
                Triple(
                    MathHelper.lerp(moveAdvance, previous.x, current.x),
                    MathHelper.lerp(moveAdvance, previous.y, current.y),
                    MathHelper.lerp(moveAdvance, previous.z, current.z)
                )
            } else {
                Triple(current.x.toDouble(), current.y.toDouble(), current.z.toDouble())
            }
            temp.add(Vec3d(x.toDouble(), y.toDouble(), z.toDouble()))
        }
        return temp
    }

    private fun randomGlyphSpawnPosition(): BlockPos {
        val range = spawnRange()
        val distance = random.nextInt(range[1] - range[0] + 1) + range[0]
        val player = mc.player ?: return BlockPos(0, 0, 0)
        val fov = mc.options.fov.value
        val baseYaw = player.yaw
        val minYaw = (baseYaw - fov * 0.75).toInt()
        val maxYaw = (baseYaw + fov * 0.75).toInt()

        val randomYaw = random.nextInt(maxYaw - minYaw + 1) + minYaw
        val radYaw = Math.toRadians(randomYaw.toDouble())
        val xOffset = (-(sin(radYaw) * distance)).toInt()
        val yOffset = random.nextInt(range[3] - (-range[2]) + 1) + (-range[2])
        val zOffset = (cos(radYaw) * distance).toInt()
        val camera = mc.gameRenderer.camera
        val camX = camera.pos.x
        val camY = camera.pos.y
        val camZ = camera.pos.z
        return BlockPos((camX + xOffset).toInt(), (camY + yOffset).toInt(), (camZ + zOffset).toInt())
    }

    private fun addOneGlyph() {
        val stepsRange = lineStepsRange()
        val spawnPos = randomGlyphSpawnPosition()
        val minSteps = stepsRange[0]
        val maxSteps = stepsRange[1]
        val steps = random.nextInt(maxSteps - minSteps + 1) + minSteps
        glyphVectorGenerators.add(GlyphVectorGenerator(spawnPos, steps))
    }

    private fun removeExpiredGlyphs() {
        glyphVectorGenerators.removeIf { it.isExpired }
    }

    private fun updateGlyphs() {
        glyphVectorGenerators.forEach { it.update() }
    }

    private fun drawAllGlyphs(partialTicks: Float, matrix: Matrix4f) {
        if (glyphVectorGenerators.isEmpty()) return

        val tess = RenderSystem.renderThreadTesselator()
        val buffer: BufferBuilder = tess.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR)

        glyphVectorGenerators.forEachIndexed { index, glyph ->

            val lw = calcLineWidth(glyph)
            RenderSystem.lineWidth(lw)
            renderGlyphToBuffer(buffer, glyph, index + 1, glyph.currentAlpha, partialTicks, matrix)
        }

        RenderSystem.lineWidth(1.0f)

        BufferRenderer.drawWithGlobalProgram(buffer.endNullable() ?: return)
    }


    private fun renderGlyphToBuffer(
        buffer: BufferBuilder,
        glyph: GlyphVectorGenerator,
        colorIndex: Int,
        alphaPercentage: Float,
        partialTicks: Float,
        matrix: Matrix4f
    ) {
        if (glyph.vectorList.size < 2) return
        val lineVectors = glyph.getPositionVectors(partialTicks)
        var currentColorIndex = colorIndex

        fun getRenderColor(idx: Int, alpha: Float) = when (colorMode.activeChoice) {
            is GenericRainbowColorMode -> {
                val time = (System.currentTimeMillis() % 4000) / 4000f
                val hue = (time + idx / lineVectors.size.toFloat()) % 1f
                hslToRgb(hue, 0.95f, 0.65f, (alpha * 255).toInt())
            }
            else -> {
                val (color1, color2) = colorMode.activeChoice.getColors(mc.player)
                val t = idx / lineVectors.size.toFloat()
                color1.blend(color2, t).withAlpha((alpha * 255).toInt())
            }
        }

        for (i in 1 until lineVectors.size) {
            val p0 = lineVectors[i - 1]
            val p1 = lineVectors[i]
            val color = getRenderColor(i, alphaPercentage)
            val camera = mc.gameRenderer.camera
            val camX = camera.pos.x
            val camY = camera.pos.y
            val camZ = camera.pos.z

            buffer.vertex(matrix, (p0.x - camX).toFloat(), (p0.y - camY).toFloat(), (p0.z - camZ).toFloat()).color(color.toARGB())
            buffer.vertex(matrix, (p1.x - camX).toFloat(), (p1.y - camY).toFloat(), (p1.z - camZ).toFloat()).color(color.toARGB())

            currentColorIndex += 180
        }

        currentColorIndex = colorIndex
        for ((i, p) in lineVectors.withIndex()) {
            val color = getRenderColor(i, alphaPercentage)
            val eps = 0.001f

            buffer.vertex(matrix, p.x.toFloat() - eps, p.y.toFloat() - eps, p.z.toFloat()).color(color.toARGB())
            buffer.vertex(matrix, p.x.toFloat() + eps, p.y.toFloat() + eps, p.z.toFloat()).color(color.toARGB())
            currentColorIndex += 180
        }
    }


    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (mc.player == null) return@handler
        if (glyphVectorGenerators.size < maxGlyphCount()) addOneGlyph()
        updateGlyphs()
        removeExpiredGlyphs()
    }

    @Suppress("unused")
    private val renderHandler = handler<WorldRenderEvent> { event ->
        if (mc.player == null) return@handler
        removeExpiredGlyphs()

        renderEnvironmentForWorld(event.matrixStack) {
            // set shader for position+color
            RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR)
            val matrix = event.matrixStack.peek().positionMatrix
            drawAllGlyphs(event.partialTicks, matrix)
        }
    }

    private class GlyphVectorGenerator(spawnPos: BlockPos, maxStepsAmount: Int) {
        private var lifeTicks = 80

        val vectorList = mutableListOf<BlockPos>().apply { add(spawnPos) }
        private var currentStepTicks = 0
        private var lastStepTicks = 0
        private var stepsRemaining = maxStepsAmount
        private var lastRotation = intArrayOf(random360X, random360Y)
        private var fadeOut: Double = 1.0
        val currentAlpha: Float get() = fadeOut.toFloat()

        init { /* nothing needed */ }

        fun update() {
            lifeTicks--
            if (lifeTicks <= 0) {
                fadeOut = max(fadeOut - 0.05, 0.0)
                return
            }
            if (stepsRemaining == 0) {
                if (currentStepTicks > 0) {
                    currentStepTicks -= if (slowSpeed) 1 else 2
                    if (currentStepTicks < 0) currentStepTicks = 0
                } else {
                    fadeOut = max(fadeOut - 0.05, 0.0)
                }
                return
            }
            if (currentStepTicks > 0) {
                currentStepTicks -= if (slowSpeed) 1 else 2
                if (currentStepTicks < 0) currentStepTicks = 0
                return
            }
            lastRotation = getRandom90Rotation(lastRotation)
            val movementSteps = lineMovementSteps()
            val minStep = movementSteps[0]
            val maxStep = movementSteps[1]
            currentStepTicks = random.nextInt(maxStep - minStep + 1) + minStep
            lastStepTicks = currentStepTicks
            val lastPos = vectorList.last()
            vectorList.add(offsetFromRotation(lastPos, lastRotation, currentStepTicks))
            stepsRemaining--
        }

        fun getPositionVectors(partialTicks: Float): List<Vec3d> {
            val moveAdvance = calculateMoveAdvance(lastStepTicks, currentStepTicks, partialTicks)
            return smoothLerpVectors(vectorList, moveAdvance)
        }

        val isExpired: Boolean
            get() = fadeOut <= 0.0
    }
}
