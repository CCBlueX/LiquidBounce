/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.NetworkManager
import net.minecraft.network.login.client.C01PacketEncryptionResponse
import net.minecraft.network.login.server.S01PacketEncryptionRequest
import net.minecraft.util.IChatComponent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.reflect.Field
import java.security.PublicKey
import javax.crypto.SecretKey

object ClientUtils : MinecraftInstance()
{
    @JvmStatic
    val logger: Logger = LogManager.getLogger(LiquidBounce.CLIENT_NAME)

    private var fastRenderField: Field? = null

    init
    {
        try
        {
            val declaredField = GameSettings::class.java.getDeclaredField("ofFastRender")

            if (!declaredField.isAccessible) declaredField.isAccessible = true
            fastRenderField = declaredField
        }
        catch (_: NoSuchFieldException)
        {
            // ignored
        }
    }

    @JvmStatic
    fun disableFastRender()
    {
        val field = fastRenderField ?: return
        try
        {
            if (!field.isAccessible) field.isAccessible = true
            field.setBoolean(Minecraft.getMinecraft().gameSettings, false)
        }
        catch (ignored: IllegalAccessException)
        {
            // ignored
        }
    }

    @JvmStatic
    fun sendEncryption(networkManager: NetworkManager, secretKey: SecretKey, publicKey: PublicKey, encryptionRequest: S01PacketEncryptionRequest) = networkManager.sendPacket(C01PacketEncryptionResponse(secretKey, publicKey, encryptionRequest.verifyToken))

    @JvmStatic
    fun displayChatMessage(player: EntityPlayerSP?, message: String?)
    {
        if (player == null)
        {
            logger.info("(MCChat) {}", message)
            return
        }

        val jsonObject = JsonObject()
        jsonObject.addProperty("text", message)
        player.addChatMessage(IChatComponent.Serializer.jsonToComponent("$jsonObject"))
    }
}
