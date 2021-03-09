/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityLivingBase
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.utils.render.shader.shaders.GlowShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.OutlineShader
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.opengl.GL11
import org.lwjgl.util.vector.Vector3f
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

@ModuleInfo(name = "ESP", description = "Allows you to see targets through walls.", category = ModuleCategory.RENDER)
class ESP : Module()
{
	val modeValue = ListValue("Mode", arrayOf("Box", "OtherBox", "WireFrame", "2D", "Real2D", "Outline", "ShaderOutline", "ShaderGlow", "Fill", "CSGO"), "Box")

	private val real2DWidth = FloatValue("Real2D-Width", 1f, 0.5f, 5f)

	val outlineWidth = FloatValue("Outline-Width", 3f, 0.5f, 5f)

	val wireframeWidth = FloatValue("WireFrame-Width", 2f, 0.5f, 5f)

	private val shaderOutlineRadius = FloatValue("ShaderOutline-Radius", 1.35f, 1f, 2f)

	private val shaderGlowRadius = FloatValue("ShaderGlow-Radius", 2.3f, 2f, 3f)

	private val colorValue = ListValue("Color", arrayOf("Static", "Rainbow", "Team", "Health"), "Static")
	private val colorRedValue = IntegerValue("R", 255, 0, 255)
	private val colorGreenValue = IntegerValue("G", 255, 0, 255)
	private val colorBlueValue = IntegerValue("B", 255, 0, 255)
	private val colorAlphaValue = IntegerValue("Alpha", 255, 0, 255)

	private val botValue = BoolValue("Bots", true)
	private val friendValue = BoolValue("Friends", true)
	private val targetValue = BoolValue("Targets", true)
	private val hurtValue = BoolValue("Hurt", true)

	private val healthModeValue = ListValue("PlayerHealthMethod", arrayOf("Datawatcher", "Mineplex", "Hive"), "Datawatcher")

	private val saturationValue = FloatValue("Rainbow-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("Rainbow-Brightness", 1.0f, 0.0f, 1.0f)

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val mode = modeValue.get().toLowerCase()
		val mvMatrix = WorldToScreen.getMatrix(GL11.GL_MODELVIEW_MATRIX)
		val projectionMatrix = WorldToScreen.getMatrix(GL11.GL_PROJECTION_MATRIX)
		val real2d = mode.equals("Real2D", ignoreCase = true)

		val displayWidth = mc.displayWidth
		val displayHeight = mc.displayHeight

		val renderPartialTicks = mc.timer.renderPartialTicks

		val renderManager = mc.renderManager
		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		val provider = classProvider

		if (real2d)
		{
			GL11.glPushAttrib(GL11.GL_ENABLE_BIT)
			GL11.glEnable(GL11.GL_BLEND)
			GL11.glDisable(GL11.GL_TEXTURE_2D)
			GL11.glDisable(GL11.GL_DEPTH_TEST)
			GL11.glMatrixMode(GL11.GL_PROJECTION)
			GL11.glPushMatrix()
			GL11.glLoadIdentity()
			GL11.glOrtho(0.0, displayWidth.toDouble(), displayHeight.toDouble(), 0.0, -1.0, 1.0)
			GL11.glMatrixMode(GL11.GL_MODELVIEW)
			GL11.glPushMatrix()
			GL11.glLoadIdentity()
			GL11.glDisable(GL11.GL_DEPTH_TEST)
			GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

			provider.glStateManager.enableTexture2D()

			GL11.glDepthMask(true)
			GL11.glLineWidth(real2DWidth.get())
		}

		val draw: (entity: IEntityLivingBase, color: Color) -> Unit = { entityLiving, color ->
			val lastTickPosX = entityLiving.lastTickPosX
			val lastTickPosY = entityLiving.lastTickPosY
			val lastTickPosZ = entityLiving.lastTickPosZ

			val posX = entityLiving.posX
			val posY = entityLiving.posY
			val posZ = entityLiving.posZ

			when (mode)
			{
				"box", "otherbox" -> RenderUtils.drawEntityBox(entityLiving, color, mode == "box", false)

				"2d" ->
				{
					val x = lastTickPosX + (posX - lastTickPosX) * renderPartialTicks - renderPosX
					val y = lastTickPosY + (posY - lastTickPosY) * renderPartialTicks - renderPosY
					val z = lastTickPosZ + (posZ - lastTickPosZ) * renderPartialTicks - renderPosZ

					RenderUtils.draw2D(entityLiving, x, y, z, color.rgb, -16777216)
				}

				"real2d" ->
				{
					val bb = entityLiving.entityBoundingBox.offset(-posX, -posY, -posZ).offset(lastTickPosX + (posX - lastTickPosX) * renderPartialTicks, lastTickPosY + (posY - lastTickPosY) * renderPartialTicks, lastTickPosZ + (posZ - lastTickPosZ) * renderPartialTicks).offset(-renderPosX, -renderPosY, -renderPosZ)
					val bbMinX = bb.minX.toFloat()
					val bbMinY = bb.minY.toFloat()
					val bbMinZ = bb.minZ.toFloat()
					val bbMaxX = bb.maxX.toFloat()
					val bbMaxY = bb.maxY.toFloat()
					val bbMaxZ = bb.maxZ.toFloat()

					val boxVertices = arrayOf(floatArrayOf(bbMinX, bbMinY, bbMinZ), floatArrayOf(bbMinX, bbMaxY, bbMinZ), floatArrayOf(bbMaxX, bbMaxY, bbMinZ), floatArrayOf(bbMaxX, bbMinY, bbMinZ), floatArrayOf(bbMinX, bbMinY, bbMaxZ), floatArrayOf(bbMinX, bbMaxY, bbMaxZ), floatArrayOf(bbMaxX, bbMaxY, bbMaxZ), floatArrayOf(bbMaxX, bbMinY, bbMaxZ))

					var minX = Float.MAX_VALUE
					var minY = Float.MAX_VALUE
					var maxX = -1f
					var maxY = -1f

					boxVertices.mapNotNull { WorldToScreen.worldToScreen(Vector3f(it[0], it[1], it[2]), mvMatrix, projectionMatrix, displayWidth, displayHeight) }.forEach { screenPos ->
						minX = min(screenPos.x, minX)
						minY = min(screenPos.y, minY)
						maxX = max(screenPos.x, maxX)
						maxY = max(screenPos.y, maxY)
					}

					if (minX > 0 || minY > 0 || maxX <= displayWidth || maxY <= displayWidth)
					{
						RenderUtils.glColor(color)

						GL11.glBegin(GL11.GL_LINE_LOOP)
						GL11.glVertex2f(minX, minY)
						GL11.glVertex2f(minX, maxY)
						GL11.glVertex2f(maxX, maxY)
						GL11.glVertex2f(maxX, minY)
						GL11.glEnd()
					}
				}
			}
		}

		val bot = botValue.get()
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		theWorld.loadedEntityList.asSequence().filter { EntityUtils.isSelected(it, false) }.map(IEntity::asEntityLivingBase).run { if (bot) this else filter { !AntiBot.isBot(theWorld, thePlayer, it) } }.filter { it != thePlayer }.forEach { draw(it, getColor(it)) }

		if (real2d)
		{
			GL11.glEnable(GL11.GL_DEPTH_TEST)
			GL11.glMatrixMode(GL11.GL_PROJECTION)
			GL11.glPopMatrix()
			GL11.glMatrixMode(GL11.GL_MODELVIEW)
			GL11.glPopMatrix()
			GL11.glPopAttrib()
		}
	}

	@EventTarget
	fun onRender2D(event: Render2DEvent)
	{
		val mode = modeValue.get().toLowerCase()
		val shader = (if (mode.equals("ShaderOutline", ignoreCase = true)) OutlineShader.INSTANCE else if (mode.equals("ShaderGlow", ignoreCase = true)) GlowShader.INSTANCE else null) ?: return

		val renderPartialTicks = mc.timer.renderPartialTicks
		val renderManager = mc.renderManager

		val bot = botValue.get()

		shader.startDraw(event.partialTicks)

		renderNameTags = false

		try
		{
			val theWorld = mc.theWorld ?: return
			val thePlayer = mc.thePlayer ?: return

			theWorld.loadedEntityList.filter { EntityUtils.isSelected(it, false) }.map(IEntity::asEntityLivingBase).run { if (bot) this else filter { AntiBot.isBot(theWorld, thePlayer, it) } }.forEach { renderManager.renderEntityStatic(it, renderPartialTicks, true) }
		}
		catch (ex: Exception)
		{
			ClientUtils.logger.error("An error occurred while rendering all entities for shader esp", ex)
		}

		renderNameTags = true

		shader.stopDraw(getColor(null), if (mode.equals("ShaderOutline", ignoreCase = true)) shaderOutlineRadius.get() else if (mode.equals("ShaderGlow", ignoreCase = true)) shaderGlowRadius.get() else 1f, 1f)
	}

	fun getColor(entity: IEntity?): Color = ColorUtils.getESPColor(entity = entity, colorMode = colorValue.get(), customStaticColor = Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get()), healthMode = healthModeValue.get(), indicateHurt = hurtValue.get(), indicateTarget = targetValue.get(), indicateFriend = friendValue.get(), rainbowSaturation = saturationValue.get(), rainbowBrightness = brightnessValue.get(), alpha = colorAlphaValue.get())

	override val tag: String
		get() = modeValue.get()

	companion object
	{
		var renderNameTags = true
	}
}
