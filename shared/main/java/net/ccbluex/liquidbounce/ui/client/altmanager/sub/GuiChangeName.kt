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

		val buttonX = representedScreen.width / 2 - 100
		val quarterScreen = representedScreen.height / 4

		representedScreen.buttonList.add(classProvider.createGuiButton(1, buttonX, quarterScreen + 96, "Change"))
		representedScreen.buttonList.add(classProvider.createGuiButton(0, buttonX, quarterScreen + 120, "Back"))

		name = classProvider.createGuiTextField(2, Fonts.font40, buttonX, 60, 200, 20).apply {
			isFocused = true
			text = mc.session.username
			maxStringLength = 16
		}
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)

		drawRect(30, 30, representedScreen.width - 30, representedScreen.height - 30, Int.MIN_VALUE)

		Fonts.font40.drawCenteredString("Change Name", representedScreen.width / 2.0f, 34f, 0xffffff)
		Fonts.font40.drawCenteredString(status, representedScreen.width / 2.0f, representedScreen.height / 4.0f + 84, 0xffffff)

		name.drawTextBox()

		if (name.text.isEmpty() && !name.isFocused) Fonts.font40.drawCenteredString("\u00A77Username", representedScreen.width / 2.0f - 74, 66f, 0xffffff)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	override fun actionPerformed(button: IGuiButton)
	{
		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui.representedScreen)

			1 ->
			{
				if (name.text.isEmpty())
				{
					status = "\u00A7cEnter a name!"
					return
				}

				if (!name.text.equals(mc.session.username, ignoreCase = true))
				{
					status = "\u00A7cJust change the upper and lower case!"
					return
				}

				mc.session = classProvider.createSession(name.text, mc.session.playerId, mc.session.token, mc.session.sessionType)

				LiquidBounce.eventManager.callEvent(SessionEvent())

				status = "\u00A7aChanged name to \u00A77" + name.text + "\u00A7c."
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
