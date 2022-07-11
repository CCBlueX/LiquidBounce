/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.update.UpdateInfo
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class GuiUpdate : GuiScreen()
{
    override fun initGui()
    {
        val j = (height shr 2) + 48

        val middleScreen = width shr 1

        val buttonList = buttonList
        buttonList.add(GuiButton(1, middleScreen + 2, j + 48, 98, 20, "Ignore"))
        buttonList.add(GuiButton(2, middleScreen - 100, j + 48, 98, 20, "Go to download page"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawBackground(0)

        val font = Fonts.font35

        val middleScreen = (width shr 1).toFloat()
        val textY = (height shr 3) + 80f

        font.drawCenteredString("b${UpdateInfo.newestVersion?.lbVersion} got released!", middleScreen, textY, 0xffffff)
        font.drawCenteredString("Press \"Download\" to visit our website or dismiss this message by pressing \"OK\".", middleScreen, textY + font.fontHeight, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)

        // Title
        GL11.glScalef(2F, 2F, 2F)
        font.drawCenteredString("New update available!", (width shr 2).toFloat(), (height shr 4) + 20f, -65536)
    }

    override fun actionPerformed(button: GuiButton)
    {
        when (button.id)
        {
            1 -> mc.displayGuiScreen(GuiMainMenu())
            2 -> MiscUtils.showURL("https://liquidbounce.net/download")
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        if (Keyboard.KEY_ESCAPE == keyCode) return

        super.keyTyped(typedChar, keyCode)
    }
}
