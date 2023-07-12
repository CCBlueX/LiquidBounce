/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

/**
 * Data class representing the rendering environment.
 *
 * @property matrixStack The matrix stack for rendering.
 */
data class RenderEnvironment(val matrixStack: MatrixStack)

/**
 * Helper function to render an environment with the specified [matrixStack] and [draw] block.
 *
 * @param matrixStack The matrix stack for rendering.
 * @param draw The block of code to be executed in the rendering environment.
 */
fun renderEnvironment(matrixStack: MatrixStack, draw: RenderEnvironment.() -> Unit) {
    val camera = mc.entityRenderDispatcher.camera ?: return
    val cameraPosition = camera.pos

    RenderSystem.enableBlend()
    RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA)
    RenderSystem.disableDepthTest()

    matrixStack.push()

    // Translate to the entity position
    matrixStack.translate(-cameraPosition.x, -cameraPosition.y, -cameraPosition.z)

    val environment = RenderEnvironment(matrixStack)
    draw(environment)

    matrixStack.pop()

    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
    RenderSystem.disableBlend()
    RenderSystem.enableDepthTest()
}

/**
 * Extension function to apply a position transformation to the current rendering environment.
 *
 * @param pos The position vector.
 * @param draw The block of code to be executed in the transformed environment.
 */
fun RenderEnvironment.withPosition(pos: Vec3, draw: RenderEnvironment.() -> Unit) {
    with(matrixStack) {
        push()
        translate(pos.x, pos.y, pos.z)
        draw()
        pop()
    }
}

/**
 * Extension function to apply a color transformation to the current rendering environment.
 *
 * @param color4b The color transformation.
 * @param draw The block of code to be executed in the transformed environment.
 */
fun RenderEnvironment.withColor(color4b: Color4b, draw: RenderEnvironment.() -> Unit) {
    RenderSystem.setShaderColor(color4b.r / 255f, color4b.g / 255f, color4b.b / 255f, color4b.a / 255f)
    draw()
    RenderSystem.setShaderColor(1f, 1f, 1f, 1f)
}

/**
 * Function to draw lines using the specified [lines] vectors.
 *
 * @param lines The vectors representing the lines.
 */
fun RenderEnvironment.drawLines(vararg lines: Vec3) {
    drawLines(*lines, mode = DrawMode.DEBUG_LINES)
}

/**
 * Function to draw a line strip using the specified [lines] vectors.
 *
 * @param lines The vectors representing the line strip.
 */
fun RenderEnvironment.drawLineStrip(vararg lines: Vec3) {
    drawLines(*lines, mode = DrawMode.DEBUG_LINE_STRIP)
}

/**
 * Helper function to draw lines using the specified [lines] vectors and draw mode.
 *
 * @param lines The vectors representing the lines.
 * @param mode The draw mode for the lines.
 */
private fun RenderEnvironment.drawLines(vararg lines: Vec3, mode: DrawMode = DrawMode.DEBUG_LINES) {
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    val bufferBuilder = tessellator.buffer

    // Set the shader to the position program
    RenderSystem.setShader { GameRenderer.getPositionProgram() }

    // Draw the vertices of the box
    with(bufferBuilder) {
        // Begin drawing lines with position format
        begin(mode, VertexFormats.POSITION)

        // Draw the vertices of the box
        lines.forEach { (x, y, z) ->
            vertex(matrix, x, y, z).next()
        }
    }

    // Draw the outlined box
    tessellator.draw()
}

/**
 * Function to draw a side box using the specified [box] and [side].
 *
 * @param box The bounding box of the side.
 * @param side The direction of the side.
 */
fun RenderEnvironment.drawSideBox(box: Box, side: Direction) {
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    val bufferBuilder = tessellator.buffer

    // Set the shader to the position program
    RenderSystem.setShader { GameRenderer.getPositionProgram() }

    // Draw the vertices of the box
    with(bufferBuilder) {
        // Begin drawing lines with position format
        begin(DrawMode.QUADS, VertexFormats.POSITION)

        // Draw the vertices of the box
        val vertices = when (side) {
            Direction.DOWN -> listOf(
                Vec3(box.minX, box.minY, box.maxZ),
                Vec3(box.minX, box.minY, box.minZ),
                Vec3(box.maxX, box.minY, box.minZ),
                Vec3(box.maxX, box.minY, box.maxZ)
            )
            Direction.UP -> listOf(
                Vec3(box.minX, box.maxY, box.minZ),
                Vec3(box.minX, box.maxY, box.maxZ),
                Vec3(box.maxX, box.maxY, box.maxZ),
                Vec3(box.maxX, box.maxY, box.minZ)
            )
            Direction.NORTH -> listOf(
                Vec3(box.maxX, box.maxY, box.minZ),
                Vec3(box.maxX, box.minY, box.minZ),
                Vec3(box.minX, box.minY, box.minZ),
                Vec3(box.minX, box.maxY, box.minZ)
            )
            Direction.SOUTH -> listOf(
                Vec3(box.minX, box.maxY, box.maxZ),
                Vec3(box.minX, box.minY, box.maxZ),
                Vec3(box.maxX, box.minY, box.maxZ),
                Vec3(box.maxX, box.maxY, box.maxZ)
            )
            Direction.WEST -> listOf(
                Vec3(box.minX, box.maxY, box.minZ),
                Vec3(box.minX, box.minY, box.minZ),
                Vec3(box.minX, box.minY, box.maxZ),
                Vec3(box.minX, box.maxY, box.maxZ)
            )
            Direction.EAST -> listOf(
                Vec3(box.maxX, box.maxY, box.maxZ),
                Vec3(box.maxX, box.minY, box.maxZ),
                Vec3(box.maxX, box.minY, box.minZ),
                Vec3(box.maxX, box.maxY, box.minZ)
            )
        }

        vertices.forEach { (x, y, z) ->
            vertex(matrix, x, y, z).next()
        }
    }

    // Draw the outlined box
    tessellator.draw()
}

/**
 * Function to draw an outlined box using the specified [box].
 *
 * @param box The bounding box of the box.
 */
fun RenderEnvironment.drawOutlinedBox(box: Box) {
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    val bufferBuilder = tessellator.buffer

    // Set the shader to the position program
    RenderSystem.setShader { GameRenderer.getPositionProgram() }

    // Draw the vertices of the box
    with(bufferBuilder) {
        // Begin drawing lines with position format
        begin(DrawMode.DEBUG_LINES, VertexFormats.POSITION)

        // Draw the vertices of the box
        val vertices = listOf(
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ)
        )

        vertices.forEach { (x, y, z) ->
            vertex(matrix, x, y, z).next()
        }
    }

    // Draw the outlined box
    tessellator.draw()
}

/**
 * Function to draw a solid box using the specified [box].
 *
 * @param box The bounding box of the box.
 */
fun RenderEnvironment.drawSolidBox(box: Box) {
    val matrix = matrixStack.peek().positionMatrix
    val tessellator = RenderSystem.renderThreadTesselator()
    val bufferBuilder = tessellator.buffer

    // Set the shader to the position program
    RenderSystem.setShader { GameRenderer.getPositionProgram() }

    // Begin drawing quads with position format

    with(bufferBuilder) {
        // Begin drawing lines with position format
        begin(DrawMode.QUADS, VertexFormats.POSITION)

        // Draw the vertices of the box
        val vertices = listOf(
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.minY, box.minZ),
            Vec3(box.maxX, box.maxY, box.minZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.maxX, box.minY, box.maxZ),
            Vec3(box.maxX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.minY, box.minZ),
            Vec3(box.minX, box.minY, box.maxZ),
            Vec3(box.minX, box.maxY, box.maxZ),
            Vec3(box.minX, box.maxY, box.minZ)
        )

        vertices.forEach { (x, y, z) ->
            vertex(matrix, x, y, z).next()
        }
    }

    // Draw the solid box
    tessellator.draw()
}
