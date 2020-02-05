/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class GuiUpdate : GuiScreen() {

    override fun initGui() {
        val j = height / 4 + 48

        buttonList.add(GuiButton(1, this.width / 2 + 2, j + 24 * 2, 98, 20, "OK"))
        buttonList.add(GuiButton(2, this.width / 2 - 100, j + 24 * 2, 98, 20, "Download"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        drawCenteredString(Fonts.font35, "b${LiquidBounce.latestVersion} got released!", width / 2, height / 8 + 80, 0xffffff)
        drawCenteredString(Fonts.font35, "Press \"Download\" to visit our website or dismiss this message by pressing \"OK\".", width / 2, height / 8 + 80 + Fonts.font35.FONT_HEIGHT, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)

        // Title
        GL11.glScalef(2F, 2F, 2F)
        drawCenteredString(Fonts.font35, "New update available!", width / 2 / 2, height / 8 / 2 + 20, Color(255, 0, 0).rgb)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            1 -> mc.displayGuiScreen(GuiMainMenu())
            2 -> MiscUtils.showURL("https://liquidbounce.net/download")
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode)
            return

        super.keyTyped(typedChar, keyCode)
    }
}
