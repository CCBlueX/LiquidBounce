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
import java.awt.Color

class GuiUpdate : WrappedGuiScreen() {

    override fun initGui() {
        val j = representedScreen.height / 4 + 48

        representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 + 2, j + 24 * 2, 98, 20, "OK"))
        representedScreen.buttonList.add(classProvider.createGuiButton(2, representedScreen.width / 2 - 100, j + 24 * 2, 98, 20, "Download"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        representedScreen.drawBackground(0)

        Fonts.font35.drawCenteredString("b${LiquidBounce.latestVersion} got released!", representedScreen.width / 2.0f, representedScreen.height / 8.0f + 80, 0xffffff)
        Fonts.font35.drawCenteredString("Press \"Download\" to visit our website or dismiss this message by pressing \"OK\".", representedScreen.width / 2.0f, representedScreen.height / 8.0f + 80 + Fonts.font35.fontHeight, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)

        // Title
        GL11.glScalef(2F, 2F, 2F)
        Fonts.font35.drawCenteredString("New update available!", representedScreen.width / 4.0f, representedScreen.height / 16.0f + 20, Color(255, 0, 0).rgb)
    }

    override fun actionPerformed(button: IGuiButton) {
        when (button.id) {
            1 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiMainMenu()))
            2 -> MiscUtils.showURL("https://liquidbounce.net/download")
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode)
            return

        super.keyTyped(typedChar, keyCode)
    }
}
