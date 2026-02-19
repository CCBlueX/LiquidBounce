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

package net.ccbluex.liquidbounce.integration.theme.component.components.minimap

import it.unimi.dsi.fastutil.objects.ReferenceArrayList
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.misc.HideAppearance
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.integration.theme.component.components.NativeHudComponent
import net.ccbluex.liquidbounce.render.getBounds
import net.ccbluex.liquidbounce.render.drawCustomElement
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.font.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.ceilToInt
import net.ccbluex.liquidbounce.utils.client.fastCos
import net.ccbluex.liquidbounce.utils.client.fastSin
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.RenderedEntities
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentRotation
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.render.Alignment
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.Items
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.phys.Vec2
import kotlin.math.abs
import kotlin.math.max

object MinimapHudComponent : NativeHudComponent("Minimap", false, Alignment(
    horizontalAlignment = Alignment.ScreenAxisX.LEFT,
    horizontalOffset = 7,
    verticalAlignment = Alignment.ScreenAxisY.TOP,
    verticalOffset = 180,
)) {

    private val size by int("Size", 96, 1..256)
    private val viewDistance by float("ViewDistance", 3.0F, 1.0F..8.0F)
    private val fixedDirection by boolean("FixedDirection", false)

    private object TextureValueGroup : ToggleableValueGroup(this, "Texture", true) {
        val vertexColor by color("VertexColor", Color4b.WHITE)

        override fun onEnabled() {
            ChunkScanner.subscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        }

        override fun onDisabled() {
            ChunkScanner.unsubscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
            ChunkRenderer.unloadEverything()
        }
    }

    private object EntityValueGroup : ToggleableValueGroup(this, "Entity", true) {
        val scale by float("Scale", 1f, 0.25F..4F)
        val outOfBounds by enumChoice("OutOfBounds", OutOfBounds.NONE)

        val entities = ReferenceArrayList<LivingEntity>()

        private val MINIMAP_ENTITY_ORDER = Comparator<Entity> { e1, e2 ->
            when {
                e1.y != e2.y -> e1.y.compareTo(e2.y)
                e1.x != e2.x -> e1.x.compareTo(e2.x)
                else -> e1.z.compareTo(e2.z)
            }
        }

        override fun onEnabled() {
            RenderedEntities.subscribe(this)
            RenderedEntities.onUpdated {
                entities.clear()
                entities.ensureCapacity(RenderedEntities.size)
                RenderedEntities.filterTo(entities) { it !== player }
                entities.sortWith(MINIMAP_ENTITY_ORDER)
            }
            super.onEnabled()
        }

        override fun onDisabled() {
            RenderedEntities.unsubscribe(this)
            super.onDisabled()
        }

        enum class OutOfBounds(override val tag: String) : Tagged {
            NONE("None"),
            ALL("All"),
        }
    }

    private class ExtraElement(
        name: String,
        private val size: Float,
        private val draw: Renderer,
    ) : ToggleableValueGroup(this, name, false) {
        val placement by enumChoice("Placement", Placement.TOP_LEFT)

        fun render(ctx: GuiGraphics, boundingBox: BoundingBox2f) {
            if (enabled) {
                ctx.pose().withPush {
                    when (placement) {
                        Placement.TOP_LEFT -> translate(boundingBox.xMin, boundingBox.yMin)
                        Placement.TOP_RIGHT -> translate(boundingBox.xMax - size, boundingBox.yMin)
                        Placement.BOTTOM_LEFT -> translate(boundingBox.xMin, boundingBox.yMax - size)
                        Placement.BOTTOM_RIGHT -> translate(boundingBox.xMax - size, boundingBox.yMax - size)
                    }
                    draw(ctx)
                }
            }
        }

        private enum class Placement(override val tag: String) : Tagged {
            TOP_LEFT("TopLeft"),
            TOP_RIGHT("TopRight"),
            BOTTOM_LEFT("BottomLeft"),
            BOTTOM_RIGHT("BottomRight"),
        }

        fun interface Renderer {
            operator fun invoke(ctx: GuiGraphics)
        }
    }

    private val extraElements = arrayOf(
        ExtraElement("Compass", GuiRenderer.DEFAULT_ITEM_SIZE.toFloat()) { ctx ->
            val stack = player.inventory.nonEquipmentItems.find { it.item === Items.COMPASS } ?: COMPASS
            ctx.renderItem(stack, 0, 0)
        },
        ExtraElement("Clock", GuiRenderer.DEFAULT_ITEM_SIZE.toFloat()) { ctx ->
            val stack = player.inventory.nonEquipmentItems.find { it.item === Items.CLOCK } ?: CLOCK
            ctx.renderItem(stack, 0, 0)
        },
    )

    private val COMPASS by lazy(LazyThreadSafetyMode.NONE) { Items.COMPASS.defaultInstance }
    private val CLOCK by lazy(LazyThreadSafetyMode.NONE) { Items.CLOCK.defaultInstance }

    init {
        tree(TextureValueGroup)
        tree(EntityValueGroup)
        extraElements.forEach(::tree)
        ChunkRenderer
        registerComponentListen(this)
    }

    val renderHandler = handler<OverlayRenderEvent>(priority = EventPriorityConvention.MODEL_STATE) { event ->
        if (HideAppearance.isHidingNow) {
            return@handler
        }

        val playerPos = player.interpolateCurrentPosition(event.tickDelta)
        val playerRotation = player.interpolateCurrentRotation(event.tickDelta)

        val minimapSize = size

        val boundingBox = alignment.getBounds(minimapSize.toFloat(), minimapSize.toFloat())

        val centerBB = Vec2(
            boundingBox.xMin + (boundingBox.xMax - boundingBox.xMin) * 0.5F,
            boundingBox.yMin + (boundingBox.yMax - boundingBox.yMin) * 0.5F
        )

        val baseX = (playerPos.x / 16.0).toInt()
        val baseZ = (playerPos.z / 16.0).toInt()

        val playerOffX = (playerPos.x / 16.0) % 1.0
        val playerOffZ = (playerPos.z / 16.0) % 1.0

        val chunksToRenderAround = (Mth.SQRT_OF_TWO * (viewDistance + 1)).ceilToInt()

        val scale = minimapSize / (2.0F * viewDistance)
        val mapRotation = if (!fixedDirection) -(playerRotation.yaw + 180.0F).toRadians() else 0F

        with(event.context) {
            val bounds = getBounds(boundingBox)
            scissorStack.withPush(bounds) {
                pose().withPush {
                    translate(boundingBox.xMin + minimapSize * 0.5F, boundingBox.yMin + minimapSize * 0.5F)
                    scale(scale)

                    if (mapRotation != 0F) rotate(mapRotation)
                    translate(-playerOffX.toFloat(), -playerOffZ.toFloat())

                    drawMinimapTexture(bounds, baseX, baseZ, chunksToRenderAround, viewDistance)

                    drawEntities(event.tickDelta, baseX.toFloat(), baseZ.toFloat())
                }
            }

            drawOutOfBoundsEntityMarkers(
                tickDelta = event.tickDelta,
                center = centerBB,
                boundingBox = boundingBox,
                playerPosX = playerPos.x.toFloat() / 16.0F,
                playerPosZ = playerPos.z.toFloat() / 16.0F,
                mapScale = scale,
                viewDistance = viewDistance,
                rotation = mapRotation,
            )

            for (element in extraElements) {
                element.render(this, boundingBox)
            }

            val from = Color4b.DEFAULT_BG_COLOR
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

            drawLines(lines, Color4b.WHITE.argb, bounds)
        }
    }

    private fun GuiGraphics.drawShadowForBB(
        boundingBox: BoundingBox2f,
        bounds: ScreenRectangle,
        from: Color4b,
        to: Color4b,
        offset: Float = 3.0F,
        width: Float = 3.0F,
    ) {
        val from = from.argb
        val to = to.argb

        drawCustomElement(
            pipeline = RenderPipelines.GUI,
            bounds = bounds,
        ) { pose ->
            addVertexWith2DPose(pose, boundingBox.xMin + offset, boundingBox.yMax).setColor(from)
            addVertexWith2DPose(pose, boundingBox.xMin + offset, boundingBox.yMax + width).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMax, boundingBox.yMax + width).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMax, boundingBox.yMax).setColor(from)

            addVertexWith2DPose(pose, boundingBox.xMax, boundingBox.yMin + offset).setColor(from)
            addVertexWith2DPose(pose, boundingBox.xMax, boundingBox.yMax).setColor(from)
            addVertexWith2DPose(pose, boundingBox.xMax + width, boundingBox.yMax).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMax + width, boundingBox.yMin + offset).setColor(to)

            addVertexWith2DPose(pose, boundingBox.xMax, boundingBox.yMax).setColor(from)
            addVertexWith2DPose(pose, boundingBox.xMax, boundingBox.yMax + width).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMax + width, boundingBox.yMax + width).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMax + width, boundingBox.yMax).setColor(to)

            addVertexWith2DPose(pose, boundingBox.xMin + offset - width, boundingBox.yMax).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMin + offset - width, boundingBox.yMax + width).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMin + offset, boundingBox.yMax + width).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMin + offset, boundingBox.yMax).setColor(from)

            addVertexWith2DPose(pose, boundingBox.xMax, boundingBox.yMin + offset - width).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMax, boundingBox.yMin + offset).setColor(from)
            addVertexWith2DPose(pose, boundingBox.xMax + width, boundingBox.yMin + offset).setColor(to)
            addVertexWith2DPose(pose, boundingBox.xMax + width, boundingBox.yMin + offset - width).setColor(to)
        }
    }

    private fun GuiGraphics.drawMinimapTexture(
        bounds: ScreenRectangle,
        baseX: Int,
        baseZ: Int,
        chunksToRenderAround: Int,
        viewDistance: Float,
    ) {
        if (!TextureValueGroup.enabled) {
            return
        }

        drawCustomElement(
            pipeline = RenderPipelines.GUI_TEXTURED,
            textureSetup = ChunkRenderer.prepareRendering(),
            bounds = bounds,
        ) { pose ->
            val color = TextureValueGroup.vertexColor.argb
            for (x in -chunksToRenderAround..chunksToRenderAround) {
                for (z in -chunksToRenderAround..chunksToRenderAround) {
                    // Don't render too much
                    if (x * x + z * z > (viewDistance + 3).sq()) {
                        continue
                    }

                    val chunkPos = ChunkPos.asLong(baseX + x, baseZ + z)

                    val texPosition = ChunkRenderer.getAtlasPosition(chunkPos).uv
                    val fromX = x.toFloat()
                    val fromY = z.toFloat()
                    val toX = fromX + 1F
                    val toY = fromY + 1F

                    addVertexWith2DPose(pose, fromX, fromY).setUv(texPosition.xMin, texPosition.yMin)
                        .setColor(color)
                    addVertexWith2DPose(pose, fromX, toY).setUv(texPosition.xMin, texPosition.yMax)
                        .setColor(color)
                    addVertexWith2DPose(pose, toX, toY).setUv(texPosition.xMax, texPosition.yMax)
                        .setColor(color)
                    addVertexWith2DPose(pose, toX, fromY).setUv(texPosition.xMax, texPosition.yMin)
                        .setColor(color)
                }
            }
        }
    }

    private fun GuiGraphics.drawEntities(
        tickDelta: Float,
        baseX: Float,
        baseZ: Float,
    ) {
        if (!EntityValueGroup.enabled) {
            return
        }

        for (entity in EntityValueGroup.entities) {
            val color = ModuleESP.getColor(entity)

            val pos = entity.interpolateCurrentPosition(tickDelta)
            val rot = entity.interpolateCurrentRotation(tickDelta)

            pose().pushMatrix()
            pose().translate(pos.x.toFloat() / 16.0F - baseX, pos.z.toFloat() / 16.0F - baseZ)
            pose().rotate(rot.yaw.toRadians())
            pose().scale(EntityValueGroup.scale)

            val w = 2.0f
            val h = w * 1.618f

            val p1 = Vec2(-w * 0.5f / 16.0f, -h * 0.5f / 16.0f)
            val p2 = Vec2(0.0f, h * 0.5f / 16.0f)
            val p3 = Vec2(w * 0.5f / 16.0f, -h * 0.5f / 16.0f)

            pose().pushMatrix()

            pose().translate(
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
            pose().popMatrix()

            // Entity
            drawTriangle(p1, p2, p3, color)

            pose().popMatrix()
        }
    }

    private fun GuiGraphics.drawOutOfBoundsEntityMarkers(
        tickDelta: Float,
        center: Vec2,
        boundingBox: BoundingBox2f,
        playerPosX: Float,
        playerPosZ: Float,
        mapScale: Float,
        viewDistance: Float,
        rotation: Float,
    ) {
        if (!EntityValueGroup.enabled || EntityValueGroup.outOfBounds == EntityValueGroup.OutOfBounds.NONE) {
            return
        }

        val markerThickness = 2.0F
        val markerLength = 4.0F
        val markerHalf = markerLength * 0.5F
        val sin = rotation.fastSin()
        val cos = rotation.fastCos()

        for (entity in EntityValueGroup.entities) {
            val color = ModuleESP.getColor(entity)
            val pos = entity.interpolateCurrentPosition(tickDelta)

            val dx = pos.x.toFloat() / 16.0F - playerPosX
            val dz = pos.z.toFloat() / 16.0F - playerPosZ

            val rx = dx * cos - dz * sin
            val rz = dx * sin + dz * cos

            val maxAxis = max(abs(rx), abs(rz))
            if (maxAxis <= viewDistance) {
                continue
            }

            val edgeFactor = viewDistance / maxAxis
            val edgeX = rx * edgeFactor
            val edgeZ = rz * edgeFactor
            val screenX = center.x + edgeX * mapScale
            val screenY = center.y + edgeZ * mapScale

            val x1: Float
            val y1: Float
            val x2: Float
            val y2: Float

            if (abs(edgeX) >= abs(edgeZ)) {
                val clampedY = screenY.coerceIn(boundingBox.yMin + markerHalf, boundingBox.yMax - markerHalf)
                y1 = clampedY - markerHalf
                y2 = clampedY + markerHalf
                if (edgeX > 0.0F) {
                    x1 = boundingBox.xMax - markerThickness
                    x2 = boundingBox.xMax
                } else {
                    x1 = boundingBox.xMin
                    x2 = boundingBox.xMin + markerThickness
                }
            } else {
                val clampedX = screenX.coerceIn(boundingBox.xMin + markerHalf, boundingBox.xMax - markerHalf)
                x1 = clampedX - markerHalf
                x2 = clampedX + markerHalf
                if (edgeZ > 0.0F) {
                    y1 = boundingBox.yMax - markerThickness
                    y2 = boundingBox.yMax
                } else {
                    y1 = boundingBox.yMin
                    y2 = boundingBox.yMin + markerThickness
                }
            }

            drawQuad(x1, y1, x2, y2, color)
        }
    }

}
