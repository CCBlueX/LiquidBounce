/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.awt.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import com.mojang.authlib.GameProfile;

import net.ccbluex.liquidbounce.injection.backend.ServerDataImplKt;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.mcleaks.MCLeaks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiConnecting.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiConnecting extends GuiScreen
{

	@Shadow
	private NetworkManager networkManager;

	@Shadow
	@Final
	private static Logger logger;

	@Shadow
	private boolean cancel;

	@Shadow
	@Final
	private GuiScreen previousGuiScreen;

	@Shadow
	@Final
	private static AtomicInteger CONNECTION_ID;

	@Inject(method = "connect", at = @At("HEAD"))
	private void headConnect(final String ip, final int port, final CallbackInfo callbackInfo)
	{
		ServerUtils.lastServerData = ServerDataImplKt.wrap(new ServerData("", ip + ":" + port, false));
	}

	@Inject(method = "connect", at = @At(value = "NEW", target = "net/minecraft/network/login/client/C00PacketLoginStart"), cancellable = true)
	private void mcLeaks(final CallbackInfo callbackInfo)
	{
		if (MCLeaks.isAltActive())
		{
			networkManager.sendPacket(new C00PacketLoginStart(new GameProfile(null, MCLeaks.getSession().getUsername())));
			callbackInfo.cancel();
		}
	}

	/**
	 * @author CCBlueX
	 * @reason
	 */
	@Overwrite
	private void connect(final String ip, final int port)
	{
		logger.info("Connecting to {}, {}", ip, port);

		new Thread(() ->
		{
			InetAddress inetaddress = null;

			try
			{
				if (cancel)
					return;

				inetaddress = InetAddress.getByName(ip);
				networkManager = NetworkManager.createNetworkManagerAndConnect(inetaddress, port, mc.gameSettings.isUsingNativeTransport());
				networkManager.setNetHandler(new NetHandlerLoginClient(networkManager, mc, previousGuiScreen));
				networkManager.sendPacket(new C00Handshake(47, ip, port, EnumConnectionState.LOGIN, true));
				networkManager.sendPacket(new C00PacketLoginStart(MCLeaks.isAltActive() ? new GameProfile(null, MCLeaks.getSession().getUsername()) : mc.getSession().getProfile()));
			}
			catch (final UnknownHostException unknownhostexception)
			{
				if (cancel)
					return;

				logger.error("Couldn't connect to server", unknownhostexception);
				mc.displayGuiScreen(new GuiDisconnected(previousGuiScreen, "connect.failed", new ChatComponentTranslation("disconnect.genericReason", "Unknown host")));
			}
			catch (final Exception exception)
			{
				if (cancel)
					return;

				logger.error("Couldn't connect to server", exception);
				String s = exception.toString();

				if (inetaddress != null)
				{
					final String s1 = inetaddress + ":" + port;
					s = s.replaceAll(s1, "");
				}

				mc.displayGuiScreen(new GuiDisconnected(previousGuiScreen, "connect.failed", new ChatComponentTranslation("disconnect.genericReason", s)));
			}
		}, "Server Connector #" + CONNECTION_ID.incrementAndGet()).start();
	}

	/**
	 * @author CCBlueX
	 * @reason
	 */
	@Overwrite
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		final ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
		final float middleWidth = scaledResolution.getScaledWidth() / 2.0f;
		final float quarterHeight = scaledResolution.getScaledHeight() / 4.0f;

		drawDefaultBackground();

		RenderUtils.drawLoadingCircle(middleWidth, quarterHeight + 70);

		String ip = "Unknown IP";
		String gameVersion = "Unknown Version";
		String protocolVersion = "Unknown Protocol Version";
		Color color = Color.gray;

		final ServerData serverData = mc.getCurrentServerData();
		if (serverData != null)
		{
			ip = serverData.serverIP + (serverData.serverName.isEmpty() ? "" : "(" + serverData.serverName + ")");
			gameVersion = "Minecraft " + serverData.gameVersion;
			protocolVersion = "NetworkManager v" + serverData.version;
			color = Color.cyan;
		}

		Fonts.font40.drawCenteredString("Connecting to", middleWidth, quarterHeight + 110, 0xFFFFFF, true);
		Fonts.font40.drawCenteredString(ip, middleWidth, quarterHeight + 120, color.getRGB(), true);
		Fonts.font35.drawCenteredString(gameVersion, middleWidth, quarterHeight + 160, color.getRGB(), true);
		Fonts.font35.drawCenteredString(protocolVersion, middleWidth, quarterHeight + 170, color.getRGB(), true);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}
}
