/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.menus

import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.compat.OAuthServer
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.ScaledResolution
import java.net.BindException

class GuiMicrosoftLoginProgress(val updateStatus: (String) -> Unit, val done: () -> Unit) : GuiScreen() {

    private var oAuthServer: OAuthServer? = null
    private var loginUrl: String? = null

    private var interrupted = false

    override fun initGui() {
        // This will start a login server and open the browser.
        try {
            oAuthServer = MicrosoftAccount.buildFromOpenBrowser(object : MicrosoftAccount.OAuthHandler {

                /**
                 * Called when the user has cancelled the authentication process or the thread has been interrupted
                 */
                override fun authError(error: String) {
                    if (!interrupted) {
                        errorAndDone(error)
                    }
                    loginUrl = null
                }

                /**
                 * Called when the user has completed authentication
                 */
                override fun authResult(account: MicrosoftAccount) {
                    loginUrl = null
                    if (LiquidBounce.fileManager.accountsConfig.accountExists(account)) {
                        errorAndDone("The account has already been added.")
                        return
                    }

                    LiquidBounce.fileManager.accountsConfig.addAccount(account)
                    LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.accountsConfig)
                    successAndDone()
                }

                /**
                 * Called when the server has prepared the user for authentication
                 */
                override fun openUrl(url: String) {
                    loginUrl = url
                    MiscUtils.showURL(url)
                }

            })
        } catch (bindException: BindException) {
            errorAndDone("Failed to start login server. (Port already in use)")
            ClientUtils.getLogger().error("Failed to start login server.", bindException)
        } catch (e: Exception) {
            errorAndDone("Failed to start login server.")
            ClientUtils.getLogger().error("Failed to start login server.", e)
        }

        buttonList.add(GuiButton(0, width / 2 - 100, height / 2 + 60, "Open URL"))
        buttonList.add(GuiButton(1, width / 2 - 100, height / 2 + 100, "Cancel"))
        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        val scaledResolution = ScaledResolution(Minecraft.getMinecraft())

        drawDefaultBackground()
        RenderUtils.drawLoadingCircle((scaledResolution.scaledWidth / 2).toFloat(), (scaledResolution.scaledHeight / 4 + 70).toFloat())
        drawCenteredString(fontRendererObj, "Logging into account...", width / 2, height / 2 - 60, 16777215)
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        // Not enabled buttons should be ignored
        if (!button.enabled) {
            return
        }

        when (button.id) {
            0 -> {
                if (loginUrl != null) {
                    MiscUtils.showURL(loginUrl!!)
                }
            }

            1 -> {
                errorAndDone("Cancelled.")
                done()
            }
        }


        super.actionPerformed(button)
    }

    override fun onGuiClosed() {
        interrupted = true
        oAuthServer?.stop()
        super.onGuiClosed()
    }

    private fun successAndDone() {
        updateStatus("§aSuccessfully logged in.")
        done()
    }

    private fun errorAndDone(error: String) {
        updateStatus("§c$error")
        done()
    }

}