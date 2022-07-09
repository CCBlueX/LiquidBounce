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
import net.ccbluex.liquidbounce.utils.runAsync
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiServerStatus(private val prevGui: GuiScreen) : GuiScreen()
{
    private val status = HashMap<String, String>()

    override fun initGui()
    {
        val width = width
        val height = height

        buttonList.add(GuiButton(1, (width shr 1) - 100, (height shr 2) + 145, "Back"))

        runAsync(::loadInformation)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawBackground(0)

        val width = width
        val height = height

        var i = (height shr 2) + 40

        val middleScreen = (width shr 1).toFloat()
        val quarterScreen = (height shr 2).toFloat()

        val font = Fonts.font40
        val fontHeight = font.fontHeight

        RenderUtils.drawRect(middleScreen - 115, i - 5.0f, middleScreen + 115, quarterScreen + 43 + if (status.keys.isEmpty()) 10 else status.keys.size * fontHeight, Integer.MIN_VALUE)

        if (status.isEmpty()) font.drawCenteredString("Loading...", middleScreen, quarterScreen + 40, -1)
        else for (address in status.keys)
        {
            val color = status[address]
            val text = "${
                when (color?.toLowerCase())
                {
                    "green" -> "\u00A7a"
                    "yellow" -> "\u00A7e"
                    "red" -> "\u00A7c"
                    else -> color
                }
            }$address: ${
                when (color?.toLowerCase())
                {
                    "green" -> "Online and Stable"
                    "yellow" -> "Slow or Unstable"
                    "red" -> "Offline or Down"
                    else -> color
                }
            }"

            font.drawCenteredString(text, middleScreen, i.toFloat(), -1)

            i += fontHeight
        }

        Fonts.fontBold180.drawCenteredString("Server Status", (width shr 1).toFloat(), (height shr 3) + 5F, 4673984, true)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    private fun loadInformation()
    {
        status.clear()

        try
        {
            @Suppress("UNCHECKED_CAST")
            val statusMaps = Gson().fromJson(HttpUtils["https://status.mojang.com/check"], List::class.java) as List<Map<String, String>>

            for (statusMap in statusMaps) for ((key, value) in statusMap) status[key] = value
        }
        catch (e: IOException)
        {
            status["status.mojang.com/check"] = "red"
        }
    }

    override fun actionPerformed(button: GuiButton)
    {
        if (button.id == 1) mc.displayGuiScreen(prevGui)
    }

    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        if (Keyboard.KEY_ESCAPE == keyCode)
        {
            mc.displayGuiScreen(prevGui)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }
}
