/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce.scriptManager
import net.ccbluex.liquidbounce.file.FileManager.clickGuiConfig
import net.ccbluex.liquidbounce.file.FileManager.hudConfig
import net.ccbluex.liquidbounce.file.FileManager.loadConfig
import net.ccbluex.liquidbounce.file.FileManager.loadConfigs
import net.ccbluex.liquidbounce.script.ScriptManager.reloadScripts
import net.ccbluex.liquidbounce.script.ScriptManager.scripts
import net.ccbluex.liquidbounce.script.ScriptManager.scriptsFolder
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import org.apache.commons.io.IOUtils
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.awt.Desktop
import java.io.File
import java.net.URL
import java.util.zip.ZipFile

class GuiScripts(private val prevGui: GuiScreen) : GuiScreen() {

    private lateinit var list: GuiList

    override fun initGui() {
        list = GuiList(this)
        list.registerScrollButtons(7, 8)
        list.elementClicked(-1, false, 0, 0)

        val j = 22
        buttonList.run {
            add(GuiButton(0, width - 80, height - 65, 70, 20, "Back"))
            add(GuiButton(1, width - 80, j + 24, 70, 20, "Import"))
            add(GuiButton(2, width - 80, j + 24 * 2, 70, 20, "Delete"))
            add(GuiButton(3, width - 80, j + 24 * 3, 70, 20, "Reload"))
            add(GuiButton(4, width - 80, j + 24 * 4, 70, 20, "Folder"))
            add(GuiButton(5, width - 80, j + 24 * 5, 70, 20, "Docs"))
            add(GuiButton(6, width - 80, j + 24 * 6, 70, 20, "Find Scripts"))
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        list.drawScreen(mouseX, mouseY, partialTicks)

        Fonts.font40.drawCenteredString("§9§lScripts", width / 2f, 28f, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> try {
                val file = MiscUtils.openFileChooser() ?: return
                val fileName = file.name

                if (fileName.endsWith(".js")) {
                    scriptManager.importScript(file)

                    loadConfig(clickGuiConfig)
                    return
                } else if (fileName.endsWith(".zip")) {
                    val zipFile = ZipFile(file)
                    val entries = zipFile.entries()
                    val scriptFiles = arrayListOf<File>()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val entryName = entry.name
                        val entryFile = File(scriptsFolder, entryName)

                        if (entry.isDirectory) {
                            entryFile.mkdir()
                            continue
                        }

                        val fileStream = zipFile.getInputStream(entry)
                        val fileOutputStream = entryFile.outputStream()

                        IOUtils.copy(fileStream, fileOutputStream)
                        fileOutputStream.close()
                        fileStream.close()

                        if ("/" !in entryName)
                            scriptFiles += entryFile
                    }

                    scriptFiles.forEach { scriptFile -> scriptManager.loadScript(scriptFile) }

                    loadConfigs(clickGuiConfig, hudConfig)
                    return
                }

                MiscUtils.showErrorPopup("Wrong file extension.", "The file extension has to be .js or .zip")
            } catch (t: Throwable) {
                LOGGER.error("Something went wrong while importing a script.", t)
                MiscUtils.showErrorPopup(t.javaClass.name, t.message!!)
            }

            2 -> try {
                if (list.getSelectedSlot() != -1) {
                    val script = scripts[list.getSelectedSlot()]

                    scriptManager.deleteScript(script)

                    loadConfigs(clickGuiConfig, hudConfig)
                }
            } catch (t: Throwable) {
                LOGGER.error("Something went wrong while deleting a script.", t)
                MiscUtils.showErrorPopup(t.javaClass.name, t.message!!)
            }
            3 -> try {
                reloadScripts()
            } catch (t: Throwable) {
                LOGGER.error("Something went wrong while reloading all scripts.", t)
                MiscUtils.showErrorPopup(t.javaClass.name, t.message!!)
            }
            4 -> try {
                Desktop.getDesktop().open(scriptsFolder)
            } catch (t: Throwable) {
                LOGGER.error("Something went wrong while trying to open your scripts folder.", t)
                MiscUtils.showErrorPopup(t.javaClass.name, t.message!!)
            }
            5 -> try {
                Desktop.getDesktop().browse(URL("https://github.com/CCBlueX/Documentation/blob/master/md/scriptapi_v2/getting_started.md").toURI())
            } catch (e: Exception) {
                LOGGER.error("Something went wrong while trying to open the web scripts docs.", e)
                MiscUtils.showErrorPopup("Scripts Error | Manual Link", "github.com/CCBlueX/Documentation/blob/master/md/scriptapi_v2/getting_started.md")
            }

            6 -> try {
                Desktop.getDesktop().browse(URL("https://forums.ccbluex.net/category/9/scripts").toURI())
            } catch (e: Exception) {
                LOGGER.error("Something went wrong while trying to open web scripts forums", e)
                MiscUtils.showErrorPopup("Scripts Error | Manual Link", "forums.ccbluex.net/category/9/scripts")
            }
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        list.handleMouseInput()
    }

    private inner class GuiList(gui: GuiScreen) :
            GuiSlot(mc, gui.width, gui.height, 40, gui.height - 40, 30) {

        private var selectedSlot = 0

        override fun isSelected(id: Int) = selectedSlot == id

        fun getSelectedSlot() = if (selectedSlot > scripts.size) -1 else selectedSlot

        override fun getSize() = scripts.size

        public override fun elementClicked(id: Int, doubleClick: Boolean, var3: Int, var4: Int) {
            selectedSlot = id
        }

        override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, var5: Int, var6: Int) {
            val script = scripts[id]

            Fonts.font40.drawCenteredString("§9" + script.scriptName + " §7v" + script.scriptVersion, width / 2f, y + 2f, Color.LIGHT_GRAY.rgb)
            Fonts.font40.drawCenteredString("by §c" + script.scriptAuthors.joinToString(", "), width / 2f, y + 15f, Color.LIGHT_GRAY.rgb).coerceAtLeast(x)
        }

        override fun drawBackground() { }
    }
}
