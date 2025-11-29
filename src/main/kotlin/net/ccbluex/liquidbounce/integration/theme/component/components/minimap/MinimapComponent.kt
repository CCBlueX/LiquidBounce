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

package net.ccbluex.liquidbounce.integration.theme.component.components.minimap

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeComponent
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentRotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.ScreenRect
import net.minecraft.client.texture.TextureSetup
import net.minecraft.entity.Entity
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec2f
import java.util.EnumSet
import kotlin.math.ceil

object MinimapComponent : NativeComponent("Minimap", false, Alignment(
    horizontalAlignment = Alignment.ScreenAxisX.LEFT,
    horizontalOffset = 7,
    verticalAlignment = Alignment.ScreenAxisY.TOP,
    verticalOffset = 180,
)) {

    private val MINIMAP_ENTITY_ORDER = Comparator<Entity> { e1, e2 ->
        when {
            e1.y != e2.y -> e1.y.compareTo(e2.y)
            e1.x != e2.x -> e1.x.compareTo(e2.x)
            else -> e1.z.compareTo(e2.z)
        }
    }

    private val size by int("Size", 96, 1..256)
    private val viewDistance by float("ViewDistance", 3.0F, 1.0F..8.0F)
    private val show by multiEnumChoice("Show", EnumSet.allOf(Show::class.java), canBeNone = false)

    private enum class Show(override val choiceName: String) : NamedChoice {
        TEXTURE("Texture"),
        ENTITY("Entity"),
    }

    private inline val showTexture get() = Show.TEXTURE in show
    private inline val showEntity get() = Show.ENTITY in show

    init {
        ChunkRenderer
        registerComponentListen(this)
    }

    override fun onEnabled() {
        RenderedEntities.subscribe(this)
        ChunkScanner.subscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
    }

    override fun onDisabled() {
        RenderedEntities.unsubscribe(this)
        ChunkScanner.unsubscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        ChunkRenderer.unloadEverything()
    }

    val renderHandler = handler<OverlayRenderEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        if (HideAppearance.isHidingNow) {
            return@handler
        }

        val playerPos = player.interpolateCurrentPosition(event.tickDelta)
        val playerRotation = player.interpolateCurrentRotation(event.tickDelta)

        val minimapSize = size

        val boundingBox = alignment.getBounds(minimapSize.toFloat(), minimapSize.toFloat())

        val centerBB = Vec2f(
            boundingBox.xMin + (boundingBox.xMax - boundingBox.xMin) * 0.5F,
            boundingBox.yMin + (boundingBox.yMax - boundingBox.yMin) * 0.5F
        )

        val baseX = (playerPos.x / 16.0).toInt()
        val baseZ = (playerPos.z / 16.0).toInt()

        val playerOffX = (playerPos.x / 16.0) % 1.0
        val playerOffZ = (playerPos.z / 16.0) % 1.0

        val chunksToRenderAround = ceil(MathHelper.SQUARE_ROOT_OF_TWO * (viewDistance + 1)).toInt()

        val scale = minimapSize / (2.0F * viewDistance)

        with(event.context) {
            val bounds = createBounds(boundingBox)
            scissorStack.withPush(bounds) {
                matrices.withPush {
                    matrices.translate(boundingBox.xMin + minimapSize * 0.5F, boundingBox.yMin + minimapSize * 0.5F)
                    matrices.scale(scale, scale)

                    matrices.rotate(-(playerRotation.yaw + 180.0F).toRadians())
                    matrices.translate(-playerOffX.toFloat(), -playerOffZ.toFloat())

                    if (showTexture) {
                        drawMinimapTexture(bounds, ChunkPos(baseX, baseZ), chunksToRenderAround, viewDistance)
                    }

                    if (showEntity) {
                        drawEntities(event.tickDelta, baseX = baseX.toFloat(), baseZ = baseZ.toFloat())
                    }
                }
            }

            val from = Color4b.BLACK.copy(a = 100)
            val to = Color4b.TRANSPARENT

            drawShadowForBB(boundingBox, bounds, from, to)

            val lines = floatArrayOf(
                // Cursor
                boundingBox.xMin, centerBB.y,
                boundingBox.xMax, centerBB.y,
                centerBB.x, boundingBox.yMin,
                centerBB.x, boundingBox.yMax,
                // Border
                boundingBox.xMin, boundingBox.yMin,
                boundingBox.xMax, boundingBox.yMin,
                boundingBox.xMin, boundingBox.yMax,
                boundingBox.xMax, boundingBox.yMax,

                boundingBox.xMin, boundingBox.yMin,
                boundingBox.xMin, boundingBox.yMax,
                boundingBox.xMax, boundingBox.yMin,
                boundingBox.xMax, boundingBox.yMax,
            )

            drawLines(lines, Color4b.WHITE.toARGB(), bounds)
        }
    }

    private fun DrawContext.drawShadowForBB(
        boundingBox: BoundingBox2f,
        bounds: ScreenRect,
        from: Color4b,
        to: Color4b,
        offset: Float = 3.0F,
        width: Float = 3.0F,
    ) {
        val from = from.toARGB()
        val to = to.toARGB()

        drawCustomElement(
            pipeline = RenderPipelines.GUI,
            bounds = bounds,
        ) { pose, depth ->
            val z = depth // - 1.0F

            vertex(pose, boundingBox.xMin + offset, boundingBox.yMax, z).color(from)
            vertex(pose, boundingBox.xMin + offset, boundingBox.yMax + width, z).color(to)
            vertex(pose, boundingBox.xMax, boundingBox.yMax + width, z).color(to)
            vertex(pose, boundingBox.xMax, boundingBox.yMax, z).color(from)

            vertex(pose, boundingBox.xMax, boundingBox.yMin + offset, z).color(from)
            vertex(pose, boundingBox.xMax, boundingBox.yMax, z).color(from)
            vertex(pose, boundingBox.xMax + width, boundingBox.yMax, z).color(to)
            vertex(pose, boundingBox.xMax + width, boundingBox.yMin + offset, z).color(to)

            vertex(pose, boundingBox.xMax, boundingBox.yMax, z).color(from)
            vertex(pose, boundingBox.xMax, boundingBox.yMax + width, z).color(to)
            vertex(pose, boundingBox.xMax + width, boundingBox.yMax + width, z).color(to)
            vertex(pose, boundingBox.xMax + width, boundingBox.yMax, z).color(to)

            vertex(pose, boundingBox.xMin + offset - width, boundingBox.yMax, z).color(to)
            vertex(pose, boundingBox.xMin + offset - width, boundingBox.yMax + width, z).color(to)
            vertex(pose, boundingBox.xMin + offset, boundingBox.yMax + width, z).color(to)
            vertex(pose, boundingBox.xMin + offset, boundingBox.yMax, z).color(from)

            vertex(pose, boundingBox.xMax, boundingBox.yMin + offset - width, z).color(to)
            vertex(pose, boundingBox.xMax, boundingBox.yMin + offset, z).color(from)
            vertex(pose, boundingBox.xMax + width, boundingBox.yMin + offset, z).color(to)
            vertex(pose, boundingBox.xMax + width, boundingBox.yMin + offset - width, z).color(to)
        }
    }

    private fun DrawContext.drawMinimapTexture(
        bounds: ScreenRect,
        centerPos: ChunkPos,
        chunksToRenderAround: Int,
        viewDistance: Float,
    ) {
        drawCustomElement(
            pipeline = RenderPipelines.GUI_TEXTURED,
            textureSetup = TextureSetup.withoutGlTexture(ChunkRenderer.prepareRendering()),
            bounds = bounds,
        ) { pose, depth ->
            for (x in -chunksToRenderAround..chunksToRenderAround) {
                for (y in -chunksToRenderAround..chunksToRenderAround) {
                    // Don't render too much
                    if (x * x + y * y > (viewDistance + 3).sq()) {
                        continue
                    }

                    val chunkPos = ChunkPos(centerPos.x + x, centerPos.z + y)

                    val texPosition = ChunkRenderer.getAtlasPosition(chunkPos).uv
                    val fromX = x.toFloat()
                    val fromY = y.toFloat()
                    val toX = fromX + 1F
                    val toY = fromY + 1F

                    vertex(pose, fromX, fromY, depth).texture(texPosition.xMin, texPosition.yMin)
                        .color(-1)
                    vertex(pose, fromX, toY, depth).texture(texPosition.xMin, texPosition.yMax)
                        .color(-1)
                    vertex(pose, toX, toY, depth).texture(texPosition.xMax, texPosition.yMax)
                        .color(-1)
                    vertex(pose, toX, fromY, depth).texture(texPosition.xMax, texPosition.yMin)
                        .color(-1)
                }
            }
        }
    }

    private fun DrawContext.drawEntities(
        tickDelta: Float,
        baseX: Float,
        baseZ: Float,
    ) {
        for (entity in RenderedEntities.sortedWith(MINIMAP_ENTITY_ORDER)) {
            val color = ModuleESP.getColor(entity)

            val pos = entity.interpolateCurrentPosition(tickDelta)
            val rot = entity.interpolateCurrentRotation(tickDelta)

            matrices.pushMatrix()
            matrices.translate(pos.x.toFloat() / 16.0F - baseX, pos.z.toFloat() / 16.0F - baseZ)
            matrices.rotate(rot.yaw.toRadians())

            val w = 2.0f
            val h = w * 1.618f

            val p1 = Vec2f(-w * 0.5f / 16.0f, -h * 0.5f / 16.0f)
            val p2 = Vec2f(0.0f, h * 0.5f / 16.0f)
            val p3 = Vec2f(w * 0.5f / 16.0f, -h * 0.5f / 16.0f)

            matrices.pushMatrix()

            matrices.translate(
                -w / 5.0F * ChunkRenderer.SUN_DIRECTION.x() / 16.0F,
                -w / 5.0F * ChunkRenderer.SUN_DIRECTION.y() / 16.0F,
            )

            // Shadow
            drawTriangle(
                p1,
                p2,
                p3,
                Color4b((color.r * 0.1).toInt(), (color.g * 0.1).toInt(), (color.b * 0.1).toInt(), 200)
            )
            matrices.popMatrix()

            // Entity
            drawTriangle(p1, p2, p3, color)

            matrices.popMatrix()
        }
    }

}
