/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.Background
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import java.nio.file.Files

class GuiBackground(val prevGui: GuiScreen) : GuiScreen() {

    companion object {
        var enabled = true
        var particles = false
    }

    private lateinit var enabledButton: GuiButton
    private lateinit var particlesButton: GuiButton

    override fun initGui() {
        enabledButton = GuiButton(1, width / 2 - 100, height / 4 + 35, "Enabled (${if (enabled) "On" else "Off"})")
        buttonList.add(enabledButton)
        particlesButton = GuiButton(2, width / 2 - 100, height / 4 + 50 + 25, "Particles (${if (particles) "On" else "Off"})")
        buttonList.add(particlesButton)
        buttonList.add(GuiButton(3, width / 2 - 100, height / 4 + 50 + 25 * 2, 98, 20, "Change wallpaper"))
        buttonList.add(GuiButton(4, width / 2 + 2, height / 4 + 50 + 25 * 2, 98, 20, "Reset wallpaper"))

        buttonList.add(GuiButton(0, width / 2 - 100, height / 4 + 55 + 25 * 4 + 5, "Back"))
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            1 -> {
                enabled = !enabled
                enabledButton.displayString = "Enabled (${if (enabled) "On" else "Off"})"
            }
            2 -> {
                particles = !particles
                particlesButton.displayString = "Particles (${if (particles) "On" else "Off"})"
            }
            3 -> {
                val file = MiscUtils.openFileChooser() ?: return

                if (file.isDirectory)
                    return

                // Delete old files
                LiquidBounce.background = null
                LiquidBounce.fileManager.backgroundImageFile.delete()
                LiquidBounce.fileManager.backgroundShaderFile.delete()

                // Copy new file
                val fileExtension = file.extension

                try {
                    val destFile =  when (fileExtension) {
                        "png" -> LiquidBounce.fileManager.backgroundImageFile
                        "frag", "glsl", "shader" -> LiquidBounce.fileManager.backgroundShaderFile
                        else -> {
                            MiscUtils.showErrorPopup("Error", "Invalid file extension: $fileExtension")
                            return
                        }
                    }

                    Files.copy(file.toPath(), destFile.outputStream())

                    // Load new background
                    try {
                        val background = Background.createBackground(destFile)
                        LiquidBounce.background = background
                    } catch (e: IllegalArgumentException) {
                        LiquidBounce.background = null
                        LiquidBounce.fileManager.backgroundImageFile.delete()
                        LiquidBounce.fileManager.backgroundShaderFile.delete()

                        MiscUtils.showErrorPopup("Error", "Invalid file extension: $fileExtension")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    MiscUtils.showErrorPopup("Error", "Exception class: " + e.javaClass.name + "\nMessage: " + e.message)

                    LiquidBounce.background = null
                    LiquidBounce.fileManager.backgroundImageFile.delete()
                    LiquidBounce.fileManager.backgroundShaderFile.delete()
                }
            }
            4 -> {
                LiquidBounce.background = null
                LiquidBounce.fileManager.backgroundImageFile.delete()
                LiquidBounce.fileManager.backgroundShaderFile.delete()
            }
            0 -> mc.displayGuiScreen(prevGui)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        Fonts.fontBold180.drawCenteredString("Background", this.width / 2F, height / 8F + 5F,
                4673984, true)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

}