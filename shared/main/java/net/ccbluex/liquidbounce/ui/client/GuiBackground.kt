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
import net.ccbluex.liquidbounce.utils.runAsync
import net.ccbluex.liquidbounce.utils.runSync
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

		buttonList.add(classProvider.createGuiButton(0, buttonX, quarterScreen + 55 + (25 shl 2) + 5, "Back"))

		enabledButton = classProvider.createGuiButton(1, buttonX, quarterScreen + 35, "Enabled (${if (enabled) "On" else "Off"})")
		buttonList.add(enabledButton)

		particlesButton = classProvider.createGuiButton(2, buttonX, quarterScreen + 75, "Particles (${if (particles) "On" else "Off"})")
		buttonList.add(particlesButton)

		val buttonY = quarterScreen + 100
		buttonList.add(classProvider.createGuiButton(3, buttonX, buttonY, 98, 20, "Change wallpaper"))
		buttonList.add(classProvider.createGuiButton(4, (representedScreen.width shr 1) + 2, buttonY, 98, 20, "Reset wallpaper"))
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

			3 -> runAsync {
				val file = MiscUtils.openFileChooser() ?: return@runAsync
				if (file.isDirectory) return@runAsync

				try
				{
					Files.copy(file.toPath(), FileOutputStream(LiquidBounce.fileManager.backgroundFile))

					val location = classProvider.createResourceLocation(LiquidBounce.CLIENT_NAME.toLowerCase() + "/background.png")
					LiquidBounce.background = location
					runSync { mc.textureManager.loadTexture(location, classProvider.createDynamicTexture(ImageIO.read(FileInputStream(LiquidBounce.fileManager.backgroundFile)))) }
				}
				catch (e: Exception)
				{
					e.printStackTrace()
					MiscUtils.showErrorPopup("Error", "Exception class: ${e.javaClass.name}\nMessage: ${e.message}")
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
