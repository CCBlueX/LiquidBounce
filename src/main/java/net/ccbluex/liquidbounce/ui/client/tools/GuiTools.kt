/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.tools

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.ButtonWidget
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.input.Keyboard

class GuiTools(private val prevGui: Screen) : Screen() {

    override fun initGui() {
        buttonList.run {
            add(ButtonWidget(1, width / 2 - 100, height / 4 + 48 + 25, "Port Scanner"))
            add(ButtonWidget(0, width / 2 - 100, height / 4 + 48 + 25 * 2 + 5, "Back"))
        }
    }

    override fun actionPerformed(button: ButtonWidget) {
        when (button.id) {
            1 -> mc.displayScreen(GuiPortScanner(prevGui))
            0 -> mc.displayScreen(prevGui)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        Fonts.fontBold180.drawCenteredString("Tools", width / 2F, height / 8F + 5F, 4673984, true)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayScreen(prevGui)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }
}