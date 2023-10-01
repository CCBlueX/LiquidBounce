package net.ccbluex.liquidbounce.features.module.modules.render.minimap

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.event.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleESP
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.drawLines
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawTextureQuad
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.renderEnvironmentForGUI
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.block.ChunkScanner
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentRotation
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.render.AlignmentConfigurable
import net.minecraft.client.texture.NativeImageBackedTexture
import net.minecraft.client.util.math.MatrixStack
import net.minecraft.entity.Entity
import net.minecraft.util.math.ChunkPos
import net.minecraft.util.math.Vec2f
import net.minecraft.util.math.Vec3d
import org.joml.AxisAngle4f
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

            val chunksToRenderAround = ceil(sqrt(2.0) * viewDistance).toInt()

            val scale = minimapSize / viewDistance

            matStack.translate(boundingBox.xMin + minimapSize * 0.5, boundingBox.yMin + minimapSize * 0.5, 0.0)
            matStack.scale(scale, scale, scale)

            matStack.push()

            matStack.multiply(Quaternionf(AxisAngle4f(-(playerRotation.yaw + 180.0F).toRadians(), 0.0F, 0.0F, 1.0F)))
            matStack.translate(-playerOffX, -playerOffZ, 0.0)

            renderEnvironmentForGUI(matStack) {
                for (x in -chunksToRenderAround..chunksToRenderAround) {
                    for (y in -chunksToRenderAround..chunksToRenderAround) {
                        drawMinimapChunkAt(this, ChunkPos(baseX + x, baseZ + y), x, y)
                    }
                }

                for (renderedEntity in ModuleESP.findRenderedEntities()) {
                    drawEntityOnMinimap(this, renderedEntity, event.tickDelta, Vec2f(baseX.toFloat(), baseZ.toFloat()))
                }
            }

            matStack.pop()

            renderEnvironmentForGUI(matStack) {
                drawLines(
                    Vec3(-4.0, 0.0, 0.0),
                    Vec3(4.0, 0.0, 0.0),
                    Vec3(0.0, -4.0, 0.0),
                    Vec3(0.0, 4.0, 0.0),
                )
            }

            GL11.glDisable(GL11.GL_SCISSOR_TEST)
        }

    private fun drawEntityOnMinimap(
        env: RenderEnvironment,
        entity: Entity,
        partialTicks: Float,
        basePos: Vec2f,
    ) {
        env.withColor(ModuleESP.getColor(entity)) {
            val pos = entity.interpolateCurrentPosition(partialTicks)
            val rot = entity.interpolateCurrentRotation(partialTicks)

            val mat = env.matrixStack

            mat.push()
            mat.translate(pos.x / 16.0 - basePos.x, pos.z / 16.0 - basePos.y, 0.0)
            mat.multiply(Quaternionf(AxisAngle4f(-(rot.yaw + 180.0F).toRadians(), 0.0F, 0.0F, 1.0F)))

            val w = 2.0
            val h = w * 1.618

            env.drawTriangle(
                Vec3d(-w * 0.5 / 16.0, 0.0, 0.0),
                Vec3d(0.0, h / 16.0, 0.0),
                Vec3d(w * 0.5 / 16.0, 0.0, 0.0),
            )

            mat.pop()
        }
    }

    private fun drawMinimapChunkAt(
        env: RenderEnvironment,
        chunkPos: ChunkPos,
        x: Int,
        y: Int,
    ) {
        val texture = ChunkRenderer.getOrUploadMinimapChunkTexture(chunkPos)

        if (texture == null) {
            drawMinimapChunkNotLoaded(env, x, y)
        } else {
            drawMinimapChunkInternal(env, x, y, texture)
        }
    }

    private fun drawMinimapChunkNotLoaded(
        env: RenderEnvironment,
        x: Int,
        y: Int,
    ) {
        renderEnvironmentForGUI {
            withColor(Color4b.BLACK) {
                val from = Vec3d(x.toDouble(), y.toDouble(), 0.0)
                val to = from + Vec3d(1.0, 1.0, 0.0)

                env.drawQuad(from, to)
            }
        }
    }

    private fun drawMinimapChunkInternal(
        env: RenderEnvironment,
        x: Int,
        y: Int,
        texture: NativeImageBackedTexture,
    ) {
        val from = Vec3d(x.toDouble(), y.toDouble(), 0.0)
        val to = from + Vec3d(1.0, 1.0, 0.0)

        texture.bindTexture()
        // Prepare OpenGL for 2D textures and bind our texture
        RenderSystem.bindTexture(texture.glId)

        RenderSystem.setShaderTexture(0, texture.glId)

        env.drawTextureQuad(from, to)
    }

    override fun enable() {
        ChunkScanner.subscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
    }

    override fun disable() {
        ChunkScanner.unsubscribe(ChunkRenderer.MinimapChunkUpdateSubscriber)
        ChunkRenderer.deleteAllTextures()
    }
}
