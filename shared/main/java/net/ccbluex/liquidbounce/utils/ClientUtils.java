/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils;

import java.security.PublicKey;

import javax.crypto.SecretKey;

import com.google.gson.JsonObject;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.INetworkManager;
import net.ccbluex.liquidbounce.api.minecraft.network.login.server.ISPacketEncryptionRequest;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SideOnly(Side.CLIENT)
public final class ClientUtils extends MinecraftInstance
{

	private static final Logger logger = LogManager.getLogger("LiquidBounce");

	public static Logger getLogger()
	{
		return logger;
	}

	public static void disableFastRender()
	{
		LiquidBounce.wrapper.getFunctions().disableFastRender();
	}

	public static void sendEncryption(final INetworkManager networkManager, final SecretKey secretKey, final PublicKey publicKey, final ISPacketEncryptionRequest encryptionRequest)
	{
		networkManager.sendPacket(classProvider.createCPacketEncryptionResponse(secretKey, publicKey, encryptionRequest.getVerifyToken()), () ->
		{
			networkManager.enableEncryption(secretKey);

			return null;
		});
	}

	public static void displayChatMessage(final String message)
	{
		if (mc.getThePlayer() == null)
		{
			logger.info("(MCChat){}", message);
			return;
		}

		final JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("text", message);

		mc.getThePlayer().addChatMessage(LiquidBounce.wrapper.getFunctions().jsonToComponent(jsonObject.toString()));
	}
}
