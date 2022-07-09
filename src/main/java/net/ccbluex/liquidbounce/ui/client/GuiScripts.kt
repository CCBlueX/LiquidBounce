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
import net.ccbluex.liquidbounce.api.util.WrappedGuiSlot
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import org.apache.commons.io.IOUtils
import org.lwjgl.input.Keyboard
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipFile

class GuiScripts(private val prevGui: IGuiScreen) : WrappedGuiScreen()
{

	private lateinit var list: GuiList

	override fun initGui()
	{
		list = GuiList(representedScreen)
		list.represented.registerScrollButtons(7, 8)
		list.elementClicked(-1, false, 0, 0)

		val buttonX = representedScreen.width - 80

		val provider = classProvider

		val buttonList = representedScreen.buttonList
		buttonList.add(provider.createGuiButton(0, buttonX, representedScreen.height - 65, 70, 20, "Back"))
		buttonList.add(provider.createGuiButton(1, buttonX, 46, 70, 20, "Import"))
		buttonList.add(provider.createGuiButton(2, buttonX, 70, 70, 20, "Delete"))
		buttonList.add(provider.createGuiButton(3, buttonX, 94, 70, 20, "Reload"))
		buttonList.add(provider.createGuiButton(4, buttonX, 118, 70, 20, "Folder"))
		buttonList.add(provider.createGuiButton(5, buttonX, 142, 70, 20, "Docs"))
		buttonList.add(provider.createGuiButton(6, buttonX, 166, 70, 20, "Find Scripts"))
		buttonList.add(provider.createGuiButton(6, buttonX, 190, 70, 20, "Find Scripts (Old Forum)"))
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)

		list.represented.drawScreen(mouseX, mouseY, partialTicks)

		Fonts.font40.drawCenteredString("\u00A79\u00A7lScripts", (representedScreen.width shr 1).toFloat(), 28.0f, 0xffffff)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	override fun actionPerformed(button: IGuiButton)
	{
		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui)

			1 -> try
			{
				val file = MiscUtils.openFileChooser() ?: return
				val fileName = file.name

				if (fileName.endsWith(".js"))
				{
					LiquidBounce.scriptManager.importScript(file)

					LiquidBounce.clickGui = ClickGui()
					FileManager.loadConfig(LiquidBounce.fileManager.clickGuiConfig)
					return
				}
				else if (fileName.endsWith(".zip"))
				{
					val zipFile = ZipFile(file)
					val entries = zipFile.entries()
					val scriptFiles = ArrayList<File>()

					while (entries.hasMoreElements())
					{
						val entry = entries.nextElement()
						val entryName = entry.name
						val entryFile = File(LiquidBounce.scriptManager.scriptsFolder, entryName)

						if (entry.isDirectory)
						{
							entryFile.mkdir()
							continue
						}

						val fileStream = zipFile.getInputStream(entry)
						val fileOutputStream = FileOutputStream(entryFile)

						IOUtils.copy(fileStream, fileOutputStream)
						fileOutputStream.close()
						fileStream.close()

						if (!entryName.contains("/")) scriptFiles.add(entryFile)
					}

					scriptFiles.forEach(LiquidBounce.scriptManager::loadScript)

					LiquidBounce.clickGui = ClickGui()
					FileManager.loadConfig(LiquidBounce.fileManager.clickGuiConfig)
					FileManager.loadConfig(LiquidBounce.fileManager.hudConfig)
					return
				}

				MiscUtils.showErrorPopup("Wrong file extension.", "The file extension has to be .js or .zip")
			}
			catch (t: Throwable)
			{
				ClientUtils.logger.error("Something went wrong while importing a script.", t)
				MiscUtils.showErrorPopup(t.javaClass.name, t.message)
			}

			2 -> try
			{
				if (list.getSelectedSlot() != -1)
				{
					val script = LiquidBounce.scriptManager.scripts[list.getSelectedSlot()]

					LiquidBounce.scriptManager.deleteScript(script)

					LiquidBounce.clickGui = ClickGui()
					FileManager.loadConfig(LiquidBounce.fileManager.clickGuiConfig)
					FileManager.loadConfig(LiquidBounce.fileManager.hudConfig)
				}
			}
			catch (t: Throwable)
			{
				ClientUtils.logger.error("Something went wrong while deleting a script.", t)
				MiscUtils.showErrorPopup(t.javaClass.name, t.message)
			}

			3 -> try
			{
				LiquidBounce.scriptManager.reloadScripts()
			}
			catch (t: Throwable)
			{
				ClientUtils.logger.error("Something went wrong while reloading all scripts.", t)
				MiscUtils.showErrorPopup(t.javaClass.name, t.message)
			}

			4 -> try
			{
				Desktop.getDesktop().open(LiquidBounce.scriptManager.scriptsFolder)
			}
			catch (t: Throwable)
			{
				ClientUtils.logger.error("Something went wrong while trying to open your scripts folder.", t)
				MiscUtils.showErrorPopup(t.javaClass.name, t.message)
			}

			5 -> try
			{
				Desktop.getDesktop().browse(URL("https://liquidbounce.net/docs/ScriptAPI/Getting%20Started").toURI())
			}
			catch (ignored: Exception)
			{
			}

			6 -> try
			{
				Desktop.getDesktop().browse(URL("https://forums.ccbluex.net/category/9/scripts").toURI())
			}
			catch (ignored: Exception)
			{
			}

			7 -> try
			{
				Desktop.getDesktop().browse(URL("https://forum.ccbluex.net/viewforum.php?id=16").toURI())
			}
			catch (ignored: Exception)
			{
			}
		}
	}

	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		when (keyCode)
		{
			Keyboard.KEY_ESCAPE ->
			{
				mc.displayGuiScreen(prevGui)
				return
			}

			Keyboard.KEY_UP -> list.elementClicked((list.getSelectedSlot() - 1).coerceAtLeast(0), false, 0, 0)
			Keyboard.KEY_DOWN -> list.elementClicked((list.getSelectedSlot() + 1).coerceAtMost(list.getSize() - 1), false, 0, 0)
			Keyboard.KEY_NEXT -> list.represented.scrollBy(representedScreen.height - 100)

			Keyboard.KEY_PRIOR ->
			{
				list.represented.scrollBy(-representedScreen.height + 100)
				return
			}

			else -> super.keyTyped(typedChar, keyCode)
		}
	}

	override fun handleMouseInput()
	{
		super.handleMouseInput()
		list.represented.handleMouseInput()
	}

	private inner class GuiList(gui: IGuiScreen) : WrappedGuiSlot(mc, gui.width, gui.height, 40, gui.height - 40, 30)
	{
		private var selectedSlot = 0

		override fun isSelected(id: Int) = selectedSlot == id

		fun getSelectedSlot() = if (selectedSlot > LiquidBounce.scriptManager.scripts.size) -1 else selectedSlot

		override fun getSize() = LiquidBounce.scriptManager.scripts.size

		override fun elementClicked(id: Int, doubleClick: Boolean, var3: Int, var4: Int)
		{
			selectedSlot = id
		}

		override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, mouseXIn: Int, mouseYIn: Int)
		{
			val script = LiquidBounce.scriptManager.scripts[id]

			val middleScreen = (representedScreen.width shr 1).toFloat()

			Fonts.font40.drawCenteredString("\u00A79" + script.scriptName + " \u00A77v" + script.scriptVersion, middleScreen, y + 2.0f, -4144960)
			Fonts.font40.drawCenteredString("by \u00A7c" + script.scriptAuthors.joinToString(), middleScreen, y + 15.0f, -4144960).coerceAtLeast(x)
		}

		override fun drawBackground()
		{
		}
	}
}
