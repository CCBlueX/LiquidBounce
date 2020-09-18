/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.ui.font.Fonts
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class GuiHelp : WrappedGuiScreen() {

    override fun initGui() {
        representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 - 100, representedScreen.height - 80, "Ok"))

    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        representedScreen.drawBackground(0)

        val font = Fonts.font35

        font.drawCenteredString("Help incomplete", representedScreen.width / 2F, representedScreen.height / 8F + 70, 0xffffff, true)

        super.drawScreen(mouseX, mouseY, partialTicks)

        // Title
        GL11.glScalef(2F, 2F, 2F)
        Fonts.font40.drawCenteredString("Welcome, ${mc.session.username}!", representedScreen.width / 2 / 2F, representedScreen.height / 8F / 2 + 20, Color(0, 140, 255).rgb, true)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode)
            return

        super.keyTyped(typedChar, keyCode)
    }

    override fun actionPerformed(button: IGuiButton) {
        if (button.id == 1) {
            mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiMainMenu()))
        }
    }
}