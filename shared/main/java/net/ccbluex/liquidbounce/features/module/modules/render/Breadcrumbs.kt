/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.render.ColorUtils.rainbow
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.util.*

@ModuleInfo(name = "Breadcrumbs", description = "Leaves a trail behind you.", category = ModuleCategory.RENDER)
class Breadcrumbs : Module()
{
	val colorRedValue = IntegerValue("R", 255, 0, 255)
	val colorGreenValue = IntegerValue("G", 179, 0, 255)
	val colorBlueValue = IntegerValue("B", 72, 0, 255)
	val colorAlphaValue = IntegerValue("Alpha", 255, 0, 255)

	val colorRainbow = BoolValue("Rainbow", false)
	val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	private val positions = LinkedList<DoubleArray>()

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent?)
	{
		val color = if (colorRainbow.get()) rainbow(colorAlphaValue.get(), saturation = saturationValue.get(), brightness = brightnessValue.get()) else Color(colorRedValue.get(), colorGreenValue.get(), colorBlueValue.get())

		val renderManager = mc.renderManager
		val renderPosX = renderManager.viewerPosX
		val renderPosY = renderManager.viewerPosY
		val renderPosZ = renderManager.viewerPosZ

		val entityRenderer = mc.entityRenderer

		synchronized(positions) {
			glPushMatrix()
			glDisable(GL_TEXTURE_2D)
			glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
			glEnable(GL_LINE_SMOOTH)
			glEnable(GL_BLEND)
			glDisable(GL_DEPTH_TEST)

			entityRenderer.disableLightmap()

			glBegin(GL_LINE_STRIP)
			RenderUtils.glColor(color)

			for (pos in positions) glVertex3d(pos[0] - renderPosX, pos[1] - renderPosY, pos[2] - renderPosZ)

			glColor4d(1.0, 1.0, 1.0, 1.0)
			glEnd()
			glEnable(GL_DEPTH_TEST)
			glDisable(GL_LINE_SMOOTH)
			glDisable(GL_BLEND)
			glEnable(GL_TEXTURE_2D)
			glPopMatrix()
		}
	}

	@EventTarget
	fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent?)
	{
		val thePlayer = mc.thePlayer ?: return

		synchronized(positions) {
			positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ))
		}
	}

	override fun onEnable()
	{
		val thePlayer = mc.thePlayer ?: return

		synchronized(positions) {
			positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight * 0.5f, thePlayer.posZ))
			positions.add(doubleArrayOf(thePlayer.posX, thePlayer.entityBoundingBox.minY, thePlayer.posZ))
		}
		super.onEnable()
	}

	override fun onDisable()
	{
		synchronized(positions, positions::clear)
		super.onDisable()
	}

	override val tag: String
		get() = "${positions.size}"
}
