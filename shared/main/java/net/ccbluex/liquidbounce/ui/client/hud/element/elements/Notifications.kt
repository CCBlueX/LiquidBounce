/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.hud.element.*
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.AnimationUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max

/**
 * CustomHUD Notification element
 * TODO: Vertical animation
 */
@ElementInfo(name = "Notifications", single = true)
class Notifications(
	x: Double = 0.0, y: Double = 30.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)
) : Element(x, y, scale, side)
{
	companion object
	{
		val bodyColorModeValue = ListValue("Body-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
		val bodyRedValue = IntegerValue("Body-R", 0, 0, 255)
		val bodyGreenValue = IntegerValue("Body-G", 0, 0, 255)
		val bodyBlueValue = IntegerValue("Body-B", 0, 0, 255)
		val bodyAlphaValue = IntegerValue("Body-Alpha", 255, 0, 255)

		val rectValue = BoolValue("Rect", true)
		val rectColorModeValue = ListValue("Rect-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
		val rectRedValue = IntegerValue("Rect-R", 0, 0, 255)
		val rectGreenValue = IntegerValue("Rect-G", 111, 0, 255)
		val rectBlueValue = IntegerValue("Rect-B", 255, 0, 255)
		val rectAlphaValue = IntegerValue("Rect-Alpha", 255, 0, 255)

		val maxRendered = IntegerValue("MaxRendered", 6, 3, 15)

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
			var index = 0
			val itr = LiquidBounce.hud.notifications.iterator()
			while (itr.hasNext() && index + 1 <= maxRendered.get())
			{
				val notification = itr.next()

				if (index != 0) GL11.glTranslated(0.0, -35.0, 0.0)

				notification.drawNotification()

				if (notification.fadeState == Notification.FadeState.END) itr.remove()

				index++
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

	/**
	 * Fade state for animation
	 */
	enum class FadeState
	{ IN, STAY, OUT, END }

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
		val bodyCustomColor = Color(Notifications.bodyRedValue.get(), Notifications.bodyGreenValue.get(), Notifications.bodyBlueValue.get(), Notifications.bodyAlphaValue.get()).rgb

		val rect = Notifications.rectValue.get()
		val rectColorMode = Notifications.rectColorModeValue.get()
		val rectColorAlpha = Notifications.rectAlphaValue.get()
		val rectCustomColor = Color(Notifications.rectRedValue.get(), Notifications.rectGreenValue.get(), Notifications.rectBlueValue.get(), rectColorAlpha).rgb

		val rainbowShaderX = if (Notifications.rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / Notifications.rainbowShaderXValue.get()
		val rainbowShaderY = if (Notifications.rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / Notifications.rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 / 10000F

		val saturation = Notifications.saturationValue.get()
		val brightness = Notifications.brightnessValue.get()
		val rainbowSpeed = Notifications.rainbowSpeedValue.get()

		// Draw Background (Body)
		val bodyRainbowShader = bodyColorMode.equals("RainbowShader", ignoreCase = true)

		RainbowShader.begin(bodyRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
			val color = when
			{
				bodyRainbowShader -> 0
				bodyColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
				else -> bodyCustomColor
			}

			RenderUtils.drawRect(-x + 8 + textLength, 0F, -x, -30F, color)
		}

		// Draw remaining time line
		val remainingTimePercentage = stayTimer.hasTimeLeft(stayTime).coerceAtLeast(0).toFloat() / stayTime.toFloat()
		val color = (ColorUtils.blendColors(floatArrayOf(0f, 0.5f, 1f), arrayOf(Color.RED, Color.YELLOW, Color.GREEN), remainingTimePercentage)).brighter()
		RenderUtils.drawRect(-x + 8 + textLength, -28F, -x - 2 + (10 + textLength) * (1 - remainingTimePercentage), -30F, color)


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
					rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbow(alpha = rectColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness).rgb
					else -> rectCustomColor
				}

				RenderUtils.drawRect(-x, 0F, -x - 5, -30F, rectColor)
			}
		}

		// Reset Color
		GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f)

		// Animation
		val delta = RenderUtils.deltaTime
		val width = textLength + 8F

		@Suppress("NON_EXHAUSTIVE_WHEN") when (fadeState)
		{
			FadeState.IN ->
			{
				if (x < width)
				{
					x = AnimationUtils.easeOut(fadeStep, width) * width
					fadeStep += delta / 4F
				}
				if (x >= width)
				{
					fadeState = FadeState.STAY
					x = width
					fadeStep = width
				}

				stayTimer.reset()
			}

			FadeState.STAY -> if (stayTimer.hasTimePassed(stayTime)) fadeState = FadeState.OUT

			FadeState.OUT -> if (x > 0)
			{
				x = AnimationUtils.easeOut(fadeStep, width) * width
				fadeStep -= delta / 4F
			}
			else fadeState = FadeState.END

			// FadeState.END -> LiquidBounce.hud.removeNotification(this) // Raises ConcurrentModificationException
		}
	}
}

