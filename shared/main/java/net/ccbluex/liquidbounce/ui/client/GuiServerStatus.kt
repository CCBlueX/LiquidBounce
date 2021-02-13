/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import com.google.gson.Gson
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.io.IOException
import java.util.*

class GuiServerStatus(private val prevGui: IGuiScreen) : WrappedGuiScreen()
{
	private val status = HashMap<String, String>()

	override fun initGui()
	{
		val width = representedScreen.width
		val height = representedScreen.height

		representedScreen.buttonList.add(classProvider.createGuiButton(1, width / 2 - 100, height / 4 + 145, "Back"))

		WorkerUtils.workers.submit(::loadInformation)
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)

		val width = representedScreen.width
		val height = representedScreen.height

		var i = height / 4 + 40

		val middleScreen = width / 2.0f
		val quarterScreen = height / 4.0f

		RenderUtils.drawRect(middleScreen - 115, i - 5.0f, middleScreen + 115, quarterScreen + 43 + if (status.keys.isEmpty()) 10 else status.keys.size * Fonts.font40.fontHeight, Integer.MIN_VALUE)

		if (status.isEmpty())
		{
			Fonts.font40.drawCenteredString("Loading...", middleScreen, quarterScreen + 40, Color.WHITE.rgb)
		}
		else
		{
			for (server in status.keys)
			{
				val color = status[server]
				val text = "$server: ${
					when (color?.toLowerCase())
					{
						"green" -> "\u00A7aOnline and Stable"
						"yellow" -> "\u00A7eSlow or Unstable"
						"red" -> "\u00A7cOffline or Down"
						else -> color
					}
				}"

				Fonts.font40.drawCenteredString(text, middleScreen, i.toFloat(), Color.WHITE.rgb)

				i += Fonts.font40.fontHeight
			}
		}

		Fonts.fontBold180.drawCenteredString("Server Status", width / 2F, height / 8f + 5F, 4673984, true)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	private fun loadInformation()
	{
		status.clear()

		try
		{
			@Suppress("UNCHECKED_CAST")
			val linkedTreeMaps = Gson().fromJson(
				HttpUtils["https://status.mojang.com/check"], List::class.java
			) as List<Map<String, String>>

			for (linkedTreeMap in linkedTreeMaps) for ((key, value) in linkedTreeMap) status[key] = value
		}
		catch (e: IOException)
		{
			status["status.mojang.com/check"] = "red"
		}
	}

	override fun actionPerformed(button: IGuiButton)
	{
		if (button.id == 1) mc.displayGuiScreen(prevGui)
	}

	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		if (Keyboard.KEY_ESCAPE == keyCode)
		{
			mc.displayGuiScreen(prevGui)
			return
		}

		super.keyTyped(typedChar, keyCode)
	}
}
