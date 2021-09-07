/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.settings.IKeyBinding
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11
import java.awt.Color

/**
 * CustomHUD Armor element
 *
 * Shows a horizontal display of current armor
 */
@ElementInfo(name = "Keystrokes")
class Keystrokes(x: Double = -8.0, y: Double = 57.0, scale: Float = 1F, side: Side = Side(Side.Horizontal.MIDDLE, Side.Vertical.DOWN)) : Element(x, y, scale, side)
{
	private val keyboardGroup = ValueGroup("Keyboard")
	private val keyboardEnabledValue = BoolValue("Enabled", true, "Keyboard")
	private val keyboardYPosValue = IntegerValue("YPos", 0, 0, 10, "Keyboard-YPos")

	private val keyboardPressedColor = ValueGroup("Pressed")
	private val keyboardPressedBackgroundValue = RGBAColorValue("Background", 0, 0, 0, 192, listOf("Keyboard-Pressed-Red", "Keyboard-Pressed-Green", "Keyboard-Pressed-Blue", "Keyboard-Pressed-Alpha"))
	private val keyboardPressedTextValue = RGBAColorValue("Text", 255, 255, 255, 192, listOf("Keyboard-Text-Pressed-Red", "Keyboard-Text-Pressed-Green", "Keyboard-Text-Pressed-Blue", "Keyboard-Text-Pressed-Alpha"))

	private val keyboardUnpressedColor = ValueGroup("Unpressed")
	private val keyboardUnpressedBackgroundValue = RGBAColorValue("Background", 0, 0, 0, 192, listOf("Keyboard-Unpressed-Red", "Keyboard-Unpressed-Green", "Keyboard-Unpressed-Blue", "Keyboard-Unpressed-Alpha"))
	private val keyboardUnpressedTextValue = RGBAColorValue("Text", 255, 255, 255, 192, listOf("Keyboard-Text-Unpressed-Red", "Keyboard-Text-Unpressed-Green", "Keyboard-Text-Unpressed-Blue", "Keyboard-Text-Unpressed-Alpha"))

	private val mouseGroup = ValueGroup("Mouse")
	private val mouseEnabledValue = BoolValue("Enabled", true, "Mouse")
	private val mouseYPosValue = IntegerValue("YPos", 0, -20, 20, "Mouse-YPos")

	private val mousePressedColor = ValueGroup("Pressed")
	private val mousePressedBackgroundValue = RGBAColorValue("Background", 0, 0, 0, 192, listOf("Mouse-Pressed-Red", "Mouse-Pressed-Green", "Mouse-Pressed-Blue", "Mouse-Pressed-Alpha"))
	private val mousePressedTextValue = RGBAColorValue("Text", 255, 255, 255, 192, listOf("Mouse-Text-Pressed-Red", "Mouse-Text-Pressed-Green", "Mouse-Text-Pressed-Blue", "Mouse-Text-Pressed-Alpha"))

	private val mouseUnpressedColor = ValueGroup("Unpressed")
	private val mouseUnpressedBackgroundValue = RGBAColorValue("Background", 0, 0, 0, 192, listOf("Mouse-Unpressed-Red", "Mouse-Unpressed-Green", "Mouse-Unpressed-Blue", "Mouse-Unpressed-Alpha"))
	private val mouseUnpressedTextValue = RGBAColorValue("Text", 255, 255, 255, 192, listOf("Mouse-Text-Unpressed-Red", "Mouse-Text-Unpressed-Green", "Mouse-Text-Unpressed-Blue", "Mouse-Text-Unpressed-Alpha"))

	private val spaceGroup = ValueGroup("Space")
	private val spaceEnabledValue = BoolValue("Enabled", true, "Space")
	private val spaceYPosValue = IntegerValue("YPos", 0, -20, 20, "Space-YPos")

	private val spacePressedColor = ValueGroup("Pressed")
	private val spacePressedBackgroundValue = RGBAColorValue("Background", 0, 0, 0, 192, listOf("Space-Pressed-Red", "Space-Pressed-Green", "Space-Pressed-Blue", "Space-Pressed-Alpha"))
	private val spacePressedTextValue = RGBAColorValue("Text", 255, 255, 255, 192, listOf("Space-Text-Pressed-Red", "Space-Text-Pressed-Green", "Space-Text-Pressed-Blue", "Space-Text-Pressed-Alpha"))

	private val spaceUnpressedColor = ValueGroup("Unpressed")
	private val spaceUnpressedBackgroundValue = RGBAColorValue("Background", 0, 0, 0, 192, listOf("Space-Unpressed-Red", "Space-Unpressed-Green", "Space-Unpressed-Blue", "Space-Unpressed-Alpha"))
	private val spaceUnpressedTextValue = RGBAColorValue("Text", 255, 255, 255, 192, listOf("Space-Text-Unpressed-Red", "Space-Text-Unpressed-Green", "Space-Text-Unpressed-Blue", "Space-Text-Unpressed-Alpha"))

	private val fadingTime = IntegerValue("FadingTime", 100, 0, 1000)

	private var fontValue = FontValue("Font", Fonts.font35)

	private val keyboardKeys = run {
		val gameSettings = mc.gameSettings
		val pressed = { keyboardPressedBackgroundValue.getColor() to keyboardPressedTextValue.getColor() }
		val unpressed = { keyboardUnpressedBackgroundValue.getColor() to keyboardUnpressedTextValue.getColor() }
		val fading = fadingTime::get
		val yPos = { keyboardYPosValue.get() + 20 }

		arrayOf(Key(gameSettings.keyBindForward, 20, keyboardYPosValue::get, 19, 19, pressed, unpressed, fading), // Forward
			Key(gameSettings.keyBindLeft, 0, yPos, 19, 19, pressed, unpressed, fading), // Left
			Key(gameSettings.keyBindBack, 20, yPos, 19, 19, pressed, unpressed, fading), // Back
			Key(gameSettings.keyBindRight, 40, yPos, 19, 19, pressed, unpressed, fading) // Right
		)
	}

	private val mouseKeys = run {
		val gameSettings = mc.gameSettings
		val pressed = { mousePressedBackgroundValue.getColor() to mousePressedTextValue.getColor() }
		val unpressed = { mouseUnpressedBackgroundValue.getColor() to mouseUnpressedTextValue.getColor() }
		val fading = fadingTime::get
		val yPos = { mouseYPosValue.get() + 40 }

		arrayOf(Key(gameSettings.keyBindAttack, 0, yPos, 29, 19, pressed, unpressed, fading), // Attack
			Key(gameSettings.keyBindUseItem, 30, yPos, 29, 19, pressed, unpressed, fading) // Use
		)
	}

	private val spaceBar = KeySpace(mc.gameSettings.keyBindJump, 0, { spaceYPosValue.get() + 60 }, 59, 11, { spacePressedBackgroundValue.getColor() to spacePressedTextValue.getColor() }, { spaceUnpressedBackgroundValue.getColor() to spaceUnpressedTextValue.getColor() }, fadingTime::get)

	init
	{
		keyboardPressedColor.addAll(keyboardPressedBackgroundValue, keyboardPressedTextValue)
		keyboardUnpressedColor.addAll(keyboardUnpressedBackgroundValue, keyboardUnpressedTextValue)
		keyboardGroup.addAll(keyboardEnabledValue, keyboardYPosValue, keyboardPressedColor, keyboardUnpressedColor)

		mousePressedColor.addAll(mousePressedBackgroundValue, mousePressedTextValue)
		mouseUnpressedColor.addAll(mouseUnpressedBackgroundValue, mouseUnpressedTextValue)
		mouseGroup.addAll(mouseEnabledValue, mouseYPosValue, mousePressedColor, mouseUnpressedColor)

		spacePressedColor.addAll(spacePressedBackgroundValue, spacePressedTextValue)
		spaceUnpressedColor.addAll(spaceUnpressedBackgroundValue, spaceUnpressedTextValue)
		spaceGroup.addAll(spaceEnabledValue, spaceYPosValue, spacePressedColor, spaceUnpressedColor)
	}

	/**
	 * Draw element
	 */
	override fun drawElement(): Border
	{
		var renderQueue = arrayOfNulls<Key>(7)
		if (keyboardEnabledValue.get()) renderQueue += keyboardKeys
		if (mouseEnabledValue.get()) renderQueue += mouseKeys
		if (spaceEnabledValue.get()) renderQueue += spaceBar

		for (key in renderQueue.filterNotNull())
		{
			key.updateKeyState()
			key.drawKey(fontValue.get())
		}

		return Border(0F, 0F, 59F, 72F)
	}
}

private open class Key(val keybinding: IKeyBinding, val xPos: Int, val yPos: () -> Int, val width: Int, val height: Int, val pressedColor: () -> Pair<Color, Color>, val unpressedColor: () -> Pair<Color, Color>, val fadingTime: () -> Int)
{
	var isPressed = false
	var percentFade = 0.0
	var lastPress = System.currentTimeMillis()

	val color: Int
		get()
		{
			val color = (if (isPressed) pressedColor() else unpressedColor()).first
			if (percentFade < 1F) return ColorUtils.blend(color, (if (isPressed) unpressedColor() else pressedColor()).first, percentFade).rgb
			return color.rgb
		}

	val textColor: Int
		get()
		{
			val color = (if (isPressed) pressedColor() else unpressedColor()).second
			if (percentFade < 1F) return ColorUtils.blend(color, (if (isPressed) unpressedColor() else pressedColor()).second, percentFade).rgb
			return color.rgb
		}

	val keyName: String
		get()
		{
			val code: Int = keybinding.keyCode
			when (code)
			{
				-100 -> return "LMB"
				-99 -> return "RMB"
				-98 -> return "MMB"

				Keyboard.KEY_UP -> return "UP"
				Keyboard.KEY_LEFT -> return "LEFT"
				Keyboard.KEY_RIGHT -> return "RIGHT"
				Keyboard.KEY_DOWN -> return "DOWN"
				Keyboard.KEY_INSERT -> return "INS"
				Keyboard.KEY_LCONTROL -> return "LCTRL"
				Keyboard.KEY_RCONTROL -> return "RCTRL"
				Keyboard.KEY_LMENU -> return "LALT"
				Keyboard.KEY_RMENU -> return "RALT"
			}

			return if (code in 0..223) Keyboard.getKeyName(code)
			else if (code >= -100 && code <= -84) Mouse.getButtonName(code + 100)
			else "$code"
		}

	open fun drawKey(font: IFontRenderer)
	{
		var drawPosX = xPos
		var drawPosY = yPos()

		RenderUtils.drawRect(drawPosX, drawPosY, drawPosX + width, drawPosY + height, color)

		val keyNameString = keyName

		drawPosX += (width - font.getStringWidth(keyNameString)) / 2 + 1
		drawPosY += (height - font.fontHeight) / 2 + 1

		GL11.glEnable(GL11.GL_BLEND)
		font.drawString(keyNameString, drawPosX, drawPosY, textColor)
		GL11.glDisable(GL11.GL_BLEND)
	}

	fun updateKeyState()
	{
		if (isPressed != isKeyDown(keybinding.keyCode))
		{
			isPressed = !isPressed
			lastPress = System.currentTimeMillis()
		}

		percentFade = (System.currentTimeMillis() - lastPress).toDouble() / fadingTime()
	}

	private fun isKeyDown(keyCode: Int) = if (keyCode < 0) Mouse.isButtonDown(keyCode + 100) else Keyboard.isKeyDown(keyCode)
}

private class KeySpace(keybinding: IKeyBinding, xPos: Int, yPos: () -> Int, width: Int, height: Int, pressedColor: () -> Pair<Color, Color>, unpressedColor: () -> Pair<Color, Color>, fadingTime: () -> Int) : Key(keybinding, xPos, yPos, width, height, pressedColor, unpressedColor, fadingTime)
{
	override fun drawKey(font: IFontRenderer)
	{
		RenderUtils.drawRect(xPos, yPos(), xPos + width, yPos() + height, color)

		RenderUtils.drawHorizontalLine(xPos + width / 2 - 6, xPos + width / 2 + 6, yPos() + height / 2 - 1, textColor)
	}
}

