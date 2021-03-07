/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.isClientFriend
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import org.lwjgl.opengl.GL11
import java.awt.Color

@ModuleInfo(name = "Tracers", description = "Draws a line to targets around you.", category = ModuleCategory.RENDER)
class Tracers : Module()
{

	private val colorMode = ListValue("Color", arrayOf("Custom", "DistanceColor", "Rainbow"), "Custom")

	private val thicknessValue = FloatValue("Thickness", 2F, 1F, 5F)

	private val colorRedValue = IntegerValue("R", 0, 0, 255)
	private val colorGreenValue = IntegerValue("G", 160, 0, 255)
	private val colorBlueValue = IntegerValue("B", 255, 0, 255)

	private val botValue = BoolValue("Bots", true)

	private val saturationValue = FloatValue("HSB-Saturation", 1.0f, 0.0f, 1.0f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1.0f, 0.0f, 1.0f)

	@EventTarget
	fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
	{
		val theWorld = mc.theWorld ?: return
		val thePlayer = mc.thePlayer ?: return

		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
		GL11.glEnable(GL11.GL_BLEND)
		GL11.glEnable(GL11.GL_LINE_SMOOTH)
		GL11.glLineWidth(thicknessValue.get())
		GL11.glDisable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(false)

		GL11.glBegin(GL11.GL_LINES)

		val colorRed = colorRedValue.get()
		val colorGreen = colorGreenValue.get()
		val greenBlue = colorBlueValue.get()

		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()

		val colorMode = colorMode.get().toLowerCase()

		val partialTicks = mc.timer.renderPartialTicks

		val renderManager = mc.renderManager
		val renderPosX = renderManager.renderPosX
		val renderPosY = renderManager.renderPosY
		val renderPosZ = renderManager.renderPosZ

		val rotationYawRadians = WMathHelper.toRadians(thePlayer.rotationYaw)
		val rotationPitchRadians = WMathHelper.toRadians(thePlayer.rotationPitch)

		val eyeHeight = thePlayer.eyeHeight.toDouble()
		val eyeVector = WVec3(0.0, 0.0, 1.0).rotatePitch(-rotationPitchRadians).rotateYaw(-rotationYawRadians).addVector(0.0, eyeHeight, 0.0)
		val eyeX = eyeVector.xCoord
		val eyeY = eyeVector.yCoord
		val eyeZ = eyeVector.zCoord

		val color = when
		{
			colorMode.equals("Custom", ignoreCase = true) -> Color(colorRed, colorGreen, greenBlue, 150)
			colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(saturation = saturation, brightness = brightness)
			else -> Color(255, 255, 255, 150)
		}

		val bot = botValue.get()
		val provider = classProvider

		theWorld.loadedEntityList.filter { EntityUtils.isSelected(it, false) }.map(IEntity::asEntityLivingBase).filter { it != thePlayer }.run { if (bot) this else filter { !AntiBot.isBot(theWorld, thePlayer, it) } }.forEach { entity ->
			val distance = (thePlayer.getDistanceToEntity(entity) * 2).toInt().coerceAtMost(255)

			val lastTickPosX = entity.lastTickPosX
			val lastTickPosY = entity.lastTickPosY
			val lastTickPosZ = entity.lastTickPosZ

			val x = (lastTickPosX + (entity.posX - lastTickPosX) * partialTicks - renderPosX)
			val y = (lastTickPosY + (entity.posY - lastTickPosY) * partialTicks - renderPosY)
			val z = (lastTickPosZ + (entity.posZ - lastTickPosZ) * partialTicks - renderPosZ)

			RenderUtils.glColor(when
			{
				provider.isEntityPlayer(entity) && entity.asEntityPlayer().isClientFriend() -> Color(0, 0, 255, 150)
				colorMode.equals("DistanceColor", ignoreCase = true) -> Color(255 - distance, distance, 0, 150)
				else -> color
			})

			GL11.glVertex3d(eyeX, eyeY, eyeZ)
			GL11.glVertex3d(x, y, z)
			GL11.glVertex3d(x, y, z)
			GL11.glVertex3d(x, y + entity.height, z)
		}

		GL11.glEnd()

		GL11.glEnable(GL11.GL_TEXTURE_2D)
		GL11.glDisable(GL11.GL_LINE_SMOOTH)
		GL11.glEnable(GL11.GL_DEPTH_TEST)
		GL11.glDepthMask(true)
		GL11.glDisable(GL11.GL_BLEND)
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)
	}
}
