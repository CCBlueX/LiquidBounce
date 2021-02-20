/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiChangeName(private val prevGui: GuiAltManager) : WrappedGuiScreen()
{
	private lateinit var name: IGuiTextField

	private var status = "\u00A77Idle..."

	override fun initGui()
	{
		Keyboard.enableRepeatEvents(true)

		val screen = representedScreen
		val buttonX = (screen.width shr 1) - 100
		val quarterScreen = screen.height shr 2

		val buttonList = screen.buttonList
		buttonList.add(classProvider.createGuiButton(1, buttonX, quarterScreen + 96, "Change"))
		buttonList.add(classProvider.createGuiButton(0, buttonX, quarterScreen + 120, "Back"))

		name = classProvider.createGuiTextField(2, Fonts.font40, buttonX, 60, 200, 20).apply {
			isFocused = true
			text = mc.session.username
			maxStringLength = 16
		}
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		val screen = representedScreen
		screen.drawBackground(0)

		val width = screen.width
		val height = screen.height

		val middleScreen = (width shr 1).toFloat()
		val quarterScreen = (height shr 2).toFloat()

		drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)

		Fonts.font40.drawCenteredString("Change Name", middleScreen, 34f, 0xffffff)
		Fonts.font40.drawCenteredString(status, middleScreen, quarterScreen + 84, 0xffffff)

		val name = name.apply(IGuiTextField::drawTextBox)

		if (name.text.isEmpty() && !name.isFocused) Fonts.font40.drawCenteredString("\u00A77Username", middleScreen - 74, 66f, 0xffffff)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	override fun actionPerformed(button: IGuiButton)
	{
		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui.representedScreen)

			1 ->
			{
				val nameText = name.text
				if (nameText.isEmpty())
				{
					status = "\u00A7cEnter a name!"
					return
				}

				val prevSession = mc.session

				if (!nameText.equals(prevSession.username, ignoreCase = true))
				{
					status = "\u00A7cJust change the upper and lower case!"
					return
				}

				mc.session = classProvider.createSession(nameText, prevSession.playerId, prevSession.token, prevSession.sessionType)

				LiquidBounce.eventManager.callEvent(SessionEvent())

				status = "\u00A7aChanged name to \u00A77$nameText\u00A7c."
				prevGui.status = status

				mc.displayGuiScreen(prevGui.representedScreen)
			}
		}
	}

	@Throws(IOException::class)
	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		if (keyCode == Keyboard.KEY_ESCAPE)
		{
			mc.displayGuiScreen(prevGui.representedScreen)
			return
		}

		if (name.isFocused) name.textboxKeyTyped(typedChar, keyCode)

		super.keyTyped(typedChar, keyCode)
	}

	@Throws(IOException::class)
	override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
	{
		name.mouseClicked(mouseX, mouseY, mouseButton)

		super.mouseClicked(mouseX, mouseY, mouseButton)
	}

	override fun updateScreen()
	{
		name.updateCursorCounter()

		super.updateScreen()
	}

	override fun onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false)

		super.onGuiClosed()
	}
}
