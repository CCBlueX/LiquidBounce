package net.ccbluex.liquidbounce.features.module.modules.render.minimap

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleESP
import net.ccbluex.liquidbounce.render.coloredTriangle
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentRotation
import net.ccbluex.liquidbounce.utils.math.Vec2i
import net.ccbluex.liquidbounce.utils.render.AlignmentConfigurable
import net.minecraft.client.render.BufferBuilder
import net.minecraft.client.render.GameRenderer
import net.minecraft.client.render.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import org.joml.AxisAngle4f
import org.joml.Matrix4f
import org.joml.Quaternionf
import org.lwjgl.opengl.GL11
import kotlin.math.ceil
import kotlin.math.sqrt

object ModuleMinimap : Module("Minimap", Category.RENDER) {
    private val size by int("Size", 96, 1..256)
    private val viewDistance by float("ViewDistance", 3.0F, 1.0F..8.0F)

    private val alignment =
        AlignmentConfigurable(
            horizontalAlignment = AlignmentConfigurable.ScreenAxisX.LEFT,
            horizontalPadding = 16,
            verticalAlignment = AlignmentConfigurable.ScreenAxisY.BOTTOM,
            verticalPadding = 16,
        )

    init {
        ChunkRenderer

        tree(alignment)
    }

    val renderHandler =
        handler<OverlayRenderEvent> { event ->
            val matStack = MatrixStack()

            val playerPos = player.interpolateCurrentPosition(event.tickDelta)
            val playerRotation = player.interpolateCurrentRotation(event.tickDelta)

            val minimapSize = size

            val boundingBox = alignment.getBounds(minimapSize.toFloat(), minimapSize.toFloat())

            GL11.glEnable(GL11.GL_SCISSOR_TEST)
            GL11.glScissor(
                (boundingBox.xMin * mc.options.guiScale.value).toInt(),
                mc.framebuffer.viewportHeight - ((boundingBox.yMin + minimapSize) * mc.options.guiScale.value).toInt(),
                (minimapSize * mc.options.guiScale.value),
                minimapSize * mc.options.guiScale.value,
            )

            val baseX = (playerPos.x / 16.0).toInt()
            val baseZ = (playerPos.z / 16.0).toInt()

            val playerOffX = (playerPos.x / 16.0) % 1.0
            val playerOffZ = (playerPos.z / 16.0) % 1.0

            val chunksToRenderAround = ceil(sqrt(2.0) * (viewDistance + 1)).toInt()

            val scale = minimapSize / (2.0F * viewDistance)

            matStack.translate(boundingBox.xMin + minimapSize * 0.5, boundingBox.yMin + minimapSize * 0.5, 0.0)
            matStack.scale(scale, scale, scale)

            matStack.push()

            matStack.multiply(Quaternionf(AxisAngle4f(-(playerRotation.yaw + 180.0F).toRadians(), 0.0F, 0.0F, 1.0F)))
            matStack.translate(-playerOffX, -playerOffZ, 0.0)

            renderEnvironmentForGUI(matStack) {
                val glId = ChunkRenderer.prepareRendering()

                RenderSystem.bindTexture(glId)

                RenderSystem.setShaderTexture(0, glId)

                drawCustomMesh(
                    VertexFormat.DrawMode.QUADS,
                    VertexFormats.POSITION_TEXTURE_COLOR,
                    GameRenderer.getPositionTexColorProgram()!!,
                ) { matrix ->
                    buildMinimapMesh(this, matrix, Vec2i(baseX, baseZ), chunksToRenderAround, viewDistance)
                }

                drawCustomMesh(
                    VertexFormat.DrawMode.TRIANGLES,
                    VertexFormats.POSITION_COLOR,
                    GameRenderer.getPositionColorProgram()!!,
                ) { matrix ->
                    for (renderedEntity in ModuleESP.findRenderedEntities()) {
                        drawEntityOnMinimap(
                            this,
                            matStack,
                            renderedEntity,
                            event.tickDelta,
                            Vec2f(baseX.toFloat(), baseZ.toFloat())
                        )
                    }
                }
            }

            matStack.pop()

            renderEnvironmentForGUI(matStack) {
                drawLines(
                    Vec3(-chunksToRenderAround.toDouble(), 0.0, 0.0),
                    Vec3(chunksToRenderAround.toDouble(), 0.0, 0.0),
                    Vec3(0.0, -chunksToRenderAround.toDouble(), 0.0),
                    Vec3(0.0, chunksToRenderAround.toDouble(), 0.0),
                )
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST)
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

                builder
                    .vertex(matrix, from.x, from.y, 0.0F)
                    .texture(texPosition.xMin, texPosition.yMin)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .next()
                builder
                    .vertex(matrix, from.x, to.y, 0.0F)
                    .texture(texPosition.xMin, texPosition.yMax)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .next()
                builder
                    .vertex(matrix, to.x, to.y, 0.0F)
                    .texture(texPosition.xMax, texPosition.yMax)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .next()
                builder
                    .vertex(matrix, to.x, from.y, 0.0F)
                    .texture(texPosition.xMax, texPosition.yMin)
                    .color(1.0F, 1.0F, 1.0F, 1.0F)
                    .next()
            }
        }
    }

    private fun drawEntityOnMinimap(
        bufferBuilder: BufferBuilder,
        matStack: MatrixStack,
        entity: Entity,
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

        matStack.translate(
            /* x = */ -w / 5.0 * ChunkRenderer.SUN_DIRECTION.x / 16.0,
            /* y = */ -w / 5.0 * ChunkRenderer.SUN_DIRECTION.y / 16.0,
            /* z = */ 0.0
        )
        bufferBuilder.coloredTriangle(
            matStack.peek().positionMatrix,
            p1, p2, p3,
            Color4b((color.r * 0.1).toInt(), (color.g * 0.1).toInt(), (color.b * 0.1).toInt(), 200)
        )
        matStack.pop()

        bufferBuilder.coloredTriangle(matStack.peek().positionMatrix, p1, p2, p3, color)

        matStack.pop()
    }

    override fun enable() {
        ChunkScanner.subscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        ChunkRenderer.unloadEverything()
    }
}
