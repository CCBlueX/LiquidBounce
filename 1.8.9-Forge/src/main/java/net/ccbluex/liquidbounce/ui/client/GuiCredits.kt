/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiSlot
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.util.*
import kotlin.concurrent.thread

class GuiCredits(private val prevGui: GuiScreen) : GuiScreen() {

    private lateinit var list: GuiList

    private val credits = ArrayList<Credit>()

    override fun initGui() {
        list = GuiList(this)
        list.registerScrollButtons(7, 8)
        list.elementClicked(-1, false, 0, 0)

        buttonList.add(GuiButton(1, width / 2 - 100, height - 30, "Back"))

        thread { loadCredits() }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        list.drawScreen(mouseX, mouseY, partialTicks)

        Gui.drawRect(width / 4, 40, width, height - 40, Integer.MIN_VALUE)

        if (list.getSelectedSlot() != -1) {
            val credit = credits[list.getSelectedSlot()]

            var y = 45
            Fonts.font40.drawString("Name: " + credit.name, (width / 4 + 5).toFloat(), y.toFloat(), Color.WHITE.rgb, true)

            if (credit.twitterName != null) {
                y += Fonts.font40.FONT_HEIGHT
                Fonts.font40.drawString("Twitter: " + credit.twitterName, (width / 4 + 5).toFloat(), y.toFloat(), Color.WHITE.rgb, true)
            }

            if (credit.youtubeName != null) {
                y += Fonts.font40.FONT_HEIGHT
                Fonts.font40.drawString("YouTube: " + credit.youtubeName, (width / 4 + 5).toFloat(), y.toFloat(), Color.WHITE.rgb, true)
            }

            y += Fonts.font40.FONT_HEIGHT

            for (s in credit.credits) {
                y += Fonts.font40.FONT_HEIGHT
                Fonts.font40.drawString(s, (width / 4 + 5).toFloat(), y.toFloat(), Color.WHITE.rgb, true)
            }
        }

        Fonts.font40.drawCenteredString("Credits", width / 2F, 6F, 0xffffff)

        if (credits.isEmpty()) {
            drawCenteredString(Fonts.font40, "Loading...", width / 8, height / 2, Color.WHITE.rgb)
            RenderUtils.drawLoadingCircle((width / 8).toFloat(), (height / 2 - 40).toFloat())
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        if (button.id == 1) {
            mc.displayGuiScreen(prevGui)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun handleMouseInput() {
        super.handleMouseInput()
        list.handleMouseInput()
    }

    private fun loadCredits() {
        credits.clear()

        try {
            val json = JsonParser()
                    .parse(HttpUtils.get("${LiquidBounce.CLIENT_CLOUD}/credits.json"))

            if (json !is JsonArray) return

            for (value in json) {
                if (value !is JsonObject)
                    continue

                val userCredits = ArrayList<String>()

                value.get("Credits").asJsonObject
                        .entrySet().forEach { stringJsonElementEntry -> userCredits.add(stringJsonElementEntry.value.asString) }

                credits.add(Credit(
                        getInfoFromJson(value, "Name")!!,
                        getInfoFromJson(value, "TwitterName"),
                        getInfoFromJson(value, "YouTubeName"),
                        userCredits
                ))
            }
        } catch (e: Exception) {
            ClientUtils.getLogger().error("Failed to load credits.", e)
        }
    }

    private fun getInfoFromJson(jsonObject: JsonObject, key: String): String? = if (jsonObject.has(key)) jsonObject.get(key).asString else null

    internal inner class Credit(val name: String, val twitterName: String?, val youtubeName: String?, val credits: List<String>)

    private inner class GuiList(gui: GuiScreen) :
            GuiSlot(mc, gui.width / 4, gui.height, 40, gui.height - 40, 15) {

        private var selectedSlot = 0

        override fun isSelected(id: Int) = selectedSlot == id

        override fun getSize() = credits.size

        internal fun getSelectedSlot() = if(selectedSlot > credits.size) -1 else selectedSlot

        public override fun elementClicked(index: Int, doubleClick: Boolean, var3: Int, var4: Int) {
            selectedSlot = index
        }

        override fun drawSlot(entryID: Int, p_180791_2_: Int, p_180791_3_: Int, p_180791_4_: Int, mouseXIn: Int, mouseYIn: Int) {
            val credit = credits[entryID]

            Fonts.font40.drawCenteredString(credit.name, width / 2F, p_180791_3_ + 2F, Color.WHITE.rgb, true)
        }

        override fun drawBackground() { }
    }
}