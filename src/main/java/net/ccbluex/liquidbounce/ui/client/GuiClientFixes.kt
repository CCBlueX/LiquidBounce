/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client

import net.ccbluex.liquidbounce.features.special.ClientFixes
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockFML
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockPayloadPackets
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockProxyPacket
import net.ccbluex.liquidbounce.features.special.ClientFixes.blockResourcePackExploit
import net.ccbluex.liquidbounce.features.special.ClientFixes.clientBrand
import net.ccbluex.liquidbounce.features.special.ClientFixes.fmlFixesEnabled
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.file.FileManager.valuesConfig
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.minecraft.client.gui.ButtonWidget
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.input.Keyboard
import java.io.IOException
import java.util.*

class GuiClientFixes(private val prevGui: Screen) : Screen() {

    private lateinit var enabledButton: ButtonWidget
    private lateinit var fmlButton: ButtonWidget
    private lateinit var proxyButton: ButtonWidget
    private lateinit var payloadButton: ButtonWidget
    private lateinit var customBrandButton: ButtonWidget
    private lateinit var resourcePackButton: ButtonWidget
    override fun initGui() {
        enabledButton = ButtonWidget(1, width / 2 - 100, height / 4 + 35, "AntiForge (" + (if (fmlFixesEnabled) "On" else "Off") + ")")
        fmlButton = ButtonWidget(2, width / 2 - 100, height / 4 + 35 + 25, "Block FML (" + (if (blockFML) "On" else "Off") + ")")
        proxyButton = ButtonWidget(3, width / 2 - 100, height / 4 + 35 + 25 * 2, "Block FML Proxy Packet (" + (if (blockProxyPacket) "On" else "Off") + ")")
        payloadButton = ButtonWidget(4, width / 2 - 100, height / 4 + 35 + 25 * 3, "Block Non-MC Payloads (" + (if (blockPayloadPackets) "On" else "Off") + ")")
        customBrandButton = ButtonWidget(5, width / 2 - 100, height / 4 + 35 + 25 * 4, "Brand ($clientBrand)")
        resourcePackButton = ButtonWidget(6, width / 2 - 100, height / 4 + 50 + 25 * 5, "Block Resource Pack Exploit (" + (if (blockResourcePackExploit) "On" else "Off") + ")")

        buttonList = listOf(
            enabledButton, fmlButton, proxyButton, payloadButton, customBrandButton, resourcePackButton,
            ButtonWidget(0, width / 2 - 100, height / 4 + 55 + 25 * 6 + 5, "Back")
        )
    }

    public override fun actionPerformed(button: ButtonWidget) {
        when (button.id) {
            1 -> {
                fmlFixesEnabled = !fmlFixesEnabled
                enabledButton.displayString = "AntiForge (${if (fmlFixesEnabled) "On" else "Off"})"
            }
            2 -> {
                blockFML = !blockFML
                fmlButton.displayString = "Block FML (${if (blockFML) "On" else "Off"})"
            }
            3 -> {
                blockProxyPacket = !blockProxyPacket
                proxyButton.displayString = "Block FML Proxy Packet (${if (blockProxyPacket) "On" else "Off"})"
            }
            4 -> {
                blockPayloadPackets = !blockPayloadPackets
                payloadButton.displayString = "Block FML Payload Packets (${if (blockPayloadPackets) "On" else "Off"})"
            }
            5 -> {
                val brands = listOf(*ClientFixes.possibleBrands)

                // Switch to next client brand
                clientBrand = brands[(brands.indexOf(clientBrand) + 1) % brands.size]

                customBrandButton.displayString = "Brand ($clientBrand)"
            }
            6 -> {
                blockResourcePackExploit = !blockResourcePackExploit
                resourcePackButton.displayString = "Block Resource Pack Exploit (${if (blockResourcePackExploit) "On" else "Off"})"
            }
            0 -> mc.displayScreen(prevGui)
        }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        Fonts.fontBold180.drawCenteredString("Fixes", width / 2f, height / 8f + 5f, 4673984, true)
        
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    public override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayScreen(prevGui)
            return
        }
        
        super.keyTyped(typedChar, keyCode)
    }

    override fun onGuiClosed() {
        saveConfig(valuesConfig)
        super.onGuiClosed()
    }
}