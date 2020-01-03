package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui
import net.ccbluex.liquidbounce.ui.font.Fonts

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class ReloadCommand : Command("reload", arrayOf("configreload")) {

    override fun execute(args: Array<String>) {
        chat("Reloading...")
        chat("§c§lReloading scripts...")
        LiquidBounce.CLIENT.scriptManager.reloadScripts()
        chat("§c§lReloading fonts...")
        Fonts.loadFonts()
        chat("§c§lReloading modules...")
        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.modulesConfig)
        chat("§c§lReloading values...")
        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.valuesConfig)
        chat("§c§lReloading accounts...")
        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.accountsConfig)
        chat("§c§lReloading friends...")
        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.friendsConfig)
        chat("§c§lReloading xray...")
        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.xrayConfig)
        chat("§c§lReloading HUD...")
        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.hudConfig)
        chat("§c§lReloading ClickGUI...")
        LiquidBounce.CLIENT.clickGui = ClickGui()
        LiquidBounce.CLIENT.fileManager.loadConfig(LiquidBounce.CLIENT.fileManager.clickGuiConfig)
        chat("Reloaded.")
    }

}