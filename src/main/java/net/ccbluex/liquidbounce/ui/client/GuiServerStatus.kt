/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import com.google.gson.Gson

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.io.IOException
import kotlin.concurrent.thread

class GuiServerStatus(private val prevGui: GuiScreen) : GuiScreen() {
    private val status = HashMap<String, String>()

    override fun initGui() {
        buttonList.add(GuiButton(1, width / 2 - 100, height / 4 + 145, "Back"))

        thread { loadInformation() }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        var i = height / 4 + 40
        RenderUtils.drawRect(width / 2.0f - 115, i - 5.0f, width / 2.0f + 115, height / 4.0f + 43 + if (status.keys.isEmpty()) 10 else status.keys.size * Fonts.font40.fontHeight, Integer.MIN_VALUE)

        if (status.isEmpty()) {
            Fonts.font40.drawCenteredString("Loading...", width / 2.0f, height / 4.0f + 40, Color.WHITE.rgb)
        } else {
            for (server in status.keys) {
                val color = status[server]
                Fonts.font40.drawCenteredString("$server: ${if (color.equals("red", ignoreCase = true)) "§c" else if (color.equals("yellow", ignoreCase = true)) "§e" else "§a"}${if (color.equals("red", ignoreCase = true)) "Offline" else if (color.equals("yellow", ignoreCase = true)) "Slow" else "Online"}", width / 2.0f, i.toFloat(), Color.WHITE.rgb)
                i += Fonts.font40.fontHeight
            }
        }

        Fonts.fontBold180.drawCenteredString("Server Status", width / 2F, height / 8f + 5F, 4673984, true)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun loadInformation() {
        status.clear()

        try {
            val linkedTreeMaps = Gson().fromJson(HttpUtils.get("https://status.mojang.com/check"),
                    List::class.java) as List<Map<String, String>>

            for (linkedTreeMap in linkedTreeMaps)
                for (entry in linkedTreeMap)
                    status[entry.key] = entry.value
        } catch (e: IOException) {
            status["status.mojang.com/check"] = "red"
        }

    }

    override fun actionPerformed(button: GuiButton) {
        if (button.id == 1) mc.displayGuiScreen(prevGui)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }
}
