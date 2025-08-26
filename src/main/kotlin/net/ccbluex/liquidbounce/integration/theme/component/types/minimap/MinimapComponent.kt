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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.theme.component.types.minimap

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.integration.theme.component.Component
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentRotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.LivingEntity
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import org.joml.AxisAngle4f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.opengl.GL11
import kotlin.math.ceil
import kotlin.math.sqrt

object MinimapComponent : Component("Minimap", true) {

    private val size by int("Size", 96, 1..256)
    private val viewDistance by float("ViewDistance", 3.0F, 1.0F..8.0F)

    init {
        ChunkRenderer
        registerComponentListen()
    }

    val renderHandler = handler<OverlayRenderEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        if (HideAppearance.isHidingNow) {
            return@handler
        }

        val matStack = MatrixStack()

        val playerPos = player.interpolateCurrentPosition(event.tickDelta)
        val playerRotation = player.interpolateCurrentRotation(event.tickDelta)

        val minimapSize = size

        val boundingBox = alignment.getBounds(minimapSize.toFloat(), minimapSize.toFloat())
        val scaleFactor = mc.window.scaleFactor

        GL11.glEnable(GL11.GL_SCISSOR_TEST)
        GL11.glScissor(
            (boundingBox.xMin * scaleFactor).toInt(),
            mc.framebuffer.viewportHeight - ((boundingBox.yMin + minimapSize) * scaleFactor).toInt(),
            (minimapSize * scaleFactor).toInt(),
            (minimapSize * scaleFactor).toInt(),
        )

        val baseX = (playerPos.x / 16.0).toInt()
        val baseZ = (playerPos.z / 16.0).toInt()

        val playerOffX = (playerPos.x / 16.0) % 1.0
        val playerOffZ = (playerPos.z / 16.0) % 1.0

        val chunksToRenderAround = ceil(sqrt(2.0) * (viewDistance + 1)).toInt()

        val scale = minimapSize / (2.0F * viewDistance)

        matStack.push()

        matStack.translate(boundingBox.xMin + minimapSize * 0.5, boundingBox.yMin + minimapSize * 0.5, 0.0)
        matStack.scale(scale, scale, scale)

        matStack.multiply(Quaternionf(AxisAngle4f(-(playerRotation.yaw + 180.0F).toRadians(), 0.0F, 0.0F, 1.0F)))
        matStack.translate(-playerOffX, -playerOffZ, 0.0)

        renderEnvironmentForGUI(matStack) {
            val glId = ChunkRenderer.prepareRendering()

            RenderSystem.bindTexture(glId)

            RenderSystem.setShaderTexture(0, glId)

            drawCustomMesh(
                VertexFormat.DrawMode.QUADS,
                VertexFormats.POSITION_TEXTURE_COLOR,
                ShaderProgramKeys.POSITION_TEX_COLOR,
            ) { matrix ->
                buildMinimapMesh(this, matrix, Vec2i(baseX, baseZ), chunksToRenderAround, viewDistance)
            }

            drawCustomMesh(
                VertexFormat.DrawMode.TRIANGLES,
                VertexFormats.POSITION_COLOR,
                ShaderProgramKeys.POSITION_COLOR,
            ) { matrix ->
                for (renderedEntity in RenderedEntities) {
                    drawEntityOnMinimap(
                        this, matStack, renderedEntity, event.tickDelta, Vec2f(baseX.toFloat(), baseZ.toFloat())
                    )
                }
            }
        }

        matStack.pop()

        val centerBB = Vec2f(
            boundingBox.xMin + (boundingBox.xMax - boundingBox.xMin) * 0.5F,
            boundingBox.yMin + (boundingBox.yMax - boundingBox.yMin) * 0.5F
        )
        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        renderEnvironmentForGUI(matStack) {

            val from = Color4b(0, 0, 0, 100)
            val to = Color4b(0, 0, 0, 0)

            drawShadowForBB(boundingBox, from, to)
            drawLines(
                // Cursor
                Vec3(boundingBox.xMin, centerBB.y, 0.0F),
                Vec3(boundingBox.xMax, centerBB.y, 0.0F),
                Vec3(centerBB.x, boundingBox.yMin, 0.0F),
                Vec3(centerBB.x, boundingBox.yMax, 0.0F),
                // Border
                Vec3(boundingBox.xMin, boundingBox.yMin, 0.0F),
                Vec3(boundingBox.xMax, boundingBox.yMin, 0.0F),
                Vec3(boundingBox.xMin, boundingBox.yMax, 0.0F),
                Vec3(boundingBox.xMax, boundingBox.yMax, 0.0F),

                Vec3(boundingBox.xMin, boundingBox.yMin, 0.0F),
                Vec3(boundingBox.xMin, boundingBox.yMax, 0.0F),
                Vec3(boundingBox.xMax, boundingBox.yMin, 0.0F),
                Vec3(boundingBox.xMax, boundingBox.yMax, 0.0F),
            )
        }

    }

    private fun RenderEnvironment.drawShadowForBB(
        boundingBox: BoundingBox2f, from: Color4b, to: Color4b, offset: Float = 3.0F, width: Float = 3.0F
    ) {
        drawGradientQuad(
            listOf(
                Vec3(boundingBox.xMin + offset, boundingBox.yMax, -1.0F),
                Vec3(boundingBox.xMin + offset, boundingBox.yMax + width, -1.0F),
                Vec3(boundingBox.xMax, boundingBox.yMax + width, -1.0F),
                Vec3(boundingBox.xMax, boundingBox.yMax, -1.0F),

                Vec3(boundingBox.xMax, boundingBox.yMin + offset, -1.0F),
                Vec3(boundingBox.xMax, boundingBox.yMax, -1.0F),
                Vec3(boundingBox.xMax + width, boundingBox.yMax, -1.0F),
                Vec3(boundingBox.xMax + width, boundingBox.yMin + offset, -1.0F),

                Vec3(boundingBox.xMax, boundingBox.yMax, -1.0F),
                Vec3(boundingBox.xMax, boundingBox.yMax + width, -1.0F),
                Vec3(boundingBox.xMax + width, boundingBox.yMax + width, -1.0F),
                Vec3(boundingBox.xMax + width, boundingBox.yMax, -1.0F),

                Vec3(boundingBox.xMin + offset - width, boundingBox.yMax, -1.0F),
                Vec3(boundingBox.xMin + offset - width, boundingBox.yMax + width, -1.0F),
                Vec3(boundingBox.xMin + offset, boundingBox.yMax + width, -1.0F),
                Vec3(boundingBox.xMin + offset, boundingBox.yMax, -1.0F),

                Vec3(boundingBox.xMax, boundingBox.yMin + offset - width, -1.0F),
                Vec3(boundingBox.xMax, boundingBox.yMin + offset, -1.0F),
                Vec3(boundingBox.xMax + width, boundingBox.yMin + offset, -1.0F),
                Vec3(boundingBox.xMax + width, boundingBox.yMin + offset - width, -1.0F),
            ), listOf(
                from,
                to,
                to,
                from,

                from,
                from,
                to,
                to,

                from,
                to,
                to,
                to,

                to,
                to,
                to,
                from,

                to,
                from,
                to,
                to,
            )
        )
    }

    private fun buildMinimapMesh(
        builder: BufferBuilder,
        matrix: Matrix4f,
        centerPos: Vec2i,
        chunksToRenderAround: Int,
        viewDistance: Float,
    ) {
        for (x in -chunksToRenderAround..chunksToRenderAround) {
            for (y in -chunksToRenderAround..chunksToRenderAround) {
                // Don't render too much
                if (x * x + y * y > (viewDistance + 3) * (viewDistance + 3)) {
                    continue
                }

                val chunkPos = ChunkPos(centerPos.x + x, centerPos.y + y)

                val texPosition = ChunkRenderer.getAtlasPosition(chunkPos).uv
                val from = Vec2f(x.toFloat(), y.toFloat())
                val to = from.add(Vec2f(1.0F, 1.0F))

                builder.vertex(matrix, from.x, from.y, 0.0F).texture(texPosition.xMin, texPosition.yMin)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                builder.vertex(matrix, from.x, to.y, 0.0F).texture(texPosition.xMin, texPosition.yMax)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                builder.vertex(matrix, to.x, to.y, 0.0F).texture(texPosition.xMax, texPosition.yMax)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                builder.vertex(matrix, to.x, from.y, 0.0F).texture(texPosition.xMax, texPosition.yMin)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
            }
        }
    }

    private fun drawEntityOnMinimap(
        bufferBuilder: BufferBuilder,
        matStack: MatrixStack,
        entity: LivingEntity,
        partialTicks: Float,
        basePos: Vec2f,
    ) {
        val color = ModuleESP.getColor(entity)

        val pos = entity.interpolateCurrentPosition(partialTicks)
        val rot = entity.interpolateCurrentRotation(partialTicks)

        matStack.push()
        matStack.translate(pos.x / 16.0 - basePos.x, pos.z / 16.0 - basePos.y, 0.0)
        val rotation = Quaternionf(AxisAngle4f((rot.yaw).toRadians(), 0.0F, 0.0F, 1.0F))

        val w = 2.0
        val h = w * 1.618

        val p1 = Vec3d(-w * 0.5 / 16.0, -h * 0.5 / 16.0, 0.0)
        val p2 = Vec3d(0.0, h * 0.5 / 16.0, 0.0)
        val p3 = Vec3d(w * 0.5 / 16.0, -h * 0.5 / 16.0, 0.0)

        matStack.multiply(rotation)

        matStack.push()

        matStack.translate(/* x = */ -w / 5.0 * ChunkRenderer.SUN_DIRECTION.x / 16.0,/* y = */
            -w / 5.0 * ChunkRenderer.SUN_DIRECTION.y / 16.0,/* z = */
            0.0
        )
        bufferBuilder.coloredTriangle(
            matStack.peek().positionMatrix,
            p1,
            p2,
            p3,
            Color4b((color.r * 0.1).toInt(), (color.g * 0.1).toInt(), (color.b * 0.1).toInt(), 200)
        )
        matStack.pop()

        bufferBuilder.coloredTriangle(matStack.peek().positionMatrix, p1, p2, p3, color)

        matStack.pop()
    }

}
