/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import org.lwjgl.input.Keyboard
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Files
import javax.imageio.ImageIO

class GuiBackground(val prevGui: IGuiScreen) : WrappedGuiScreen()
{

	companion object
	{
		var enabled = true
		var particles = false
	}

	private lateinit var enabledButton: IGuiButton
	private lateinit var particlesButton: IGuiButton

	override fun initGui()
	{
		val buttonX = (representedScreen.width shr 1) - 100
		val quarterScreen = representedScreen.height shr 2

		val buttonList = representedScreen.buttonList

		val provider = classProvider

		buttonList.add(provider.createGuiButton(0, buttonX, quarterScreen + 55 + (25 shl 2) + 5, "Back"))

		enabledButton = provider.createGuiButton(1, buttonX, quarterScreen + 35, "Enabled (${if (enabled) "On" else "Off"})")
		buttonList.add(enabledButton)

		particlesButton = provider.createGuiButton(2, buttonX, quarterScreen + 50 + 25, "Particles (${if (particles) "On" else "Off"})")
		buttonList.add(particlesButton)

		val buttonY = quarterScreen + (25 shl 1) + 50
		buttonList.add(provider.createGuiButton(3, buttonX, buttonY, 98, 20, "Change wallpaper"))
		buttonList.add(provider.createGuiButton(4, (representedScreen.width shr 1) + 2, buttonY, 98, 20, "Reset wallpaper"))
	}

	override fun actionPerformed(button: IGuiButton)
	{
		when (button.id)
		{
			1 ->
			{
				enabled = !enabled
				enabledButton.displayString = "Enabled (${if (enabled) "On" else "Off"})"
			}

			2 ->
			{
				particles = !particles
				particlesButton.displayString = "Particles (${if (particles) "On" else "Off"})"
			}

			3 ->
			{
				val file = MiscUtils.openFileChooser() ?: return
				if (file.isDirectory) return

				try
				{
					Files.copy(file.toPath(), FileOutputStream(LiquidBounce.fileManager.backgroundFile))

					val location = classProvider.createResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + "/background.png")

					LiquidBounce.background = location

					mc.textureManager.loadTexture(location, classProvider.createDynamicTexture(ImageIO.read(FileInputStream(LiquidBounce.fileManager.backgroundFile))))
				}
				catch (e: Exception)
				{
					e.printStackTrace()
					MiscUtils.showErrorPopup("Error", "Exception class: " + e.javaClass.name + "\nMessage: " + e.message)
					LiquidBounce.fileManager.backgroundFile.delete()
				}
			}

			4 ->
			{
				LiquidBounce.background = null
				LiquidBounce.fileManager.backgroundFile.delete()
			}

			0 -> mc.displayGuiScreen(prevGui)
		}
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)
		Fonts.fontBold180.drawCenteredString("Background", (representedScreen.width shr 1).toFloat(), (representedScreen.height shr 3) + 5F, 4673984, true)

		super.drawScreen(mouseX, mouseY, partialTicks)
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
