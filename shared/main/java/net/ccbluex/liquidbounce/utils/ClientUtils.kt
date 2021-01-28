/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.INetworkManager
import net.ccbluex.liquidbounce.api.minecraft.network.login.server.ISPacketEncryptionRequest
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.security.PublicKey
import javax.crypto.SecretKey

@SideOnly(Side.CLIENT)
object ClientUtils : MinecraftInstance()
{
	@JvmStatic
	val logger: Logger = LogManager.getLogger("LiquidBounce")

	@JvmStatic
	fun disableFastRender()
	{
		LiquidBounce.wrapper.functions.disableFastRender()
	}

	@JvmStatic
	fun sendEncryption(networkManager: INetworkManager, secretKey: SecretKey, publicKey: PublicKey, encryptionRequest: ISPacketEncryptionRequest)
	{
		networkManager.sendPacket(classProvider.createCPacketEncryptionResponse(secretKey, publicKey, encryptionRequest.verifyToken)) { networkManager.enableEncryption(secretKey) }
	}

	@JvmStatic
	fun displayChatMessage(message: String?)
	{
		if (mc.thePlayer == null)
		{
			logger.info("(MCChat) {}", message)
			return
		}

		val jsonObject = JsonObject()
		jsonObject.addProperty("text", message)
		mc.thePlayer!!.addChatMessage(LiquidBounce.wrapper.functions.jsonToComponent("$jsonObject"))
	}
}
