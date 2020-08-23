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
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.LazySVGRenderer
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import org.apache.commons.io.IOUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.*
import java.util.zip.ZipFile

class GuiScripts(private val prevGui: IGuiScreen) : WrappedGuiScreen() {

    private lateinit var list: GuiList
    private val biohazardIcon: LazySVGRenderer = LazySVGRenderer("/assets/minecraft/liquidbounce/biohazard.svg")

    override fun initGui() {
        list = GuiList(representedScreen)
        list.represented.registerScrollButtons(7, 8)
        list.elementClicked(-1, false, 0, 0)

        val j = 22
        representedScreen.buttonList.add(classProvider.createGuiButton(0, representedScreen.width - 80, representedScreen.height - 65, 70, 20, "Back"))
        representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width - 80, j + 24, 70, 20, "Import"))
        representedScreen.buttonList.add(classProvider.createGuiButton(2, representedScreen.width - 80, j + 24 * 2, 70, 20, "Delete"))
        representedScreen.buttonList.add(classProvider.createGuiButton(3, representedScreen.width - 80, j + 24 * 3, 70, 20, "Reload"))
        representedScreen.buttonList.add(classProvider.createGuiButton(4, representedScreen.width - 80, j + 24 * 4, 70, 20, "Folder"))
        representedScreen.buttonList.add(classProvider.createGuiButton(5, representedScreen.width - 80, j + 24 * 5, 70, 20, "Docs"))
        representedScreen.buttonList.add(classProvider.createGuiButton(6, representedScreen.width - 80, j + 24 * 6, 70, 20, "Find Scripts"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        representedScreen.drawBackground(0)

        list.represented.drawScreen(mouseX, mouseY, partialTicks)

        Fonts.font40.drawCenteredString("§9§lScripts", representedScreen.width / 2.0f, 28.0f, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: IGuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> try {
                val file = MiscUtils.openFileChooser() ?: return
                val fileName = file.name

                if (fileName.endsWith(".js")) {
                    LiquidBounce.scriptManager.importScript(file)

                    LiquidBounce.clickGui = ClickGui()
                    LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.clickGuiConfig)
                    return
                } else if (fileName.endsWith(".zip")) {
                    val zipFile = ZipFile(file)
                    val entries = zipFile.entries()
                    val scriptFiles = ArrayList<File>()

                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        val entryName = entry.name
                        val entryFile = File(LiquidBounce.scriptManager.scriptsFolder, entryName)

                        if (entry.isDirectory) {
                            entryFile.mkdir()
                            continue
                        }

                        val fileStream = zipFile.getInputStream(entry)
                        val fileOutputStream = FileOutputStream(entryFile)

                        IOUtils.copy(fileStream, fileOutputStream)
                        fileOutputStream.close()
                        fileStream.close()

                        if (!entryName.contains("/"))
                            scriptFiles.add(entryFile)
                    }

                    scriptFiles.forEach { scriptFile -> LiquidBounce.scriptManager.loadScript(scriptFile) }

                    LiquidBounce.clickGui = ClickGui()
                    LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.clickGuiConfig)
                    LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.hudConfig)
                    return
                }

                MiscUtils.showErrorPopup("Wrong file extension.", "The file extension has to be .js or .zip")
            } catch (t: Throwable) {
                ClientUtils.getLogger().error("Something went wrong while importing a script.", t)
                MiscUtils.showErrorPopup(t.javaClass.name, t.message)
            }

            2 -> try {
                if (list.getSelectedSlot() != -1) {
                    val script = LiquidBounce.scriptManager.scripts[list.getSelectedSlot()]

                    LiquidBounce.scriptManager.deleteScript(script)

                    LiquidBounce.clickGui = ClickGui()
                    LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.clickGuiConfig)
                    LiquidBounce.fileManager.loadConfig(LiquidBounce.fileManager.hudConfig)
                }
            } catch (t: Throwable) {
                ClientUtils.getLogger().error("Something went wrong while deleting a script.", t)
                MiscUtils.showErrorPopup(t.javaClass.name, t.message)
            }
            3 -> try {
                LiquidBounce.scriptManager.reloadScripts()
            } catch (t: Throwable) {
                ClientUtils.getLogger().error("Something went wrong while reloading all scripts.", t)
                MiscUtils.showErrorPopup(t.javaClass.name, t.message)
            }
            4 -> try {
                Desktop.getDesktop().open(LiquidBounce.scriptManager.scriptsFolder)
            } catch (t: Throwable) {
                ClientUtils.getLogger().error("Something went wrong while trying to open your scripts folder.", t)
                MiscUtils.showErrorPopup(t.javaClass.name, t.message)
            }
            5 -> try {
                Desktop.getDesktop().browse(URL("https://liquidbounce.net/docs/ScriptAPI/Getting%20Started").toURI())
            } catch (ignored: Exception) { }

            6 -> try {
                Desktop.getDesktop().browse(URL("https://forum.ccbluex.net/viewforum.php?id=16").toURI())
            } catch (ignored: Exception) { }
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
        list.represented.handleMouseInput()
    }

    private inner class GuiList(gui: IGuiScreen) :
            WrappedGuiSlot(mc, gui.width, gui.height, 40, gui.height - 40, 30) {

        private var selectedSlot = 0

        override fun isSelected(id: Int) = selectedSlot == id

        internal fun getSelectedSlot() = if (selectedSlot > LiquidBounce.scriptManager.scripts.size) -1 else selectedSlot

        override fun getSize() = LiquidBounce.scriptManager.scripts.size

        override fun elementClicked(id: Int, doubleClick: Boolean, var3: Int, var4: Int) {
            selectedSlot = id
        }

        override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, var5: Int, var6: Int) {
            val script = LiquidBounce.scriptManager.scripts[id]

            var x = Fonts.font40.drawCenteredString((if (script.isSignatureValid) "§9" else "§c") + script.scriptName + " §7v" + script.scriptVersion, representedScreen.width / 2.0f, y + 2.0f, Color.LIGHT_GRAY.rgb)
            x = Fonts.font40.drawCenteredString("by §c" + script.scriptAuthors.joinToString(", "), representedScreen.width / 2.0f, y + 15.0f, Color.LIGHT_GRAY.rgb).coerceAtLeast(x)

            if (!script.isSignatureValid) {
                val width = Fonts.font40.fontHeight * 2 + 6

                classProvider.getGlStateManager().bindTexture(biohazardIcon.getTexture(width * classProvider.createScaledResolution(mc).scaleFactor).glTextureId)

                GL11.glColor4f(1.0f, 0.0f, 0.0f, 1.0f)

                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

                RenderUtils.drawModalRectWithCustomSizedTexture(x.toFloat() + 3, y.toFloat() + 1, 0.0f, 0.0f, width.toFloat(), width.toFloat(), width.toFloat(), width.toFloat())

                classProvider.getGlStateManager().bindTexture(0)
            }
        }

        override fun drawBackground() { }
    }
}