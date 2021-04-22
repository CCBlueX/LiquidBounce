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
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
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
	private val keyboardValue = BoolValue("Keyboard", true)

	private val keyboardYPosValue = IntegerValue("Keyboard-YPos", 0, 0, 10)

	private val keyboardPressedRedValue = IntegerValue("Keyboard-Pressed-Red", 255, 0, 255)
	private val keyboardPressedGreenValue = IntegerValue("Keyboard-Pressed-Green", 255, 0, 255)
	private val keyboardPressedBlueValue = IntegerValue("Keyboard-Pressed-Blue", 255, 0, 255)
	private val keyboardPressedAlphaValue = IntegerValue("Keyboard-Pressed-Alpha", 192, 0, 255)

	private val keyboardUnpressedRedValue = IntegerValue("Keyboard-Unpressed-Red", 0, 0, 255)
	private val keyboardUnpressedGreenValue = IntegerValue("Keyboard-Unpressed-Green", 0, 0, 255)
	private val keyboardUnpressedBlueValue = IntegerValue("Keyboard-Unpressed-Blue", 0, 0, 255)
	private val keyboardUnpressedAlphaValue = IntegerValue("Keyboard-Unpressed-Alpha", 192, 0, 255)

	private val keyboardTextPressedRedValue = IntegerValue("Keyboard-Text-Pressed-Red", 255, 0, 255)
	private val keyboardTextPressedGreenValue = IntegerValue("Keyboard-Text-Pressed-Green", 255, 0, 255)
	private val keyboardTextPressedBlueValue = IntegerValue("Keyboard-Text-Pressed-Blue", 255, 0, 255)
	private val keyboardTextPressedAlphaValue = IntegerValue("Keyboard-Text-Pressed-Alpha", 192, 0, 255)

	private val keyboardTextUnpressedRedValue = IntegerValue("Keyboard-Text-Unpressed-Red", 255, 0, 255)
	private val keyboardTextUnpressedGreenValue = IntegerValue("Keyboard-Text-Unpressed-Green", 255, 0, 255)
	private val keyboardTextUnpressedBlueValue = IntegerValue("Keyboard-Text-Unpressed-Blue", 255, 0, 255)
	private val keyboardTextUnpressedAlphaValue = IntegerValue("Keyboard-Text-Unpressed-Alpha", 192, 0, 255)

	private val mouseValue = BoolValue("Mouse", true)

	private val mouseYPosValue = IntegerValue("Mouse-YPos", 0, -20, 20)

	private val mousePressedRedValue = IntegerValue("Mouse-Pressed-Red", 255, 0, 255)
	private val mousePressedGreenValue = IntegerValue("Mouse-Pressed-Green", 255, 0, 255)
	private val mousePressedBlueValue = IntegerValue("Mouse-Pressed-Blue", 255, 0, 255)
	private val mousePressedAlphaValue = IntegerValue("Mouse-Pressed-Alpha", 192, 0, 255)

	private val mouseUnpressedRedValue = IntegerValue("Mouse-Unpressed-Red", 0, 0, 255)
	private val mouseUnpressedGreenValue = IntegerValue("Mouse-Unpressed-Green", 0, 0, 255)
	private val mouseUnpressedBlueValue = IntegerValue("Mouse-Unpressed-Blue", 0, 0, 255)
	private val mouseUnpressedAlphaValue = IntegerValue("Mouse-Unpressed-Alpha", 192, 0, 255)

	private val mouseTextPressedRedValue = IntegerValue("Mouse-Text-Pressed-Red", 255, 0, 255)
	private val mouseTextPressedGreenValue = IntegerValue("Mouse-Text-Pressed-Green", 255, 0, 255)
	private val mouseTextPressedBlueValue = IntegerValue("Mouse-Text-Pressed-Blue", 255, 0, 255)
	private val mouseTextPressedAlphaValue = IntegerValue("Mouse-Text-Pressed-Alpha", 192, 0, 255)

	private val mouseTextUnpressedRedValue = IntegerValue("Mouse-Text-Unpressed-Red", 255, 0, 255)
	private val mouseTextUnpressedGreenValue = IntegerValue("Mouse-Text-Unpressed-Green", 255, 0, 255)
	private val mouseTextUnpressedBlueValue = IntegerValue("Mouse-Text-Unpressed-Blue", 255, 0, 255)
	private val mouseTextUnpressedAlphaValue = IntegerValue("Mouse-Text-Unpressed-Alpha", 192, 0, 255)

	private val spaceValue = BoolValue("Space", true)

	private val spaceYPosValue = IntegerValue("Space-YPos", 0, -20, 20)

	private val spacePressedRedValue = IntegerValue("Space-Pressed-Red", 255, 0, 255)
	private val spacePressedGreenValue = IntegerValue("Space-Pressed-Green", 255, 0, 255)
	private val spacePressedBlueValue = IntegerValue("Space-Pressed-Blue", 255, 0, 255)
	private val spacePressedAlphaValue = IntegerValue("Space-Pressed-Alpha", 192, 0, 255)

	private val spaceUnpressedRedValue = IntegerValue("Space-Unpressed-Red", 0, 0, 255)
	private val spaceUnpressedGreenValue = IntegerValue("Space-Unpressed-Green", 0, 0, 255)
	private val spaceUnpressedBlueValue = IntegerValue("Space-Unpressed-Blue", 0, 0, 255)
	private val spaceUnpressedAlphaValue = IntegerValue("Space-Unpressed-Alpha", 192, 0, 255)

	private val spaceTextPressedRedValue = IntegerValue("Space-Text-Pressed-Red", 255, 0, 255)
	private val spaceTextPressedGreenValue = IntegerValue("Space-Text-Pressed-Green", 255, 0, 255)
	private val spaceTextPressedBlueValue = IntegerValue("Space-Text-Pressed-Blue", 255, 0, 255)
	private val spaceTextPressedAlphaValue = IntegerValue("Space-Text-Pressed-Alpha", 192, 0, 255)

	private val spaceTextUnpressedRedValue = IntegerValue("Space-Text-Unpressed-Red", 255, 0, 255)
	private val spaceTextUnpressedGreenValue = IntegerValue("Space-Text-Unpressed-Green", 255, 0, 255)
	private val spaceTextUnpressedBlueValue = IntegerValue("Space-Text-Unpressed-Blue", 255, 0, 255)
	private val spaceTextUnpressedAlphaValue = IntegerValue("Space-Text-Unpressed-Alpha", 192, 0, 255)

	private val fadingTime = IntegerValue("FadingTime", 100, 0, 1000)

	private var fontValue = FontValue("Font", Fonts.font35)

	private val keyboardKeys = run {
		val gameSettings = mc.gameSettings
		val pressed = { Color(keyboardPressedRedValue.get(), keyboardPressedGreenValue.get(), keyboardPressedBlueValue.get(), keyboardPressedAlphaValue.get()) to Color(keyboardTextPressedRedValue.get(), keyboardTextPressedGreenValue.get(), keyboardTextPressedBlueValue.get(), keyboardTextPressedAlphaValue.get()) }
		val unpressed = { Color(keyboardUnpressedRedValue.get(), keyboardUnpressedGreenValue.get(), keyboardUnpressedBlueValue.get(), keyboardUnpressedAlphaValue.get()) to Color(keyboardTextUnpressedRedValue.get(), keyboardTextUnpressedGreenValue.get(), keyboardTextUnpressedBlueValue.get(), keyboardTextUnpressedAlphaValue.get()) }
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
		val pressed = { Color(mousePressedRedValue.get(), mousePressedGreenValue.get(), mousePressedBlueValue.get(), mousePressedAlphaValue.get()) to Color(mouseTextPressedRedValue.get(), mouseTextPressedGreenValue.get(), mouseTextPressedBlueValue.get(), mouseTextPressedAlphaValue.get()) }
		val unpressed = { Color(mouseUnpressedRedValue.get(), mouseUnpressedGreenValue.get(), mouseUnpressedBlueValue.get(), mouseUnpressedAlphaValue.get()) to Color(mouseTextUnpressedRedValue.get(), mouseTextUnpressedGreenValue.get(), mouseTextUnpressedBlueValue.get(), mouseTextUnpressedAlphaValue.get()) }
		val fading = fadingTime::get
		val yPos = { mouseYPosValue.get() + 40 }

		arrayOf(Key(gameSettings.keyBindAttack, 0, yPos, 29, 19, pressed, unpressed, fading), // Attack
			Key(gameSettings.keyBindUseItem, 30, yPos, 29, 19, pressed, unpressed, fading) // Use
		)
	}

	private val spaceBar = KeySpace(mc.gameSettings.keyBindJump, 0, { spaceYPosValue.get() + 60 }, 59, 11, { Color(spacePressedRedValue.get(), spacePressedGreenValue.get(), spacePressedBlueValue.get(), spacePressedAlphaValue.get()) to Color(spaceTextPressedRedValue.get(), spaceTextPressedGreenValue.get(), spaceTextPressedBlueValue.get(), spaceTextPressedAlphaValue.get()) }, { Color(spaceUnpressedRedValue.get(), spaceUnpressedGreenValue.get(), spaceUnpressedBlueValue.get(), spaceUnpressedAlphaValue.get()) to Color(spaceTextUnpressedRedValue.get(), spaceTextUnpressedGreenValue.get(), spaceTextUnpressedBlueValue.get(), spaceTextUnpressedAlphaValue.get()) }, fadingTime::get)

	/**
	 * Draw element
	 */
	override fun drawElement(): Border
	{
		var renderQueue = arrayOfNulls<Key>(7)
		if (keyboardValue.get()) renderQueue += keyboardKeys
		if (mouseValue.get()) renderQueue += mouseKeys
		if (spaceValue.get()) renderQueue += spaceBar

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

