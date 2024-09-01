/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_WEBSITE
import net.ccbluex.liquidbounce.LiquidBounce.clientVersionText
import net.ccbluex.liquidbounce.api.messageOfTheDay
import net.ccbluex.liquidbounce.lang.translationMenu
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.minecraft.client.gui.*
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.widget.ButtonWidget
import net.minecraft.client.resources.I18n

class GuiMainMenu : Screen() {

    override fun initGui() {
        val defaultHeight = height / 4 + 48

        buttonList.run {
            add(ButtonWidget(100, width / 2 - 100, defaultHeight + 24, 98, 20, translationMenu("altManager")))
            add(ButtonWidget(103, width / 2 + 2, defaultHeight + 24, 98, 20, translationMenu("mods")))
            add(ButtonWidget(101, width / 2 - 100, defaultHeight + 24 * 2, 98, 20, translationMenu("serverStatus")))
            add(ButtonWidget(102, width / 2 + 2, defaultHeight + 24 * 2, 98, 20, translationMenu("configuration")))

            add(ButtonWidget(1, width / 2 - 100, defaultHeight, 98, 20, I18n.format("menu.singleplayer")))
            add(ButtonWidget(2, width / 2 + 2, defaultHeight, 98, 20, I18n.format("menu.multiplayer")))

            // Minecraft Realms
            //		this.buttonList.add(new ButtonWidget(14, this.width / 2 - 100, j + 24 * 2, I18n.format("menu.online", new Object[0])));

            add(ButtonWidget(108, width / 2 - 100, defaultHeight + 24 * 3, translationMenu("contributors")))
            add(ButtonWidget(0, width / 2 - 100, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.options")))
            add(ButtonWidget(4, width / 2 + 2, defaultHeight + 24 * 4, 98, 20, I18n.format("menu.quit")))
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        drawRoundedBorderRect(width / 2f - 115, height / 4f + 35, width / 2f + 115, height / 4f + 175,
            2f,
            Integer.MIN_VALUE,
            Integer.MIN_VALUE,
            3F
        )

        Fonts.fontBold180.drawCenteredString(CLIENT_NAME, width / 2F, height / 8F, 4673984, true)
        Fonts.font35.drawCenteredString(clientVersionText, width / 2F + 148, height / 8F + Fonts.font35.fontHeight, 0xffffff, true)

        val messageOfTheDay = messageOfTheDay?.message
        if (messageOfTheDay?.isNotBlank() == true) {
            val lines = messageOfTheDay.lines()

            drawRoundedBorderRect(width / 2f - 115,
                height / 4f + 190,
                width / 2f + 115,
                height / 4f + 192 + (Fonts.font35.fontHeight * lines.size),
                2f,
                Integer.MIN_VALUE,
                Integer.MIN_VALUE,
                3F
            )

            // Draw rect below main rect and within draw MOTD text
            for ((index, line) in lines.withIndex()) {
                Fonts.font35.drawCenteredString(line, width / 2F, height / 4f + 197.5f
                        + (Fonts.font35.fontHeight * index), 0xffffff, true)
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // When clicking the message of the day text
        val messageOfTheDay = messageOfTheDay?.message
        if (messageOfTheDay?.isNotBlank() == true) {
            val lines = messageOfTheDay.lines()
            val motdHeight = height / 4f + 190
            val motdWidth = width / 2f - 115
            val motdHeightEnd = motdHeight + 192 + (Fonts.font35.fontHeight * lines.size)

            if (mouseX >= motdWidth && mouseX <= width / 2f + 115 && mouseY >= motdHeight && mouseY <= motdHeightEnd) {
                // Open liquidbounce website
                MiscUtils.showURL("https://$CLIENT_WEBSITE")
            }
        }

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun actionPerformed(button: ButtonWidget) {
        when (button.id) {
            0 -> mc.displayScreen(GuiOptions(this, mc.options))
            1 -> mc.displayScreen(GuiSelectWorld(this))
            2 -> mc.displayScreen(MultiplayerScreen(this))
            4 -> mc.shutdown()
            100 -> mc.displayScreen(GuiAltManager(this))
            101 -> mc.displayScreen(GuiServerStatus(this))
            102 -> mc.displayScreen(GuiClientConfiguration(this))
            103 -> mc.displayScreen(GuiModsMenu(this))
            108 -> mc.displayScreen(GuiContributors(this))
        }
    }
}