/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils

class GuiMainMenu : WrappedGuiScreen() {

    override fun initGui() {
        val defaultHeight = representedScreen.height / 4 + 48

        representedScreen.buttonList.add(classProvider.createGuiButton(100, representedScreen.width / 2 - 100, defaultHeight + 24, 98, 20, "AltManager"))
        representedScreen.buttonList.add(classProvider.createGuiButton(103, representedScreen.width / 2 + 2, defaultHeight + 24, 98, 20, "Mods"))
        representedScreen.buttonList.add(classProvider.createGuiButton(101, representedScreen.width / 2 - 100, defaultHeight + 24 * 2, 98, 20, "Server Status"))
        representedScreen.buttonList.add(classProvider.createGuiButton(102, representedScreen.width / 2 + 2, defaultHeight + 24 * 2, 98, 20, "Background"))

        representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 - 100, defaultHeight, 98, 20, functions.formatI18n("menu.singleplayer")))
        representedScreen.buttonList.add(classProvider.createGuiButton(2, representedScreen.width / 2 + 2, defaultHeight, 98, 20, functions.formatI18n("menu.multiplayer")))

        // Minecraft Realms
        //		this.buttonList.add(new classProvider.createGuiButton(14, this.width / 2 - 100, j + 24 * 2, I18n.format("menu.online", new Object[0])));

        representedScreen.buttonList.add(classProvider.createGuiButton(108, representedScreen.width / 2 - 100, defaultHeight + 24 * 3, "Contributors"))
        representedScreen.buttonList.add(classProvider.createGuiButton(0, representedScreen.width / 2 - 100, defaultHeight + 24 * 4, 98, 20, functions.formatI18n("menu.options")))
        representedScreen.buttonList.add(classProvider.createGuiButton(4, representedScreen.width / 2 + 2, defaultHeight + 24 * 4, 98, 20, functions.formatI18n("menu.quit")))
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        representedScreen.drawBackground(0)

        RenderUtils.drawRect(representedScreen.width / 2.0f - 115, representedScreen.height / 4.0f + 35, representedScreen.width / 2.0f + 115, representedScreen.height / 4.0f + 175, Integer.MIN_VALUE)

        Fonts.fontBold180.drawCenteredString(LiquidBounce.CLIENT_NAME, representedScreen.width / 2F, representedScreen.height / 8F, 4673984, true)
        Fonts.font35.drawCenteredString("b" + LiquidBounce.CLIENT_VERSION, representedScreen.width / 2F + 148, representedScreen.height / 8F + Fonts.font35.fontHeight, 0xffffff, true)

        representedScreen.superDrawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: IGuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(classProvider.createGuiOptions(this.representedScreen, mc.gameSettings))
            1 -> mc.displayGuiScreen(classProvider.createGuiSelectWorld(this.representedScreen))
            2 -> mc.displayGuiScreen(classProvider.createGuiMultiplayer(this.representedScreen))
            4 -> mc.shutdown()
            100 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiAltManager(this.representedScreen)))
            101 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiServerStatus(this.representedScreen)))
            102 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiBackground(this.representedScreen)))
            103 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiModsMenu(this.representedScreen)))
            108 -> mc.displayGuiScreen(classProvider.wrapGuiScreen(GuiContributors(this.representedScreen)))
        }
    }
}