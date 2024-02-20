/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.render.ESP
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.extensions.toRadians
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorder
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRectNew
import net.ccbluex.liquidbounce.utils.render.RenderUtils.glColor
import net.ccbluex.liquidbounce.utils.render.RenderUtils.makeScissorBox
import net.ccbluex.liquidbounce.utils.render.SafeVertexBuffer
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.renderer.GlStateManager.bindTexture
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexBuffer
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.*

@ElementInfo(name = "Radar", disableScale = true, priority = 1)
class Radar(x: Double = 5.0, y: Double = 130.0) : Element(x, y) {

    companion object {
        private val SQRT_OF_TWO = sqrt(2f)
    }

    private val size by FloatValue("Size", 90f, 30f..500f)
    private val viewDistance by FloatValue("View Distance", 4F, 0.5F..32F)

    private val playerShape by ListValue("Player Shape", arrayOf("Triangle", "Rectangle", "Circle"), "Triangle")
    private val playerSize by FloatValue("Player Size", 2f, 0.5f..20F)
    private val useESPColors by BoolValue("Use ESP Colors", true)
    private val fovSize by FloatValue("FOV Size", 10F, 0F..50F)
    private val fovAngle by FloatValue("FOV Angle", 70F, 30F..160F)

    private val minimap by BoolValue("Minimap", true)

    private val backgroundRed by IntegerValue("Background Red", 0, 0..255)
    private val backgroundGreen by IntegerValue("Background Green", 0, 0..255)
    private val backgroundBlue by IntegerValue("Background Blue", 0, 0..255)
    private val backgroundAlpha by IntegerValue("Background Alpha", 50, 0..255)

    private val borderStrength by FloatValue("Border Strength", 2F, 1F..5F)

    private val borderRainbow by BoolValue("Border Rainbow", false)
        private val rainbowX by FloatValue("Rainbow-X", -1000F, -2000F..2000F) { borderRainbow }
        private val rainbowY by FloatValue("Rainbow-Y", -1000F, -2000F..2000F) { borderRainbow }

        private val borderRed by IntegerValue("Border Red", 0, 0..255) { !borderRainbow }
        private val borderGreen by IntegerValue("Border Green", 0, 0..255) { !borderRainbow }
        private val borderBlue by IntegerValue("Border Blue", 0, 0..255) { !borderRainbow }
        private val borderAlpha by IntegerValue("Border Alpha", 150, 0..255) { !borderRainbow }

    private var fovMarkerVertexBuffer: VertexBuffer? = null
    private var lastFov = 0f

    override fun drawElement(): Border {
        if (lastFov != fovAngle || fovMarkerVertexBuffer == null) {
            // Free Memory
            fovMarkerVertexBuffer?.deleteGlBuffers()

            fovMarkerVertexBuffer = createFovIndicator(fovAngle)
            lastFov = fovAngle
        }

        val renderViewEntity = mc.renderViewEntity

        val size = size

        if (!minimap) {
            drawRectNew(0F, 0F, size, size, Color(backgroundRed, backgroundGreen, backgroundBlue, backgroundAlpha).rgb)
        }

        val viewDistance = viewDistance * 16f

        val maxDisplayableDistanceSquare = ((viewDistance + fovSize.toDouble()) *
                (viewDistance + fovSize.toDouble()))
        val halfSize = size / 2f

        makeScissorBox(x.toFloat(), y.toFloat(), x.toFloat() + ceil(size), y.toFloat() + ceil(size))

        glEnable(GL_SCISSOR_TEST)

        glPushMatrix()

        glTranslatef(halfSize, halfSize, 0f)
        glRotatef(renderViewEntity.rotationYaw, 0f, 0f, -1f)

        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

        glColor4f(1f, 1f, 1f, 1f)

        if (minimap) {
            glEnable(GL_TEXTURE_2D)

            val chunkSizeOnScreen = size / viewDistance
            val chunksToRender = max(1, ceil((SQRT_OF_TWO * (viewDistance * 0.5f))).toInt())

            val currX = renderViewEntity.posX / 16.0
            val currZ = renderViewEntity.posZ / 16.0

            for (x in -chunksToRender..chunksToRender) {
                for (z in -chunksToRender..chunksToRender) {
                    val currChunk = MiniMapRegister.getChunkTextureAt(floor(currX).toInt() + x, floor(currZ).toInt() + z)

                    if (currChunk != null) {
                        val sc = chunkSizeOnScreen.toDouble()

                        val onScreenX = (currX - floor(currX).toLong() - 1 - x) * sc
                        val onScreenZ = (currZ - floor(currZ).toLong() - 1 - z) * sc

                        bindTexture(currChunk.texture.glTextureId)

                        glBegin(GL_QUADS)

                        glTexCoord2f(0f, 0f)
                        glVertex2d(onScreenX, onScreenZ)
                        glTexCoord2f(0f, 1f)
                        glVertex2d(onScreenX, onScreenZ + chunkSizeOnScreen)
                        glTexCoord2f(1f, 1f)
                        glVertex2d(onScreenX + chunkSizeOnScreen, onScreenZ + chunkSizeOnScreen)
                        glTexCoord2f(1f, 0f)
                        glVertex2d(onScreenX + chunkSizeOnScreen, onScreenZ)

                        glEnd()
                    }

                }
            }

            bindTexture(0)

            glDisable(GL_TEXTURE_2D)
        }

        glDisable(GL_TEXTURE_2D)
        glEnable(GL_LINE_SMOOTH)

        val triangleMode = playerShape == "Triangle"
        val circleMode = playerShape == "Circle"

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        if (circleMode) {
            glEnable(GL_POINT_SMOOTH)
        }

        var playerSize = playerSize

        glEnable(GL_POLYGON_SMOOTH)

        if (triangleMode) {
            playerSize *= 2
        } else {
            worldRenderer.begin(GL_POINTS, DefaultVertexFormats.POSITION)
            glPointSize(playerSize)
        }

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity != mc.thePlayer && isSelected(entity, false)) {
                val positionRelativeToPlayer = Vector2f((renderViewEntity.posX - entity.posX).toFloat(),
                        (renderViewEntity.posZ - entity.posZ).toFloat())

                if (maxDisplayableDistanceSquare < positionRelativeToPlayer.lengthSquared())
                    continue

                val transform = triangleMode || fovSize > 0F

                if (transform) {
                    glPushMatrix()

                    glTranslatef((positionRelativeToPlayer.x / viewDistance) * size,
                            (positionRelativeToPlayer.y / viewDistance) * size, 0f)
                    glRotatef(entity.rotationYaw, 0f, 0f, 1f)
                }

                if (fovSize > 0F) {
                    glPushMatrix()
                    glRotatef(180f, 0f, 0f, 1f)
                    val sc = (fovSize / viewDistance) * size
                    glScalef(sc, sc, sc)

                    glColor4f(1f, 1f, 1f, if (minimap) 0.75f else 0.25f)

                    val vbo = fovMarkerVertexBuffer!!

                    vbo.bindBuffer()

                    glEnableClientState(GL_VERTEX_ARRAY)
                    glVertexPointer(3, GL_FLOAT, 12, 0L)

                    vbo.drawArrays(GL_TRIANGLE_FAN)
                    vbo.unbindBuffer()

                    glDisableClientState(GL_VERTEX_ARRAY)

                    glPopMatrix()
                }

                if (triangleMode) {
                    if (useESPColors) {
                        val color = ESP.getColor(entity)

                        glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, 1f)
                    } else {
                        glColor4f(1f, 1f, 1f, 1f)
                    }

                    glBegin(GL_TRIANGLES)

                    glVertex2f(-playerSize * 0.25f, playerSize * 0.5f)
                    glVertex2f(playerSize * 0.25f, playerSize * 0.5f)
                    glVertex2f(0f, -playerSize * 0.5f)

                    glEnd()
                } else {
                    val color = ESP.getColor(entity)

                    worldRenderer.pos(((positionRelativeToPlayer.x / viewDistance) * size).toDouble(), ((positionRelativeToPlayer.y / viewDistance) * size).toDouble(), 0.0)
                            .color(color.red / 255f, color.green / 255f,
                                    color.blue / 255f, 1f).endVertex()
                }

                if (transform) {
                    glPopMatrix()
                }

            }
        }

        if (!triangleMode)
            tessellator.draw()

        if (circleMode) {
            glDisable(GL_POINT_SMOOTH)
        }

        glDisable(GL_POLYGON_SMOOTH)

        glEnable(GL_TEXTURE_2D)
        glDisable(GL_BLEND)
        glDisable(GL_LINE_SMOOTH)

        glDisable(GL_SCISSOR_TEST)

        glPopMatrix()

        RainbowShader.begin(borderRainbow, if (rainbowX == 0f) 0f else 1f / rainbowX,
                if (rainbowY == 0f) 0f else 1f / rainbowY, System.currentTimeMillis() % 10000 / 10000F).use {
            drawBorder(0F, 0F, size, size, borderStrength, Color(borderRed,
                    borderGreen, borderBlue, borderAlpha).rgb)

            glEnable(GL_BLEND)
            glDisable(GL_TEXTURE_2D)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            glEnable(GL_LINE_SMOOTH)

            glColor(borderRed, borderGreen, borderBlue, borderAlpha)
            glLineWidth(borderStrength)

            glBegin(GL_LINES)

            glVertex2f(halfSize, 0f)
            glVertex2f(halfSize, size)

            glVertex2f(0f, halfSize)
            glVertex2f(size, halfSize)

            glEnd()

            glEnable(GL_TEXTURE_2D)
            glDisable(GL_BLEND)
            glDisable(GL_LINE_SMOOTH)
        }

        glColor4f(1f, 1f, 1f, 1f)

        return Border(0F, 0F, size, size)
    }

    private fun createFovIndicator(angle: Float): VertexBuffer {
        // Rendering
        val worldRenderer = Tessellator.getInstance().worldRenderer

        worldRenderer.begin(GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION)

        val start = (90f - (angle * 0.5f)).toRadians()
        val end = (90f + (angle * 0.5f)).toRadians()

        var curr = end
        val radius = 1.0

        worldRenderer.pos(0.0, 0.0, 0.0).endVertex()

        while (curr >= start) {
            worldRenderer.pos(cos(curr) * radius, sin(curr) * radius, 0.0).endVertex()

            curr -= 0.15f
        }

        // Uploading to VBO

        val safeVertexBuffer = SafeVertexBuffer(worldRenderer.vertexFormat)

        worldRenderer.finishDrawing()
        worldRenderer.reset()
        safeVertexBuffer.bufferData(worldRenderer.byteBuffer)

        return safeVertexBuffer
    }

}