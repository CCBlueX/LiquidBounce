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
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.PacketCounter
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.extensions.getPing
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.createRGB
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowFontShader
import net.ccbluex.liquidbounce.utils.render.shader.shaders.RainbowShader
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import kotlin.math.hypot

/**
 * CustomHUD text element
 *
 * Allows to draw custom text
 */
@ElementInfo(name = "Text")
class Text(x: Double = 10.0, y: Double = 10.0, scale: Float = 1F, side: Side = Side.default()) : Element(x, y, scale, side)
{

	companion object
	{

		val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
		val HOUR_FORMAT = SimpleDateFormat("HH:mm")

		val DECIMAL_FORMAT = DecimalFormat("0.00")

		/**
		 * Create default element
		 */
		fun defaultClient(): Text
		{
			val text = Text(x = 2.0, y = 2.0, scale = 2F)

			text.displayString.set("%clientName%")
			text.shadowValue.set(true)
			text.fontValue.set(Fonts.font40)
			text.setColor(Color(0, 111, 255))

			return text
		}
	}

	private val displayString = TextValue("DisplayText", "")

	private val colorModeValue = ListValue("ColorMode", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
	private val redValue = IntegerValue("Red", 255, 0, 255)
	private val greenValue = IntegerValue("Green", 255, 0, 255)
	private val blueValue = IntegerValue("Blue", 255, 0, 255)
	private val alphaValue = IntegerValue("Alpha", 255, 0, 255)

	private val rectValue = ListValue("Rect", arrayOf("None", "Left", "Right"), "None")
	private val rectColorModeValue = ListValue("Rect-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Rainbow")
	private val rectColorRedValue = IntegerValue("Rect-R", 255, 0, 255)
	private val rectColorGreenValue = IntegerValue("Rect-G", 255, 0, 255)
	private val rectColorBlueValue = IntegerValue("Rect-B", 255, 0, 255)
	private val rectColorAlphaValue = IntegerValue("Rect-Alpha", 255, 0, 255)

	private val backgroundColorModeValue = ListValue("Background-Color", arrayOf("Custom", "Rainbow", "RainbowShader"), "Custom")
	private val backgroundColorRedValue = IntegerValue("Background-R", 0, 0, 255)
	private val backgroundColorGreenValue = IntegerValue("Background-G", 0, 0, 255)
	private val backgroundColorBlueValue = IntegerValue("Background-B", 0, 0, 255)
	private val backgroundColorAlphaValue = IntegerValue("Background-Alpha", 0, 0, 255)

	private val saturationValue = FloatValue("HSB-Saturation", 0.9f, 0f, 1f)
	private val brightnessValue = FloatValue("HSB-Brightness", 1f, 0f, 1f)

	private val rainbowSpeedValue = IntegerValue("Rainbow-Speed", 10, 1, 10)

	private val rainbowShaderXValue = FloatValue("RainbowShader-X", -1000F, -2000F, 2000F)
	private val rainbowShaderYValue = FloatValue("RainbowShader-Y", -1000F, -2000F, 2000F)

	private val shadowValue = BoolValue("Shadow", true)

	private var fontValue = FontValue("Font", Fonts.font40)

	private var editMode = false
	private var editTicks = 0
	private var prevClick = 0L

	private var displayText = display

	private val display: String
		get()
		{
			val textContent = if (displayString.get().isEmpty() && !editMode) "Text Element"
			else displayString.get()
			return multiReplace(textContent)
		}

	private fun getReplacement(str: String): String?
	{
		val thePlayer = mc.thePlayer

		if (thePlayer != null)
		{
			val defaultTPS = 20.0

			when (str.toLowerCase())
			{
				"x" -> return DECIMAL_FORMAT.format(thePlayer.posX)
				"y" -> return DECIMAL_FORMAT.format(thePlayer.posY)
				"z" -> return DECIMAL_FORMAT.format(thePlayer.posZ)
				"xdp" -> return thePlayer.posX.toString()
				"ydp" -> return thePlayer.posY.toString()
				"zdp" -> return thePlayer.posZ.toString()

				"mx" -> return DECIMAL_FORMAT.format(thePlayer.motionX)
				"my" -> return DECIMAL_FORMAT.format(thePlayer.motionY)
				"mz" -> return DECIMAL_FORMAT.format(thePlayer.motionZ)

				"mxpersec" -> return DECIMAL_FORMAT.format(thePlayer.motionX * defaultTPS)
				"mypersec" -> return DECIMAL_FORMAT.format(thePlayer.motionY * defaultTPS)
				"mzpersec" -> return DECIMAL_FORMAT.format(thePlayer.motionZ * defaultTPS)

				"mxdp" -> return thePlayer.motionX.toString()
				"mydp" -> return thePlayer.motionY.toString()
				"mzdp" -> return thePlayer.motionZ.toString()

				"mxdppersec" -> return (thePlayer.motionX * defaultTPS).toString()
				"mydppersec" -> return (thePlayer.motionY * defaultTPS).toString()
				"mzdppersec" -> return (thePlayer.motionZ * defaultTPS).toString()

				"velocity" -> return DECIMAL_FORMAT.format(hypot(thePlayer.motionX, thePlayer.motionZ))
				"velocitypersec" -> return DECIMAL_FORMAT.format(hypot(thePlayer.motionX, thePlayer.motionZ) * defaultTPS)

				"velocitydp" -> return "${hypot(thePlayer.motionX, thePlayer.motionZ)}"
				"velocitydppersec" -> return "${hypot(thePlayer.motionX, thePlayer.motionZ) * defaultTPS}"

				"ping" -> return thePlayer.getPing().toString()
				"health" -> return DECIMAL_FORMAT.format(thePlayer.health)
				"maxhealth" -> return DECIMAL_FORMAT.format(thePlayer.maxHealth)
				"food" -> return thePlayer.foodStats.foodLevel.toString()

				"facing" -> return StringUtils.getHorizontalFacing(thePlayer.rotationYaw)
				"facingadv" -> return StringUtils.getHorizontalFacingAdv(thePlayer.rotationYaw)
				"facingvector" -> return StringUtils.getHorizontalFacingTowards(thePlayer.rotationYaw)
			}
		}

		return when (str.toLowerCase())
		{
			"username" -> mc.session.username
			"clientname" -> LiquidBounce.CLIENT_NAME
			"clientversion" -> "b${LiquidBounce.CLIENT_VERSION}"
			"clientcreator" -> LiquidBounce.CLIENT_CREATOR
			"fps" -> mc.debugFPS.toString()
			"date" -> DATE_FORMAT.format(System.currentTimeMillis())
			"time" -> HOUR_FORMAT.format(System.currentTimeMillis())
			"serverip" -> ServerUtils.remoteIp
			"cps", "lcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.LEFT).toString()
			"mcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.MIDDLE).toString()
			"rcps" -> return CPSCounter.getCPS(CPSCounter.MouseButton.RIGHT).toString()
			"timer" -> return mc.timer.timerSpeed.toString()
			"lastpacket" -> return mc.netHandler.networkManager.lastPacket.toString()
			"packetin", "ppsin" -> return PacketCounter.getPacketCount(PacketCounter.PacketType.INBOUND, 1000L).toString()
			"packetout", "ppsout" -> return PacketCounter.getPacketCount(PacketCounter.PacketType.OUTBOUND, 1000L).toString()
			else -> null // Null = don't replace
		}
	}

	private fun multiReplace(str: String): String
	{
		var lastPercent = -1
		val result = StringBuilder()
		for (i in str.indices)
		{
			if (str[i] == '%')
			{
				if (lastPercent != -1)
				{
					if (lastPercent + 1 != i)
					{
						val replacement = getReplacement(str.substring(lastPercent + 1, i))

						if (replacement != null)
						{
							result.append(replacement)
							lastPercent = -1
							continue
						}
					}
					result.append(str, lastPercent, i)
				}
				lastPercent = i
			}
			else if (lastPercent == -1) result.append(str[i])
		}

		if (lastPercent != -1)
		{
			result.append(str, lastPercent, str.length)
		}

		return "$result"
	}

	/**
	 * Draw element
	 */
	override fun drawElement(): Border
	{
		val colorMode = colorModeValue.get()
		val rectMode = rectValue.get()
		val rectColorMode = rectColorModeValue.get()
		val backgroundColorMode = backgroundColorModeValue.get()
		val colorAlpha = alphaValue.get()
		val customColor = createRGB(redValue.get(), greenValue.get(), blueValue.get(), colorAlpha)
		val rectColorAlpha = rectColorAlphaValue.get()
		val rectCustomColor = createRGB(rectColorRedValue.get(), rectColorGreenValue.get(), rectColorBlueValue.get(), rectColorAlpha)
		val backgroundColorAlpha = backgroundColorAlphaValue.get()
		val backgroundCustomColor = createRGB(backgroundColorRedValue.get(), backgroundColorGreenValue.get(), backgroundColorBlueValue.get(), backgroundColorAlpha)
		val fontRenderer = fontValue.get()
		val rainbowSpeed = rainbowSpeedValue.get()
		val rainbowShaderX = if (rainbowShaderXValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderXValue.get()
		val rainbowShaderY = if (rainbowShaderYValue.get() == 0.0F) 0.0F else 1.0F / rainbowShaderYValue.get()
		val rainbowShaderOffset = System.currentTimeMillis() % 10000 * 0.0001f
		val saturation = saturationValue.get()
		val brightness = brightnessValue.get()
		val shadow = shadowValue.get()

		val horizontalSide = side.horizontal

		val textWidth = fontRenderer.getStringWidth(displayText)

		val borderExpand = 2F
		val rectWidth = 3F

		val (borderXStart, borderXEnd) = when (horizontalSide)
		{
			Side.Horizontal.LEFT -> -borderExpand to textWidth + borderExpand
			Side.Horizontal.MIDDLE -> -borderExpand - textWidth * 0.5F to borderExpand + textWidth * 0.5F
			Side.Horizontal.RIGHT -> -borderExpand - textWidth to borderExpand
		}

		val borderYStart = -borderExpand
		val borderYEnd = fontRenderer.fontHeight.toFloat() + borderExpand

		val leftRect = rectMode.equals("Left", ignoreCase = true)
		val rightRect = rectMode.equals("Right", ignoreCase = true)

		val backgroundXStart = borderXStart + if (!leftRect) 0f else rectWidth
		val backgroundXEnd = borderXEnd + if (rightRect) 0f else rectWidth

		val textX = (if (rightRect) 0f else if (leftRect) rectWidth else rectWidth * 0.5F) + (borderXStart + borderExpand)

		val backgroundRainbowShader = backgroundColorMode.equals("RainbowShader", ignoreCase = true)
		val rectRainbowShader = rectColorMode.equals("RainbowShader", ignoreCase = true)
		val textRainbowShader = colorMode.equals("RainbowShader", ignoreCase = true)

		val backgroundColor = when
		{
			backgroundRainbowShader -> 0
			backgroundColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = backgroundColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
			else -> backgroundCustomColor
		}

		val rectColor = when
		{
			rectRainbowShader -> 0
			rectColorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = rectColorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
			else -> rectCustomColor
		}

		val textColor = when
		{
			textRainbowShader -> 0
			colorMode.equals("Rainbow", ignoreCase = true) -> ColorUtils.rainbowRGB(alpha = colorAlpha, speed = rainbowSpeed, saturation = saturation, brightness = brightness)
			else -> customColor
		}

		// Render Background
		RainbowShader.begin(backgroundRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
			RenderUtils.drawRect(backgroundXStart, borderYStart, backgroundXEnd, borderYEnd, backgroundColor)
		}

		// Render Rect
		if (leftRect || rightRect) RainbowShader.begin(rectRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
			if (leftRect) RenderUtils.drawRect(backgroundXStart - rectWidth, borderYStart, backgroundXStart, borderYEnd, rectColor) else RenderUtils.drawRect(borderXEnd, borderYStart, borderXEnd + rectWidth, borderYEnd, rectColor)
		}

		// Render Text
		RainbowFontShader.begin(textRainbowShader, rainbowShaderX, rainbowShaderY, rainbowShaderOffset).use {
			fontRenderer.drawString(displayText, textX, 0F, textColor, shadow)

			if (editMode && classProvider.isGuiHudDesigner(mc.currentScreen) && editTicks <= 40) fontRenderer.drawString("_", if (rectMode.equals("right", true)) 0f else if (rectMode.equals("left", true)) 3f else 1.5f + textWidth + 2F, 0F, textColor, shadow)
		}

		// Disable edit mode when current gui is not HUD Designer
		if (editMode && !classProvider.isGuiHudDesigner(mc.currentScreen))
		{
			editMode = false
			updateElement()
		}

		return Border(backgroundXStart - rectWidth, -borderExpand, backgroundXEnd + rectWidth, fontRenderer.fontHeight.toFloat() + borderExpand)
	}

	override fun updateElement()
	{
		editTicks += 5
		if (editTicks > 80) editTicks = 0

		displayText = if (editMode) displayString.get() else display
	}

	override fun handleMouseClick(x: Double, y: Double, mouseButton: Int)
	{
		if (isInBorder(x, y) && mouseButton == 0)
		{
			if (System.currentTimeMillis() - prevClick <= 250L) editMode = true

			prevClick = System.currentTimeMillis()
		}
		else editMode = false
	}

	override fun handleKey(c: Char, keyCode: Int)
	{
		if (editMode && classProvider.isGuiHudDesigner(mc.currentScreen))
		{
			if (keyCode == Keyboard.KEY_BACK)
			{
				if (displayString.get().isNotEmpty()) displayString.set(displayString.get().substring(0, displayString.get().length - 1))

				updateElement()
				return
			}

			if (ColorUtils.isAllowedCharacter(c) || c == '\u00A7') displayString.set(displayString.get() + c)

			updateElement()
		}
	}

	fun setColor(c: Color): Text
	{
		redValue.set(c.red)
		greenValue.set(c.green)
		blueValue.set(c.blue)
		return this
	}
}
