/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import joptsimple.internal.Strings
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import org.intellij.lang.annotations.Language
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color

class GuiUnsignedScripts : WrappedGuiScreen() {
    private lateinit var lines: Array<String>

    override fun initGui() {
        @Language("TEXT")
        val text = "Following scripts aren't verified by CCBlueX:\n\n" + LiquidBounce.scriptManager.lateInitScripts.joinToString("\n") { "\u00A7c${it.scriptName} ${it.scriptVersion} (by ${Strings.join(it.scriptAuthors, ", ")})" } + "\n\n\u00A7rLiquidBounce scripts have full access on your harddrive, network etc.\nThey can do as much damage as any other program!\n\nPlease only run scripts from trusted developers.\n\nTip: Scripts with the extention .signed.js are usually signed."

        val sb = StringBuffer()
        val lines = ArrayList<String>()

        for (char in text.toCharArray()) {
            if (char == '\n') {
                lines.add(sb.toString())

                sb.setLength(0)
            } else {
                sb.append(char)
            }
        }

        if (sb.isNotEmpty()) {
            lines.add(sb.toString())
        }

        this.lines = lines.toTypedArray()

        representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 - 100, representedScreen.height - 65, "Trust these scripts"))
        representedScreen.buttonList.add(classProvider.createGuiButton(2, representedScreen.width / 2 - 100, representedScreen.height - 40, "Stay safe"))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        representedScreen.drawBackground(0)

        val font = Fonts.font35

        val height = this.lines.size * font.fontHeight

        var yOffset = -(height / 2)

        for (line in this.lines) {
            font.drawCenteredString(line, representedScreen.width / 2F, representedScreen.height / 2F + yOffset, 0xffffff, true)

            yOffset += font.fontHeight
        }

        super.drawScreen(mouseX, mouseY, partialTicks)

        // Title
        GL11.glScalef(2F, 2F, 2F)

        val scaledResolution = classProvider.createScaledResolution(mc)

        Fonts.font40.drawCenteredString("Warning: Insecure scripts", representedScreen.width / (2f * scaledResolution.scaleFactor), (representedScreen.height - height) / (2f * scaledResolution.scaleFactor) - Fonts.font40.fontHeight * 2, Color(0, 140, 255).rgb, true)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode)
            return

        super.keyTyped(typedChar, keyCode)
    }

    override fun actionPerformed(button: IGuiButton) {
        if (button.id == 1) {
            for (script in LiquidBounce.scriptManager.lateInitScripts) {
                try {
                    script.initScript()

                    LiquidBounce.scriptManager.scripts.add(script)
                } catch (e: Throwable) {
                    ClientUtils.getLogger().error("[ScriptAPI] Failed to load script '${script.scriptFile.name}'.", e)
                }
            }

            // Remove the scripts from the queue
            LiquidBounce.scriptManager.lateInitScripts.clear()

            mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiMainMenu()))
        } else if (button.id == 2) {
            // Remove the scripts from the queue
            LiquidBounce.scriptManager.lateInitScripts.clear()

            mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiMainMenu()))
        }
    }
}