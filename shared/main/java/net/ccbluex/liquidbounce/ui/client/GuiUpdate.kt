/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class GuiUpdate : WrappedGuiScreen()
{
    override fun initGui()
    {
        val j = (representedScreen.height shr 2) + 48

        val middleScreen = representedScreen.width shr 1

        val buttonList = representedScreen.buttonList
        val provider = classProvider
        buttonList.add(provider.createGuiButton(1, middleScreen + 2, j + 48, 98, 20, "OK"))
        buttonList.add(provider.createGuiButton(2, middleScreen - 100, j + 48, 98, 20, "Download"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        representedScreen.drawBackground(0)

        val font = Fonts.font35

        val middleScreen = (representedScreen.width shr 1).toFloat()
        val textY = (representedScreen.height shr 3) + 80f

        font.drawCenteredString("b${LiquidBounce.latestVersion} got released!", middleScreen, textY, 0xffffff)
        font.drawCenteredString("Press \"Download\" to visit our website or dismiss this message by pressing \"OK\".", middleScreen, textY + font.fontHeight, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)

        // Title
        GL11.glScalef(2F, 2F, 2F)
        font.drawCenteredString("New update available!", (representedScreen.width shr 2).toFloat(), (representedScreen.height shr 4) + 20f, -65536)
    }

    override fun actionPerformed(button: IGuiButton)
    {
        when (button.id)
        {
            1 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiMainMenu()))
            2 -> MiscUtils.showURL("https://liquidbounce.net/download")
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        if (Keyboard.KEY_ESCAPE == keyCode) return

        super.keyTyped(typedChar, keyCode)
    }
}
