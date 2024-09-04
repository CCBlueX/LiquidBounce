/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce.clientRichPresence
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraftforge.fml.client.GuiModList
import org.lwjgl.input.Keyboard
import kotlin.concurrent.thread

class GuiModsMenu(private val prevGui: GuiScreen) : GuiScreen() {

    private lateinit var customTextField: GuiTextField

    override fun initGui() {
        buttonList.run {
            add(GuiButton(0, width / 2 - 100, height / 4 + 48, "Forge Mods"))
            add(GuiButton(1, width / 2 - 100, height / 4 + 48 + 25, "Scripts"))
            add(GuiButton(2, width / 2 - 100, height / 4 + 48 + 75, "Rich Presence: ${if (clientRichPresence.showRPCValue) "§aON" else "§cOFF"}"))
            add(GuiButton(3, width / 2 - 100, height / 4 + 48 + 100, "Rich Presence IP: ${if (clientRichPresence.showRPCServerIP) "§aON" else "§cOFF"}"))
            add(GuiButton(4, width / 2 - 100, height / 4 + 48 + 200, "Back"))
        }

        customTextField = GuiTextField(2, Fonts.font35, width / 2 - 100, height / 4 + 48 + 150, 200, 20)
        customTextField.maxStringLength = Int.MAX_VALUE
    }

    override fun actionPerformed(button: GuiButton) {
        when (val id = button.id) {
            0 -> mc.displayGuiScreen(GuiModList(this))
            1 -> mc.displayGuiScreen(GuiScripts(this))
            2 -> {
                val rpc = clientRichPresence
                rpc.showRPCValue = when (val state = !rpc.showRPCValue) {
                    false -> {
                        rpc.shutdown()
                        changeDisplayState(id, state)
                        false
                    }
                    true -> {
                        var value = true
                        thread {
                            value = try {
                                rpc.setup()
                                true
                            } catch (throwable: Throwable) {
                                LOGGER.error("Failed to setup Discord RPC.", throwable)
                                false
                            }
                        }
                        changeDisplayState(id, value)
                        value
                    }
                }
            }
            3 -> {
                val rpc = clientRichPresence
                rpc.showRPCServerIP = when (val state = !rpc.showRPCServerIP) {
                    false -> {
                        changeDisplayState(id, state)
                        false
                    }
                    true -> {
                        var value = true
                        thread {
                            value = try {
                                rpc.update()
                                true
                            } catch (throwable: Throwable) {
                                LOGGER.error("Failed to setup Discord RPC.", throwable)
                                false
                            }
                        }
                        changeDisplayState(id, value)
                        value
                    }
                }
            }
            4 -> mc.displayGuiScreen(prevGui)
        }
    }

    private fun changeDisplayState(buttonId: Int, state: Boolean) {
        val button = buttonList[buttonId]
        val displayName = button.displayString
        button.displayString = when (state) {
            false -> displayName.replace("§aON", "§cOFF")
            true -> displayName.replace("§cOFF", "§aON")
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        Fonts.fontBold180.drawCenteredString(translationMenu("mods"), width / 2F, height / 8F + 5F, 4673984, true)

        Fonts.font40.drawCenteredString("Rich Presence Text:", width / 2F, height / 4 + 48 + 135F, 0xffffff, true)

        customTextField.drawTextBox()
        if (customTextField.text.isEmpty() && !customTextField.isFocused) {
            Fonts.font35.drawStringWithShadow(
                clientRichPresence.customRPCText.ifEmpty { translationMenu("discordRPC.typeBox") },
                customTextField.xPosition + 4f,
                customTextField.yPosition + (customTextField.height - Fonts.font35.FONT_HEIGHT) / 2F,
                0xffffff
            )
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        customTextField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }

        if (customTextField.isFocused) {
            customTextField.textboxKeyTyped(typedChar, keyCode)
            clientRichPresence.customRPCText = customTextField.text
            saveConfig(valuesConfig)
        }

        super.keyTyped(typedChar, keyCode)
    }
}