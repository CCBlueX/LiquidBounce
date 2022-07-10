/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

class GuiWelcome : GuiScreen()
{
    override fun initGui()
    {
        buttonList.add(GuiButton(1, (width shr 1) - 100, height - 40, "Ok"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawBackground(0)

        val font = Fonts.font35
        val fontHeight = font.fontHeight

        val middleScreen = (width shr 1).toFloat()
        val y = height shr 3
        val y2 = y + 80f

        font.drawCenteredString("Thank you for downloading and installing our client!", middleScreen, y + 70f, 0xffffff, true)
        font.drawCenteredString("Here is some information you might find useful if you are using LiquidBounce for the first time.", middleScreen, y + 70f + fontHeight, 0xffffff, true)

        font.drawCenteredString("\u00A7lClickGUI:", middleScreen, y2 + fontHeight * 3, 0xffffff, true)
        val clickGuiBinds = LiquidBounce.moduleManager[ClickGUI::class.java].keyBinds
        if (clickGuiBinds.size > 0) font.drawCenteredString("Press ${Keyboard.getKeyName(clickGuiBinds.first())} to open up the ClickGUI", middleScreen, (height shr 3) + 80F + fontHeight * 4, 0xffffff, true)
        font.drawCenteredString("Right-click modules with a '+' next to them to edit their settings.", middleScreen, y2 + fontHeight * 5, 0xffffff, true)
        font.drawCenteredString("Hover a module or value to see it's description.", middleScreen, y2 + fontHeight * 6, 0xffffff, true)

        font.drawCenteredString("\u00A7lImportant Commands:", middleScreen, y2 + (fontHeight shl 3), 0xffffff, true)
        font.drawCenteredString(".bind <module> <add|remove> <key> / .bind <module> clear", middleScreen, y2 + fontHeight * 9, 0xffffff, true)
        font.drawCenteredString(".autosettings load <name> / .autosettings list", middleScreen, y2 + fontHeight * 10, 0xffffff, true)

        font.drawCenteredString("\u00A7lNeed help? Feel free to contact us!", middleScreen, y2 + fontHeight * 12, 0xffffff, true)
        font.drawCenteredString("YouTube: https://youtube.com/ccbluex", middleScreen, y2 + fontHeight * 13, 0xffffff, true)
        font.drawCenteredString("Twitter: https://twitter.com/ccbluex", middleScreen, y2 + fontHeight * 14, 0xffffff, true)
        font.drawCenteredString("Forum: https://forums.ccbluex.net/", middleScreen, y2 + fontHeight * 15, 0xffffff, true)
        font.drawCenteredString("(Old) Forum: https://forum.ccbluex.net/", middleScreen, y2 + fontHeight * 15, 0xffffff, true)

        super.drawScreen(mouseX, mouseY, partialTicks)

        // Title
        GL11.glScalef(2F, 2F, 2F)
        Fonts.font40.drawCenteredString("Welcome!", (width shr 2).toFloat(), (y shr 1) + 20f, -16741121, true)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        if (Keyboard.KEY_ESCAPE == keyCode) return

        super.keyTyped(typedChar, keyCode)
    }

    override fun actionPerformed(button: GuiButton)
    {
        if (button.id == 1) mc.displayGuiScreen(GuiMainMenu())
    }
}
