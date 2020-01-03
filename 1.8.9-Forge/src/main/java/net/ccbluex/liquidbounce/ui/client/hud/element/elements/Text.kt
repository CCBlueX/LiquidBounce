package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.hud.GuiHudDesigner
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.ServerUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.TextValue
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.FontRenderer
import net.minecraft.util.ChatAllowedCharacters
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.text.DecimalFormat
import java.text.SimpleDateFormat

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ElementInfo(name = "Text")
class Text : Element() {
    companion object {
        val DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd")
        val HOUR_FORMAT = SimpleDateFormat("HH:mm")

        val DECIMAL_FORMAT = DecimalFormat("#.00")
    }

    private val displayString = TextValue("DisplayText", "")
    private val redValue = IntegerValue("Red", 255, 0, 255)
    private val greenValue = IntegerValue("Green", 255, 0, 255)
    private val blueValue = IntegerValue("Blue", 255, 0, 255)
    private val rainbow = BoolValue("Rainbow", false)
    private val shadow = BoolValue("Shadow", true)
    private var fontRenderer: FontRenderer = Fonts.font40

    private var editMode = false
    private var editTicks = 0
    private var prevClick = 0L

    private var displayText = display

    private val display: String
        get() {
            val textContent = if (displayString.get().isEmpty() && !editMode)
                "Text Element"
            else
                displayString.get()


            return multiReplace(textContent)
        }

    private fun getReplacement(str: String): String? {
        if (mc.thePlayer != null) {
            when (str) {
                "x" -> return DECIMAL_FORMAT.format(mc.thePlayer.posX)
                "y" -> return DECIMAL_FORMAT.format(mc.thePlayer.posY)
                "z" -> return DECIMAL_FORMAT.format(mc.thePlayer.posZ)
                "xdp" -> return mc.thePlayer.posX.toString()
                "ydp" -> return mc.thePlayer.posY.toString()
                "zdp" -> return mc.thePlayer.posZ.toString()
                "ping" -> return EntityUtils.getPing(mc.thePlayer).toString()
            }
        }

        return when (str) {
            "username" -> mc.getSession().username
            "clientName" -> LiquidBounce.CLIENT_NAME
            "clientVersion" -> "b${LiquidBounce.CLIENT_VERSION}"
            "clientCreator" -> LiquidBounce.CLIENT_CREATOR
            "fps" -> Minecraft.getDebugFPS().toString()
            "date" -> DATE_FORMAT.format(System.currentTimeMillis())
            "time" -> HOUR_FORMAT.format(System.currentTimeMillis())
            "serverIp" -> ServerUtils.getRemoteIp()
            else -> null // Null = don't replace
        }
    }

    private fun multiReplace(str: String): String {
        var lastPercent = -1
        val result = StringBuilder()
        for (i in str.indices) {
            if (str[i] == '%') {
                if (lastPercent != -1) {
                    if (lastPercent + 1 != i) {
                        val replacement = getReplacement(str.substring(lastPercent + 1, i))

                        if (replacement != null) {
                            result.append(replacement)
                            lastPercent = -1
                            continue
                        }
                    }
                    result.append(str, lastPercent, i)
                }
                lastPercent = i
            } else if (lastPercent == -1) {
                result.append(str[i])
            }
        }

        if (lastPercent != -1) {
            result.append(str, lastPercent, str.length)
        }

        return result.toString()
    }


    override fun drawElement() {
        val color = Color(redValue.get(), greenValue.get(), blueValue.get()).rgb
        val location = locationFromFacing

        fontRenderer.drawString(displayText, location[0].toFloat(), location[1].toFloat(), if (rainbow.get())
            ColorUtils.rainbow(400000000L).rgb else color, shadow.get())

        if (editMode && mc.currentScreen is GuiHudDesigner && editTicks <= 40)
            fontRenderer.drawString("_", location[0] + fontRenderer.getStringWidth(displayText) + 2F,
                    location[1].toFloat(), if (rainbow.get()) ColorUtils.rainbow(400000000L).rgb else color, shadow.get())

        if (mc.currentScreen is GuiHudDesigner) {
            RenderUtils.drawBorderedRect(
                    location[0] - 2.toFloat(),
                    location[1] - 2.toFloat(),
                    (location[0] +
                            fontRenderer.getStringWidth(displayText) + 2).toFloat(),
                    location[1] + fontRenderer.FONT_HEIGHT.toFloat(), 3f, Int.MIN_VALUE,
                    0
            )
        } else if (editMode) {
            editMode = false
            updateElement()
        }
    }

    override fun destroyElement() {}

    override fun updateElement() {
        editTicks += 5
        if (editTicks > 80) editTicks = 0

        displayText = if (editMode) displayString.get() else display
    }

    override fun handleMouseClick(mouseX: Int, mouseY: Int, mouseButton: Int) {
        if (isMouseOverElement(mouseX, mouseY) && mouseButton == 0) {
            if (System.currentTimeMillis() - prevClick <= 250L)
                editMode = true

            prevClick = System.currentTimeMillis()
        } else {
            editMode = false
        }
    }

    override fun handleKey(c: Char, keyCode: Int) {
        if (editMode && mc.currentScreen is GuiHudDesigner) {
            if (keyCode == Keyboard.KEY_BACK) {
                if (displayString.get().isNotEmpty())
                    displayString.set(displayString.get().substring(0, displayString.get().length - 1))

                updateElement()
                return
            }

            if (ChatAllowedCharacters.isAllowedCharacter(c) || c == '§')
                displayString.set(displayString.get() + c)

            updateElement()
        }
    }

    override fun isMouseOverElement(mouseX: Int, mouseY: Int): Boolean {
        val location = locationFromFacing
        return mouseX >= location[0] && mouseY >= location[1] && mouseX <= location[0] + fontRenderer.getStringWidth(if (displayString.get().isEmpty()) "Text Element" else displayText) && mouseY <= location[1] + fontRenderer.FONT_HEIGHT
    }

    fun setText(s: String): Text {
        displayString.set(s)
        return this
    }

    fun setColor(c: Color): Text {
        redValue.set(c.red)
        greenValue.set(c.green)
        blueValue.set(c.blue)
        return this
    }

    fun setRainbow(b: Boolean): Text {
        rainbow.set(b)
        return this
    }

    fun setShadow(b: Boolean): Text {
        shadow.set(b)
        return this
    }

    fun setFontRenderer(fontRenderer: FontRenderer): Text {
        this.fontRenderer = fontRenderer
        return this
    }

}