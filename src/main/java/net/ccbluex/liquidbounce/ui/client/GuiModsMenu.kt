/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.runAsync
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.client.GuiModList
import org.lwjgl.input.Keyboard

class GuiModsMenu(private val prevGui: GuiScreen) : GuiScreen()
{

    override fun initGui()
    {
        val buttonX = (width shr 1) - 100
        val buttonY = (height shr 2) + 48

        val buttonList = buttonList

        buttonList.add(GuiButton(0, buttonX, buttonY, "Forge Mods"))
        buttonList.add(GuiButton(1, buttonX, buttonY + 25, "Scripts"))
        buttonList.add(GuiButton(2, buttonX, buttonY + 50, "Rich Presence: ${if (LiquidBounce.clientRichPresence.showRichPresenceValue) "\u00A7aON" else "\u00A7cOFF"}"))
        buttonList.add(GuiButton(3, buttonX, buttonY + 75, "Back"))
    }

    override fun actionPerformed(button: GuiButton)
    {
        when (val id = button.id)
        {
            0 -> mc.displayGuiScreen(GuiModList(this))
            1 -> mc.displayGuiScreen(GuiScripts(this))

            2 ->
            {
                val rpc = LiquidBounce.clientRichPresence
                rpc.showRichPresenceValue = when (!rpc.showRichPresenceValue)
                {
                    false ->
                    {
                        rpc.shutdown()
                        changeDisplayState(id, false)
                        false
                    }

                    true ->
                    {
                        var value = true
                        runAsync {
                            value = try
                            {
                                rpc.setup()
                                true
                            }
                            catch (throwable: Throwable)
                            {
                                ClientUtils.logger.error("Failed to setup Discord RPC.", throwable)
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

    private fun changeDisplayState(buttonId: Int, state: Boolean)
    {
        val button = buttonList[buttonId]
        val displayName = button.displayString
        button.displayString = when (state)
        {
            false -> displayName.replace("\u00A7aON", "\u00A7cOFF")
            true -> displayName.replace("\u00A7cOFF", "\u00A7aON")
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawBackground(0)

        Fonts.fontBold180.drawCenteredString("Mods", (width shr 1).toFloat(), (height shr 3) + 5F, 4673984, true)

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
