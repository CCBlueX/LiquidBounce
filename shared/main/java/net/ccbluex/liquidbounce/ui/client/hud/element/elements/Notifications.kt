/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.createRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max

/**
 * CustomHUD Notification element
 */
// TODO: Customizable slide-in and slide-out animation
@ElementInfo(name = "Notifications", single = true)
class Notifications(x: Double = 0.0, y: Double = 30.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
	companion object
	{
		val bodyColorModeValue = ListValue("Body-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
		val bodyRedValue = IntegerValue("Body-R", 0, 0, 255)
		val bodyGreenValue = IntegerValue("Body-G", 0, 0, 255)
		val bodyBlueValue = IntegerValue("Body-B", 0, 0, 255)
		val bodyAlphaValue = IntegerValue("Body-Alpha", 255, 0, 255)

		val rectValue = BoolValue("Rect", true)
		val rectWidthValue = FloatValue("Rect-Width", 5F, 1.5F, 8F)

		val rectColorModeValue = ListValue("Rect-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
		val rectRedValue = IntegerValue("Rect-R", 0, 0, 255)
		val rectGreenValue = IntegerValue("Rect-G", 111, 0, 255)
		val rectBlueValue = IntegerValue("Rect-B", 255, 0, 255)
		val rectAlphaValue = IntegerValue("Rect-Alpha", 255, 0, 255)

		val maxRendered = IntegerValue("MaxRendered", 6, 3, 15)

		val fadeSpeedValue = FloatValue("FadeSpeed", 0.25F, 0.1F, 0.95F)
		val deploySpeedValue = FloatValue("DeploySpeed", 0.65F, 0.40F, 0.95F)

		val saturationValue = FloatValue("HSB-Saturation", 1F, 0F, 1F)
		val brightnessValue = FloatValue("HSB-Brightness", 1F, 0F, 1F)

		val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)

		val rainbowShaderXValue = FloatValue("RainbowShader-X", -1000F, -2000F, 2000F)
		val rainbowShaderYValue = FloatValue("RainbowShader-Y", -1000F, -2000F, 2000F)

		val headerFontValue = FontValue("HeaderFont", Fonts.font40)
		val messageFontValue = FontValue("MessageFont", Fonts.font35)
	}

	/**
	 * Example notification for CustomHUD designer
	 */
	private val exampleNotification = Notification("Example Notification Header", "Example Notification Message", null)

	/**
	 * Draw element
	 */
	override fun drawElement(): Border?
	{
		if (LiquidBounce.hud.notifications.size > 0)
		{
			val maxRendered = maxRendered.get()

			var index = 0
			val itr = LiquidBounce.hud.notifications.asReversed().iterator()

			var deployingNotification: Notification? = null

			while (itr.hasNext())
			{
				val notification = itr.next()

				if (index + 1 <= maxRendered)
				{
					when (index)
					{
						0 -> deployingNotification = notification
						1 -> GL11.glTranslatef(0.0F, deployingNotification?.yDeploy ?: -35.0F, 0.0F)
						else -> GL11.glTranslatef(0.0F, -35.0F, 0.0F)
					}

					notification.drawNotification()

					if (notification.fadeState == Notification.FadeState.END) itr.remove()

					index++
				}
				else itr.remove()
			}
		}

		if (classProvider.isGuiHudDesigner(mc.currentScreen))
		{
			if (!LiquidBounce.hud.notifications.contains(exampleNotification)) LiquidBounce.hud.addNotification(exampleNotification)

			exampleNotification.fadeState = Notification.FadeState.STAY
			exampleNotification.x = exampleNotification.textLength + 8F

			return Border(-120F, -30F, 0F, 0F)
		}

		return null
	}
}

class Notification(private val header: String, private val message: String, private val rectColor: Color? = null, private val stayTime: Long = 0L)
{
	var x = 0F
	var textLength = 0

	private val stayTimer = MSTimer()
	private var fadeStep = 0F
	var fadeState = FadeState.IN

	private var yDeployProgress = 0F
	private var yDeployStep = 0F
	private val yDeployProgressMax = 100F
	private var yDeploying = true

	internal val yDeploy: Float
		get() = yDeployProgress * -0.35F

	/**
	 * Fade state for animation
	 */
	enum class FadeState
	{
		IN,
		STAY,
		OUT,
		END
	}

	init
	{
		val headerFont = Notifications.headerFontValue.get()
		val messageFont = Notifications.messageFontValue.get()

		textLength = max(headerFont.getStringWidth(header), messageFont.getStringWidth(message))
	}

	/**
	 * Draw notification
	 */
	fun drawNotification()
	{
		val headerFont = Notifications.headerFontValue.get()
		val messageFont = Notifications.messageFontValue.get()

		val bodyColorMode = Notifications.bodyColorModeValue.get()
		val bodyCustomColor = createRGB(Notifications.bodyRedValue.get(), Notifications.bodyGreenValue.get(), Notifications.bodyBlueValue.get(), Notifications.bodyAlphaValue.get())

		val rect = Notifications.rectValue.get()
		val rectWidth = Notifications.rectWidthValue.get()

		val rectColorMode = Notifications.rectColorModeValue.get()
		val rectColorAlpha = Notifications.rectAlphaValue.get()
		val rectCustomColor = createRGB(Notifications.rectRedValue.get(), Notifications.rectGreenValue.get(), Notifications.rectBlueValue.get(), rectColorAlpha)

		val rainbowShaderX = if (Notifications.rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / Notifications.rainbowShaderXValue.get()
		val rainbowShaderY = if (Notifications.rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / Notifications.rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

		val saturation = Notifications.saturationValue.get()
		val brightness = Notifications.brightnessValue.get()
		val rainbowSpeed = Notifications.rainbowSpeedValue.get()

		// Draw Background (Body)
		val bodyRainbowShader = bodyColorMode.equals("RainbowShader", ignoreCase = true)

		RainbowShader.begin(bodyRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
			val color = when
			{
				bodyRainbowShader -> 0
				bodyColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(speed = rainbowSpeed, saturation = saturation, brightness = brightness)
				else -> bodyCustomColor
			}

			RenderUtils.drawRect(-x + 8 + textLength, 0F, -x, -30F, color)
		}

		// Draw remaining time line
		val remainingTimePercentage = if (fadeState != FadeState.IN) stayTimer.hasTimeLeft(stayTime).coerceAtLeast(0).toFloat() / stayTime.toFloat() else if (stayTime == 0L) 0F else 1F
		val color = ColorUtils.blendColors(floatArrayOf(0f, 0.5f, 1f), arrayOf(Color.RED, Color.YELLOW, Color.GREEN), remainingTimePercentage).brighter()
		RenderUtils.drawRect(-x + 8 + textLength, -28F, -x/* - 2*/ + (10 + textLength) * (1 - remainingTimePercentage), -30F, color)


		headerFont.drawString(header, -x.toInt() + 4, -25, Int.MAX_VALUE)
		messageFont.drawString(message, -x.toInt() + 4, -12, Int.MAX_VALUE)

		// Draw Rect
		if (rect)
		{
			val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)

			RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
				val rectColor = rectColor?.rgb ?: when
				{
					rectRainbowShader -> 0
					rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = rectColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
					else -> rectCustomColor
				}

				RenderUtils.drawRect(-x, 0F, -x - rectWidth, -30F, rectColor)
			}
		}

		// Reset Color
		RenderUtils.resetColor()

		// Animation
		val delta = RenderUtils.deltaTime
		val width = textLength + 8F

		val fadeSpeed = Notifications.fadeSpeedValue.get()
		val deploySpeed = Notifications.deploySpeedValue.get()

		if (yDeploying)
		{
			if (yDeployProgress < yDeployProgressMax)
			{
				yDeployProgress = AnimationUtils.easeOut(yDeployStep, yDeployProgressMax) * yDeployProgressMax
				yDeployStep += delta * deploySpeed
			}

			if (yDeployProgress >= yDeployProgressMax)
			{
				yDeployProgress = yDeployProgressMax
				yDeployStep = yDeployProgressMax
				yDeploying = false
			}
		}

		@Suppress("NON_EXHAUSTIVE_WHEN") when (fadeState)
		{
			FadeState.IN ->
			{
				if (x < width)
				{
					x = AnimationUtils.easeOut(fadeStep, width) * width
					fadeStep += delta * fadeSpeed
				}

				if (x >= width)
				{
					stayTimer.reset()
					fadeState = FadeState.STAY

					x = width
					fadeStep = width
				}
			}

			FadeState.STAY -> if (stayTimer.hasTimePassed(stayTime)) fadeState = FadeState.OUT

			FadeState.OUT -> if (x > 0)
			{
				x = AnimationUtils.easeOut(fadeStep, width) * width
				fadeStep -= delta * fadeSpeed
			}
			else fadeState = FadeState.END

			// FadeState.END -> LiquidBounce.hud.removeNotification(this) // It throws ConcurrentModificationException
		}
	}
}

