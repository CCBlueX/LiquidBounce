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
import net.ccbluex.liquidbounce.utils.ClientUtils
import org.lwjgl.input.Keyboard
import kotlin.concurrent.thread

class GuiModsMenu(private val prevGui: IGuiScreen) : WrappedGuiScreen() {

    override fun initGui() {
        representedScreen.buttonList.add(classProvider.createGuiButton(0, representedScreen.width / 2 - 100, representedScreen.height / 4 + 48, "Forge Mods"))
        representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 - 100, representedScreen.height / 4 + 48 + 25, "Scripts"))
        representedScreen.buttonList.add(classProvider.createGuiButton(2, representedScreen.width / 2 - 100, representedScreen.height / 4 + 48 + 50, "Rich Presence: ${if (LiquidBounce.clientRichPresence.showRichPresenceValue) "§aON" else "§cOFF"}"))
        representedScreen.buttonList.add(classProvider.createGuiButton(3, representedScreen.width / 2 - 100, representedScreen.height / 4 + 48 + 75, "Back"))
    }

    override fun actionPerformed(button: IGuiButton) {
        when (val id = button.id) {
            0 -> mc.displayGuiScreen(classProvider.createGuiModList(this.representedScreen))
            1 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiScripts(this.representedScreen)))
            2 -> {
                val rpc = LiquidBounce.clientRichPresence
                rpc.showRichPresenceValue = when (val state = !rpc.showRichPresenceValue) {
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
                                ClientUtils.getLogger().error("Failed to setup Discord RPC.", throwable)
                                false
                            }
                        }
                        changeDisplayState(id, value)
                        value
                    }
                }
            }
            3 -> mc.displayGuiScreen(prevGui)
        }
    }

    private fun changeDisplayState(buttonId: Int, state: Boolean) {
        val button = representedScreen.buttonList[buttonId]
        val displayName = button.displayString
        button.displayString = when (state) {
            false -> displayName.replace("§aON", "§cOFF")
            true -> displayName.replace("§cOFF", "§aON")
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        representedScreen.drawBackground(0)

        Fonts.fontBold180.drawCenteredString("Mods", representedScreen.width / 2F, representedScreen.height / 8F + 5F, 4673984, true)

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