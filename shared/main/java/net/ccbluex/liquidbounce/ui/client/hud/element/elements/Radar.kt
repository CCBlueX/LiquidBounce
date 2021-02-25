/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.WDefaultVertexFormats
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.vertex.IVertexBuffer
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.features.module.modules.render.ESP
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11.*
import org.lwjgl.util.vector.Vector2f
import java.awt.Color
import kotlin.math.*

@ElementInfo(name = "Radar", disableScale = true, priority = 1)
class Radar(x: Double = 5.0, y: Double = 130.0) : Element(x, y)
{

	companion object
	{
		private val SQRT_OF_TWO = sqrt(2f)
	}

	private val sizeValue = FloatValue("Size", 90f, 30f, 500f)
	private val viewDistanceValue = FloatValue("View Distance", 4F, 0.5F, 32F)

	private val playerShapeValue = ListValue("Player Shape", arrayOf("Triangle", "Rectangle", "Circle"), "Triangle")
	private val playerSizeValue = FloatValue("Player Size", 2.0F, 0.5f, 20F)
	private val useESPColorsValue = BoolValue("Use ESP Colors", true)
	private val fovSizeValue = FloatValue("FOV Size", 10F, 0F, 50F)
	private val fovAngleValue = FloatValue("FOV Angle", 70F, 30F, 160F)

	private val minimapValue = BoolValue("Minimap", true)

	private val rainbowXValue = FloatValue("Rainbow-X", -1000F, -2000F, 2000F)
	private val rainbowYValue = FloatValue("Rainbow-Y", -1000F, -2000F, 2000F)

	private val backgroundRedValue = IntegerValue("Background Red", 0, 0, 255)
	private val backgroundGreenValue = IntegerValue("Background Green", 0, 0, 255)
	private val backgroundBlueValue = IntegerValue("Background Blue", 0, 0, 255)
	private val backgroundAlphaValue = IntegerValue("Background Alpha", 50, 0, 255)

	private val borderStrengthValue = FloatValue("Border Strength", 2F, 1F, 5F)
	private val borderRedValue = IntegerValue("Border Red", 0, 0, 255)
	private val borderGreenValue = IntegerValue("Border Green", 0, 0, 255)
	private val borderBlueValue = IntegerValue("Border Blue", 0, 0, 255)
	private val borderAlphaValue = IntegerValue("Border Alpha", 150, 0, 255)
	private val borderRainbowValue = BoolValue("Border Rainbow", false)

	private var fovMarkerVertexBuffer: IVertexBuffer? = null
	private var lastFov = 0f

	override fun drawElement(): Border
	{
		MiniMapRegister.updateChunks()

		val fovAngle = fovAngleValue.get()

		if (lastFov != fovAngle || fovMarkerVertexBuffer == null)
		{ // Free Memory
			fovMarkerVertexBuffer?.deleteGlBuffers()

			fovMarkerVertexBuffer = createFovIndicator(fovAngle)
			lastFov = fovAngle
		}

		val renderViewEntity = mc.renderViewEntity!!

		val size = sizeValue.get()

		val minimap = minimapValue.get()
		if (!minimap) RenderUtils.drawRect(0F, 0F, size, size, Color(backgroundRedValue.get(), backgroundGreenValue.get(), backgroundBlueValue.get(), backgroundAlphaValue.get()).rgb)

		val viewDistance = viewDistanceValue.get() * 16.0F

		val fovSize = fovSizeValue.get()
		val maxDisplayableDistanceSquare = ((viewDistance + fovSize.toDouble()) * (viewDistance + fovSize.toDouble()))
		val halfSize = size * 0.5f

		RenderUtils.makeScissorBox(x.toFloat(), y.toFloat(), x.toFloat() + ceil(size), y.toFloat() + ceil(size))

		glEnable(GL_SCISSOR_TEST)

		glPushMatrix()

		glTranslatef(halfSize, halfSize, 0f)
		glRotatef(renderViewEntity.rotationYaw, 0f, 0f, -1f)

		glEnable(GL_BLEND)
		glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

		glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

		val renderX = renderViewEntity.posX
		val renderZ = renderViewEntity.posZ

		val provider = classProvider

		if (minimap)
		{
			glEnable(GL_TEXTURE_2D)

			val chunkSizeOnScreen = size / viewDistanceValue.get()
			val chunksToRender = max(1, ceil((SQRT_OF_TWO * (viewDistanceValue.get() * 0.5f))).toInt())

			val currX = renderX * 0.0625
			val currZ = renderZ * 0.0625

			val glStateManager = provider.glStateManager
			for (x in -chunksToRender..chunksToRender)
			{
				for (z in -chunksToRender..chunksToRender)
				{
					val currChunk = MiniMapRegister.getChunkTextureAt(floor(currX).toInt() + x, floor(currZ).toInt() + z) ?: continue

					val sc = chunkSizeOnScreen.toDouble()

					val onScreenX = (currX - floor(currX).toLong() - 1 - x) * sc
					val onScreenZ = (currZ - floor(currZ).toLong() - 1 - z) * sc

					glStateManager.bindTexture(currChunk.texture.glTextureId)

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

			glStateManager.bindTexture(0)

			glDisable(GL_TEXTURE_2D)
		}

		glDisable(GL_TEXTURE_2D)
		glEnable(GL_LINE_SMOOTH)

		val triangleMode = playerShapeValue.get().equals("triangle", true)
		val circleMode = playerShapeValue.get().equals("circle", true)

		val tessellator = provider.tessellatorInstance
		val worldRenderer = tessellator.worldRenderer

		if (circleMode)
		{
			glEnable(GL_POINT_SMOOTH)
		}

		var playerSize = playerSizeValue.get()

		glEnable(GL_POLYGON_SMOOTH)

		if (triangleMode)
		{
			playerSize *= 2
		}
		else
		{
			worldRenderer.begin(GL_POINTS, provider.getVertexFormatEnum(WDefaultVertexFormats.POSITION))
			glPointSize(playerSize)
		}

		val theWorld = mc.theWorld!!
		val thePlayer = mc.thePlayer!!

		val esp = LiquidBounce.moduleManager[ESP::class.java] as ESP
		val useESPColors = useESPColorsValue.get()

		theWorld.loadedEntityList.filter { EntityUtils.isSelected(it, false) }.filter { it != thePlayer }.forEach { entity ->
			val positionRelativeToPlayer = Vector2f((renderX - entity.posX).toFloat(), (renderZ - entity.posZ).toFloat())

			if (maxDisplayableDistanceSquare < positionRelativeToPlayer.lengthSquared()) return@forEach

			val transform = triangleMode || fovSize > 0F

			val relX = positionRelativeToPlayer.x
			val relY = positionRelativeToPlayer.y

			if (transform)
			{
				glPushMatrix()

				glTranslatef((relX / viewDistance) * size, (relY / viewDistance) * size, 0f)
				glRotatef(entity.rotationYaw, 0f, 0f, 1f)
			}

			if (fovSize > 0F)
			{
				glPushMatrix()
				glRotatef(180.0f, 0f, 0f, 1f)
				val sc = (fovSize / viewDistance) * size
				glScalef(sc, sc, sc)

				glColor4f(1.0f, 1.0f, 1.0f, if (minimap) 0.75f else 0.25f)

				val vbo = fovMarkerVertexBuffer!!

				vbo.bindBuffer()

				glEnableClientState(GL_VERTEX_ARRAY)
				glVertexPointer(3, GL_FLOAT, 12, 0L)

				vbo.drawArrays(GL_TRIANGLE_FAN)
				vbo.unbindBuffer()

				glDisableClientState(GL_VERTEX_ARRAY)

				glPopMatrix()
			}

			if (triangleMode)
			{
				if (useESPColors)
				{
					val color = esp.getColor(entity)
					glColor4f(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f, 1.0f)
				}
				else glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

				glBegin(GL_TRIANGLES)

				glVertex2f(-playerSize * 0.25f, playerSize * 0.5f)
				glVertex2f(playerSize * 0.25f, playerSize * 0.5f)
				glVertex2f(0f, -playerSize * 0.5f)

				glEnd()
			}
			else
			{
				val color = esp.getColor(entity)

				worldRenderer.pos(((relX / viewDistance) * size).toDouble(), ((relY / viewDistance) * size).toDouble(), 0.0).color(color.red / 255.0f, color.green / 255.0f, color.blue / 255.0f, 1.0f).endVertex()
			}

			if (transform) glPopMatrix()
		}

		if (!triangleMode) tessellator.draw()

		if (circleMode) glDisable(GL_POINT_SMOOTH)

		glDisable(GL_POLYGON_SMOOTH)

		glEnable(GL_TEXTURE_2D)
		glDisable(GL_BLEND)
		glDisable(GL_LINE_SMOOTH)

		glDisable(GL_SCISSOR_TEST)

		glPopMatrix()

		val rainbowShaderX = if (rainbowXValue.get() == 0.0F) 0.0F else 1.0F / rainbowXValue.get()
		val rainbowShaderY = if (rainbowYValue.get() == 0.0F) 0.0F else 1.0F / rainbowYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

		RainbowShader.begin(borderRainbowValue.get(), rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
			RenderUtils.drawBorder(0F, 0F, size, size, borderStrengthValue.get(), Color(borderRedValue.get(), borderGreenValue.get(), borderBlueValue.get(), borderAlphaValue.get()).rgb)

			glEnable(GL_BLEND)
			glDisable(GL_TEXTURE_2D)
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
			glEnable(GL_LINE_SMOOTH)

			RenderUtils.glColor(borderRedValue.get(), borderGreenValue.get(), borderBlueValue.get(), borderAlphaValue.get())
			glLineWidth(borderStrengthValue.get())

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

		glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

		return Border(0F, 0F, size, size)
	}

	private fun createFovIndicator(angle: Float): IVertexBuffer
	{
		val provider = classProvider

		// Rendering
		val worldRenderer = provider.tessellatorInstance.worldRenderer

		worldRenderer.begin(GL_TRIANGLE_FAN, provider.getVertexFormatEnum(WDefaultVertexFormats.POSITION))

		val start = WMathHelper.toRadians(90.0f - angle * 0.5f)
		val end = WMathHelper.toRadians(90.0f + angle * 0.5f)

		var curr = end
		val radius = 1.0

		worldRenderer.pos(0.0, 0.0, 0.0).endVertex()

		val func = functions

		while (curr >= start)
		{
			worldRenderer.pos(func.cos(curr) * radius, func.sin(curr) * radius, 0.0).endVertex()

			curr -= 0.15f
		}

		// Uploading to VBO

		val safeVertexBuffer = provider.createSafeVertexBuffer(worldRenderer.vertexFormat)

		worldRenderer.finishDrawing()
		worldRenderer.reset()
		safeVertexBuffer.bufferData(worldRenderer.byteBuffer)

		return safeVertexBuffer
	}
}
