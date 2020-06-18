/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.render.ESP
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.SafeVertexBuffer
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.Tessellator
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.client.renderer.vertex.VertexBuffer
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

@ElementInfo(name = "Radar")
class Radar : Element() {
    private val sizeValue = FloatValue("Size", 0.125F, 0.001F, 0.5F)
    private val viewDistanceValue = FloatValue("View Distance", 4F, 0.5F, 32F)

    private val modeValue = ListValue("Mode", arrayOf("Triangle", "Rectangle", "Circle"), "Triangle")

    private val playerSize = FloatValue("Player Size", 2.0F, 0.5f, 20F)

    private val useESPColorsValue = BoolValue("Use ESP Colors", true)

    private val backgroundValue = BoolValue("Background", true)

    private val backgroundRedValue = IntegerValue("Background Red", 0, 0, 255)
    private val backgroundGreenValue = IntegerValue("Background Green", 0, 0, 255)
    private val backgroundBlueValue = IntegerValue("Background Blue", 0, 0, 255)
    private val backgroundAlphaValue = IntegerValue("Background Alpha", 50, 0, 255)

    private val borderValue = BoolValue("Border", true)
    private val crosshairValue = BoolValue("Crosshair", true)

    private val borderStrength = FloatValue("Line Strength", 2F, 1F, 5F)
    private val borderRedValue = IntegerValue("Line Red", 0, 0, 255)
    private val borderGreenValue = IntegerValue("Line Green", 0, 0, 255)
    private val borderBlueValue = IntegerValue("Line Blue", 0, 0, 255)
    private val borderAlphaValue = IntegerValue("Line Alpha", 150, 0, 255)

    private val borderRainbow = BoolValue("Line Rainbow", false)

    private val rainbowX = FloatValue("Rainbow-X", -1000F, -2000F, 2000F)
    private val rainbowY = FloatValue("Rainbow-Y", -1000F, -2000F, 2000F)

    private val drawFovIndicatorValue = BoolValue("FOV Indicator", true)

    private val fovViewDistance = FloatValue("FOV View Distance", 10F, 0.5F, 100F)
    private val fovAngle = FloatValue("FOV Angle", 70F, 30F, 160F)

    private var fovMarkerVertexBuffer: VertexBuffer? = null
    private var lastFov = 0f

    override fun drawElement(): Border? {
        val fovAngle = fovAngle.get()

        if (lastFov != fovAngle || fovMarkerVertexBuffer == null) {
            // Free Memory
            fovMarkerVertexBuffer?.deleteGlBuffers()

            fovMarkerVertexBuffer = createFovIndicator(fovAngle)
            lastFov = fovAngle
        }

        val size = ScaledResolution(mc).scaledWidth * sizeValue.get()

        if (backgroundValue.get()) {
            RenderUtils.drawRect(0F, 0F, size, size, Color(backgroundRedValue.get(), backgroundGreenValue.get(), backgroundBlueValue.get(), backgroundAlphaValue.get()).rgb)
        }

        val viewDistance = viewDistanceValue.get() * 16.0F

        val maxDisplayableDistanceSquare = (viewDistance * viewDistance) * 2.0F
        val halfSize = size / 2f

        RenderUtils.makeScissorBox(x.toFloat(), y.toFloat(), x.toFloat() + ceil(size), y.toFloat() + ceil(size))

        GL11.glEnable(GL11.GL_SCISSOR_TEST)

        GL11.glPushMatrix()

        GL11.glTranslatef(halfSize, halfSize, 0f)
        GL11.glRotatef(mc.thePlayer.rotationYaw, 0f, 0f, -1f)

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)

        val triangleMode = modeValue.get().equals("triangle", true)
        val rectangleMode = modeValue.get().equals("rectangle", true)
        val circleMode = modeValue.get().equals("circle", true)

        val tessellator = Tessellator.getInstance()
        val worldRenderer = tessellator.worldRenderer

        if (circleMode) {
            GL11.glEnable(GL11.GL_POINT_SMOOTH)
        }

        var playerSize = playerSize.get()

        GL11.glEnable(GL11.GL_POLYGON_SMOOTH)

        if (triangleMode) {
            playerSize *= 2
        } else {
            worldRenderer.begin(GL11.GL_POINTS, DefaultVertexFormats.POSITION_COLOR)
            GL11.glPointSize(playerSize)
        }

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity != null && entity !== mc.thePlayer && EntityUtils.isSelected(entity, false)) {
                val renderViewEntity = mc.renderViewEntity

                val positionRelativeToPlayer = Vector2f((renderViewEntity.posX - entity.posX).toFloat(), (renderViewEntity.posZ - entity.posZ).toFloat())

                if (maxDisplayableDistanceSquare < positionRelativeToPlayer.lengthSquared())
                    continue

                val transform = triangleMode || drawFovIndicatorValue.get()

                if (transform) {
                    GL11.glPushMatrix()

                    GL11.glTranslatef((positionRelativeToPlayer.x / viewDistance) * size, (positionRelativeToPlayer.y / viewDistance) * size, 0f)
                    GL11.glRotatef(entity.rotationYaw, 0f, 0f, 1f)
                }

                if (drawFovIndicatorValue.get()) {
                    GL11.glPushMatrix()
                    GL11.glRotatef(180.0f, 0f, 0f, 1f)
                    val sc = (fovViewDistance.get() / viewDistance) * size
                    GL11.glScalef(sc, sc, sc)

                    GL11.glColor4f(1.0f, 1.0f, 1.0f, 0.25f)

                    val vbo = fovMarkerVertexBuffer!!

                    vbo.bindBuffer()

                    GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY)
                    GL11.glVertexPointer(3, GL11.GL_FLOAT, 12, 0L)

                    vbo.drawArrays(GL11.GL_TRIANGLE_FAN)
                    vbo.unbindBuffer()

                    GL11.glDisableClientState(GL11.GL_VERTEX_ARRAY)

                    GL11.glPopMatrix()
                }

                if (triangleMode) {
                    if (useESPColorsValue.get()) {
                        val color = (LiquidBounce.moduleManager[ESP::class.java] as ESP).getColor(entity)

                        GL11.glColor4f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f, 1.0f)
                    } else {
                        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
                    }

                    GL11.glBegin(GL11.GL_TRIANGLES)

                    GL11.glVertex2f(-playerSize * 0.25f, playerSize * 0.5f)
                    GL11.glVertex2f(playerSize * 0.25f, playerSize * 0.5f)
                    GL11.glVertex2f(0f, -playerSize * 0.5f)

                    GL11.glEnd()
                } else {
                    val color = (LiquidBounce.moduleManager[ESP::class.java] as ESP).getColor(entity)

                    worldRenderer.pos(((positionRelativeToPlayer.x / viewDistance) * size).toDouble(), ((positionRelativeToPlayer.y / viewDistance) * size).toDouble(), 0.0).color(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f, 1.0f).endVertex()
                }

                if (transform) {
                    GL11.glPopMatrix()
                }

            }
        }

        if (!triangleMode)
            tessellator.draw()

        if (circleMode) {
            GL11.glDisable(GL11.GL_POINT_SMOOTH)
        }
        GL11.glDisable(GL11.GL_POLYGON_SMOOTH)

        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)

        GL11.glDisable(GL11.GL_SCISSOR_TEST)

        GL11.glPopMatrix()

        RainbowShader.begin(borderRainbow.get(), if (rainbowX.get() == 0.0F) 0.0F else 1.0F / rainbowX.get(), if (rainbowY.get() == 0.0F) 0.0F else 1.0F / rainbowY.get(), System.currentTimeMillis() % 10000 / 10000F).use {
            if (borderValue.get()) {
                RenderUtils.drawBorder(0F, 0F, size, size, borderStrength.get(), Color(borderRedValue.get(), borderGreenValue.get(), borderBlueValue.get(), borderAlphaValue.get()).rgb)
            }

            if (crosshairValue.get()) {
                GL11.glEnable(GL11.GL_BLEND)
                GL11.glDisable(GL11.GL_TEXTURE_2D)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                GL11.glEnable(GL11.GL_LINE_SMOOTH)

                RenderUtils.glColor(borderRedValue.get(), borderGreenValue.get(), borderBlueValue.get(), borderAlphaValue.get())
                GL11.glLineWidth(borderStrength.get())

                GL11.glBegin(GL11.GL_LINES)

                GL11.glVertex2f(halfSize, 0f)
                GL11.glVertex2f(halfSize, size)

                GL11.glVertex2f(0f, halfSize)
                GL11.glVertex2f(size, halfSize)

                GL11.glEnd()

                GL11.glEnable(GL11.GL_TEXTURE_2D)
                GL11.glDisable(GL11.GL_BLEND)
                GL11.glDisable(GL11.GL_LINE_SMOOTH)
            }
        }

        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

        return Border(0F, 0F, size, size)
    }

    private fun createFovIndicator(angle: Float): VertexBuffer {
        // Rendering
        val worldRenderer = Tessellator.getInstance().worldRenderer

        worldRenderer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION)

        val start = (90.0f - (angle * 0.5f)) / 180.0f * Math.PI.toFloat()
        val end = (90.0f + (angle * 0.5f)) / 180.0f * Math.PI.toFloat()

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