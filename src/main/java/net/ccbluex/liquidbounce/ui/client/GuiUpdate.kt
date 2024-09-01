/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce.IN_DEV
import net.ccbluex.liquidbounce.api.ClientUpdate
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.gui.ButtonWidget
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11.glScalef
import java.awt.Color

class GuiUpdate : Screen() {

    override fun initGui() {
        val j = height / 4 + 48

        buttonList.run {
            add(ButtonWidget(1, width / 2 + 2, j + 24 * 2, 98, 20, "Ignore"))
            add(ButtonWidget(2, width / 2 - 100, j + 24 * 2, 98, 20, "Go to download page"))
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        if (!IN_DEV) {
            Fonts.font35.drawCenteredString("${ClientUpdate.newestVersion?.lbVersion} got released!", width / 2f, height / 8f + 80, 0xffffff)
        } else {
            Fonts.font35.drawCenteredString("New build available!", width / 2f, height / 8f + 80, 0xffffff)
        }

        Fonts.font35.drawCenteredString("Press \"Download\" to visit our website or dismiss this message by pressing \"OK\".", width / 2f, height / 8f + 80 + Fonts.font35.fontHeight, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)

        // Title
        glScalef(2F, 2F, 2F)
        Fonts.font35.drawCenteredString("New update available!", width / 4f, height / 16f + 20, Color(255, 0, 0).rgb)
    }

    override fun actionPerformed(button: ButtonWidget) {
        when (button.id) {
            1 -> mc.setScreen(GuiMainMenu())
            2 -> MiscUtils.showURL("https://liquidbounce.net/download")
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode)
            return

        super.keyTyped(typedChar, keyCode)
    }
}
