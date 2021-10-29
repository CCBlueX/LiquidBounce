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
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.easeOut
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.max

/**
 * CustomHUD Notification element
 */
@ElementInfo(name = "Notifications", single = true)
class Notifications(x: Double = 0.0, y: Double = 30.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.RIGHT, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
	companion object
	{
		private val bodyColorGroup = ValueGroup("BodyColor")
		val bodyColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom", "Body-Color")
		val bodyColorValue = RGBAColorValue("Color", 0, 0, 0, 255, listOf("Body-R", "Body-G", "Body-B", "Body-Alpha"))

		private val rectGroup = ValueGroup("Rect")
		val rectModeValue = ListValue("Mode", arrayOf("None", "Left", "Right"), "Left")
		val rectWidthValue = FloatValue("Width", 5F, 1.5F, 8F, "Rect-Width")

		private val rectInfoColorGroup = ValueGroup("InfoColor")
		val rectInfoColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom", "Rect-Color")
		val rectInfoColorValue = RGBAColorValue("Color", 0, 111, 255, 255, listOf("Rect-R", "Rect-G", "Rect-B", "Rect-Alpha"))

		private val rectWarnColorGroup = ValueGroup("WarnColor")
		val rectWarnColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
		val rectWarnColorValue = RGBAColorValue("Color", 255, 255, 0, 255)

		private val rectVerboseColorGroup = ValueGroup("VerboseColor")
		val rectVerboseColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
		val rectVerboseColorValue = RGBAColorValue("Color", 128, 128, 128, 255)

		private val rectErrorColorGroup = ValueGroup("ErrorColor")
		val rectErrorColorModeValue = ListValue("Mode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
		val rectErrorColorValue = RGBAColorValue("Color", 255, 0, 0, 255)

		private val remainingTimeBarGroup = ValueGroup("RemainingTime")
		val remainingTimeBarModeValue = ListValue("Mode", arrayOf("None", "Up", "Down"), "Down")
		val remainingTimeBarWidthValue = FloatValue("Width", 1F, 0.2F, 2F)

		private val rainbowGroup = object : ValueGroup("Rainbow")
		{
			override fun showCondition() = bodyColorModeValue.get().equals("Rainbow", ignoreCase = true) || rectInfoColorModeValue.get().equals("Rainbow", ignoreCase = true)
		}
		val rainbowSpeedValue = IntegerValue("Speed", 10, 1, 10, "Rainbow-Speed")
		val rainbowSaturationValue = FloatValue("Saturation", 1F, 0F, 1F, "HSB-Saturation")
		val rainbowBrightnessValue = FloatValue("Brightness", 1F, 0F, 1F, "HSB-Brightness")

		private val rainbowShaderGroup = object : ValueGroup("RainbowShader")
		{
			override fun showCondition() = bodyColorModeValue.get().equals("RainbowShader", ignoreCase = true) || rectInfoColorModeValue.get().equals("RainbowShader", ignoreCase = true)
		}
		val rainbowShaderXValue = FloatValue("X", -1000F, -2000F, 2000F, "RainbowShader-X")
		val rainbowShaderYValue = FloatValue("Y", -1000F, -2000F, 2000F, "RainbowShader-Y")

		val maxRendered = IntegerValue("MaxRendered", 6, 3, 15)

		val fadeSpeedValue = FloatValue("FadeSpeed", 0.25F, 0.1F, 0.95F)
		val deploySpeedValue = FloatValue("DeploySpeed", 0.65F, 0.40F, 0.95F)

		val headerFontValue = FontValue("HeaderFont", Fonts.font40)
		val messageFontValue = FontValue("MessageFont", Fonts.font35)

		init
		{
			bodyColorGroup.addAll(bodyColorModeValue, bodyColorValue)

			rectInfoColorGroup.addAll(rectInfoColorModeValue, rectInfoColorValue)
			rectWarnColorGroup.addAll(rectWarnColorModeValue, rectWarnColorValue)
			rectVerboseColorGroup.addAll(rectVerboseColorModeValue, rectVerboseColorValue)
			rectErrorColorGroup.addAll(rectErrorColorModeValue, rectErrorColorValue)
			rectGroup.addAll(rectModeValue, rectWidthValue, rectInfoColorGroup, rectWarnColorGroup, rectVerboseColorGroup, rectErrorColorGroup)

			remainingTimeBarGroup.addAll(remainingTimeBarModeValue, remainingTimeBarWidthValue)

			rainbowGroup.addAll(rainbowSpeedValue, rainbowSaturationValue, rainbowBrightnessValue)

			rainbowShaderGroup.addAll(rainbowShaderXValue, rainbowShaderYValue)
		}
	}

	/**
	 * Example notification for CustomHUD designer
	 */
	private val exampleNotification = Notification(NotificationIcon.INFORMATION, "Example Notification Header", "Example Notification Message")

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
			val cornerX = (LiquidBounce.wrapper.classProvider.createScaledResolution(LiquidBounce.wrapper.minecraft).scaledWidth - renderX).toFloat()
			val vertical = if (side.vertical == Side.Vertical.UP) 1f else -1f

			while (itr.hasNext())
			{
				val notification = itr.next()

				if (index + 1 <= maxRendered)
				{
					when (index)
					{
						0 -> deployingNotification = notification
						1 -> GL11.glTranslatef(0.0F, (deployingNotification?.yDeploy ?: 35.0F) * vertical, 0.0F)
						else -> GL11.glTranslatef(0.0F, 35.0F * vertical, 0.0F)
					}

					notification.drawNotification(side, cornerX)

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

			return when (side.horizontal)
			{
				Side.Horizontal.RIGHT -> Border(0F, -30F, 150F, 0F)
				Side.Horizontal.MIDDLE -> Border(-75F, -30F, 75F, 0F)
				Side.Horizontal.LEFT -> Border(-150F, -30F, 0F, 0F)
			}
		}

		return null
	}
}

enum class NotificationIcon(iconPath: String, val colorMode: () -> String, val customColor: () -> Int)
{
	INFORMATION("/notification/information.png", Notifications.rectInfoColorModeValue::get, Notifications.rectInfoColorValue::get),
	WARNING_YELLOW("/notification/warning_yellow.png", Notifications.rectWarnColorModeValue::get, Notifications.rectWarnColorValue::get),
	WARNING_RED("/notification/warning_red.png", Notifications.rectWarnColorModeValue::get, Notifications.rectWarnColorValue::get),
	VERBOSE("/notification/verbose.png", Notifications.rectVerboseColorModeValue::get, Notifications.rectVerboseColorValue::get),
	VANISH("/notification/vanish.png", Notifications.rectWarnColorModeValue::get, Notifications.rectWarnColorValue::get),
	MURDER_MYSTERY("/notification/murder_mystery.png", Notifications.rectErrorColorModeValue::get, Notifications.rectErrorColorValue::get),
	ROBOT("/notification/robot.png", Notifications.rectErrorColorModeValue::get, Notifications.rectErrorColorValue::get),
	ERROR("/notification/error.png", Notifications.rectErrorColorModeValue::get, Notifications.rectErrorColorValue::get);

	val resourceLocation = LiquidBounce.wrapper.classProvider.createResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + iconPath)
}

class Notification(private val type: NotificationIcon, private val header: String, private val message: String, private val stayTime: Long = 0L)
{
	var x = Float.MAX_VALUE // 0F
	var textLength = 0

	private val stayTimer = MSTimer()
	private var fadeStep = 0F
	var fadeState = FadeState.IN

	private var yDeployProgress = 0F
	private var yDeployStep = 0F
	private val yDeployProgressMax = 100F
	private var yDeploying = true

	internal val yDeploy: Float
		get() = yDeployProgress * 0.35F

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
	fun drawNotification(side: Side, cornerX: Float)
	{
		val headerFont = Notifications.headerFontValue.get()
		val messageFont = Notifications.messageFontValue.get()

		val bodyColorMode = Notifications.bodyColorModeValue.get()
		val bodyCustomColor = Notifications.bodyColorValue.get()

		val rect = Notifications.rectModeValue.get()
		val rectWidth = Notifications.rectWidthValue.get()

		val rainbowShaderX = if (Notifications.rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / Notifications.rainbowShaderXValue.get()
		val rainbowShaderY = if (Notifications.rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / Notifications.rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f

		val saturation = Notifications.rainbowSaturationValue.get()
		val brightness = Notifications.rainbowBrightnessValue.get()
		val rainbowSpeed = Notifications.rainbowSpeedValue.get()

		// Draw Background (Body)
		val bodyRainbowShader = bodyColorMode.equals("RainbowShader", ignoreCase = true)

		val width = textLength + 38f
		val (bodyXStart, bodyXEnd) = when (side.horizontal)
		{
			Side.Horizontal.LEFT -> x - width to x
			Side.Horizontal.MIDDLE -> -x + width - width * 0.5f to -x + width + width * 0.5f
			Side.Horizontal.RIGHT -> -x to -x + width
		}
		RainbowShader.begin(bodyRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
			val color = when
			{
				bodyRainbowShader -> 0
				bodyColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(speed = rainbowSpeed, saturation = saturation, brightness = brightness)
				else -> bodyCustomColor
			}

			RenderUtils.drawRect(bodyXStart, 0F, bodyXEnd, -30F, color)
		}

		// Draw remaining time line
		val remainingTimePercentage = if (fadeState != FadeState.IN) stayTimer.hasTimeLeft(stayTime).coerceAtLeast(0).toFloat() / stayTime.toFloat() else if (stayTime == 0L) 0F else 1F
		val color = ColorUtils.blendColors(floatArrayOf(0f, 0.5f, 1f), arrayOf(Color.RED, Color.YELLOW, Color.GREEN), remainingTimePercentage).brighter()

		run {
			val pair: Pair<Float, Float> = when (Notifications.remainingTimeBarModeValue.get().toLowerCase())
			{
				"up" -> -30f to -30f + Notifications.remainingTimeBarWidthValue.get()
				"down" -> 0f to -Notifications.remainingTimeBarWidthValue.get()
				else -> null
			} ?: return@run
			RenderUtils.drawRect(bodyXStart + width * (1f - remainingTimePercentage), pair.first, bodyXEnd, pair.second, color)
		}

		RenderUtils.drawImage(type.resourceLocation, bodyXStart + 5f, -25f, 20f, 20f)

		headerFont.drawString(header, bodyXStart.toInt() + 30, -25, Int.MAX_VALUE)
		messageFont.drawString(message, bodyXStart.toInt() + 30, -12, Int.MAX_VALUE)

		// Draw Rect
		if (!rect.equals("None", ignoreCase = true))
		{
			val rectColorMode = type.colorMode()
			val rectCustomColor = type.customColor()
			val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)

			RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
				val rectColor = when
				{
					rectRainbowShader -> 0
					rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = rectCustomColor shr 24 and 0xFF, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
					else -> rectCustomColor
				}

				when (rect.toLowerCase())
				{
					"left" -> RenderUtils.drawRect(bodyXStart - rectWidth, 0F, bodyXStart, -30F, rectColor)
					"right" -> RenderUtils.drawRect(bodyXEnd, 0F, bodyXEnd + rectWidth, -30F, rectColor)
				}
			}
		}

		// Reset Color
		RenderUtils.resetColor()

		// Animation
		val delta = RenderUtils.frameTime

		val fadeSpeed = Notifications.fadeSpeedValue.get()
		val deploySpeed = Notifications.deploySpeedValue.get()

		if (yDeploying)
		{
			if (yDeployProgress < yDeployProgressMax)
			{
				yDeployProgress = easeOut(yDeployStep, yDeployProgressMax) * yDeployProgressMax
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
				if (x == Float.MAX_VALUE) x = -cornerX

				if (x < width)
				{
					x = easeOut(fadeStep, width) * (width + cornerX) - cornerX
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

			FadeState.OUT ->
			{
				if (x > -cornerX)
				{
					x = easeOut(fadeStep, width) * width
					fadeStep -= delta * fadeSpeed
				}
				else fadeState = FadeState.END
			}

			// FadeState.END -> LiquidBounce.hud.removeNotification(this) - throws ConcurrentModificationException
		}
	}
}

