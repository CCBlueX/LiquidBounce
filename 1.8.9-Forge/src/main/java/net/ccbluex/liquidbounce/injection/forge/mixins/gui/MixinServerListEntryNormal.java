package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.FMLClientHandler;

import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerListEntryNormal.class)
public abstract class MixinServerListEntryNormal
{
	/**
	 * The default server pinger executor pool has only 5-thread support. Improved to use all available threads.
	 */
	private static final ExecutorService enhancedServerPinger = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new ThreadFactoryBuilder().setNameFormat("Server Pinger #%d").setDaemon(true).build());

	@Shadow
	@Final
	private static Logger logger;

	@Shadow
	@Final
	private static ResourceLocation UNKNOWN_SERVER;

	@Shadow
	@Final
	private static ResourceLocation SERVER_SELECTION_BUTTONS;

	@Shadow
	@Final
	private GuiMultiplayer owner;

	@Shadow
	@Final
	private Minecraft mc;

	@Shadow
	@Final
	private ServerData server;

	@Shadow
	@Final
	private ResourceLocation serverIcon;

	@Shadow
	private String field_148299_g;

	@Shadow
	private DynamicTexture field_148305_h;

	@Shadow
	protected abstract void drawTextureAt(int x, int y, ResourceLocation resource);

	@Shadow
	private void prepareServerIcon()
	{

	}

	@Shadow
	private boolean func_178013_b()
	{
		return false;
	}

	/**
	 * @author eric0210
	 * @reason
	 */
	@Overwrite
	public void drawEntry(final int slotIndex, final int x, final int y, final int listWidth, final int slotHeight, final int mouseX, final int mouseY, final boolean isSelected)
	{
		if (!server.field_78841_f)
		{
			server.field_78841_f = true;
			server.pingToServer = -2L;
			server.serverMOTD = "";
			server.populationInfo = "";
			enhancedServerPinger.submit(() ->
			{
				try
				{
					owner.getOldServerPinger().ping(server);
				}
				catch (final Exception e)
				{
					server.pingToServer = -1L;
					server.serverMOTD = EnumChatFormatting.DARK_RED + e.toString();
					// noinspection StringConcatenationArgumentToLogCall
					logger.warn("Can't connect to the server " + server.serverIP + " (" + server.serverName + ")", e);
				}
			});
		}

		final boolean outdatedClient = server.version > 47;
		final boolean outdatedServer = server.version < 47;
		mc.fontRendererObj.drawString(server.serverName, x + 32 + 3, y + 1, 16777215);
		final List<String> list = mc.fontRendererObj.listFormattedStringToWidth(FMLClientHandler.instance().fixDescription(server.serverMOTD), listWidth - 48 - 2);

		for (int i = 0, j = Math.min(list.size(), 2); i < j; ++i)
			mc.fontRendererObj.drawString(list.get(i), x + 32 + 3, y + 12 + mc.fontRendererObj.FONT_HEIGHT * i, 8421504);

		final boolean incompatibleVersion = outdatedClient || outdatedServer;
		final String populationInfo = incompatibleVersion ? EnumChatFormatting.DARK_RED + server.gameVersion : server.populationInfo;
		final int j = mc.fontRendererObj.getStringWidth(populationInfo);
		mc.fontRendererObj.drawString(populationInfo, x + listWidth - j - 15 - 2, y + 1, 8421504);

		int pinging = 0;
		String playerList = null;
		int pingLevelImageID;
		final String pingHoverText;
		if (incompatibleVersion)
		{
			pingLevelImageID = 5;
			pingHoverText = outdatedClient ? "Client out of date!" : "Server out of date!";
			playerList = server.playerList;
		}
		else if (server.field_78841_f && server.pingToServer != -2L)
		{
			if (server.pingToServer < 0L)
				pingLevelImageID = 5;
			else if (server.pingToServer < 150L)
				pingLevelImageID = 0;
			else if (server.pingToServer < 300L)
				pingLevelImageID = 1;
			else if (server.pingToServer < 600L)
				pingLevelImageID = 2;
			else
				pingLevelImageID = server.pingToServer < 1000L ? 3 : 4;

			if (server.pingToServer < 0L)
				pingHoverText = "(no connection)";
			else
			{
				pingHoverText = server.pingToServer + "ms";
				playerList = server.playerList;
			}
		}
		else
		{
			pinging = 1;
			pingLevelImageID = (int) (Minecraft.getSystemTime() / 100L + (slotIndex << 1) & 7L);
			if (pingLevelImageID > 4)
				pingLevelImageID = 8 - pingLevelImageID;

			pingHoverText = "Pinging...";
		}

		// Draw Ping level
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		mc.getTextureManager().bindTexture(Gui.icons);
		Gui.drawModalRectWithCustomSizedTexture(x + listWidth - 15, y, pinging * 10, 176 + (pingLevelImageID << 3), 10, 8, 256.0F, 256.0F);

		// Prepair Server Icon
		if (server.getBase64EncodedIconData() != null && !server.getBase64EncodedIconData().equals(field_148299_g))
		{
			field_148299_g = server.getBase64EncodedIconData();
			prepareServerIcon();
			owner.getServerList().saveServerList();
		}

		// Draw Server Icon
		drawTextureAt(x, y, field_148305_h == null ? UNKNOWN_SERVER : serverIcon);

		final int i1 = mouseX - x;
		final int j1 = mouseY - y;

		// noinspection ConstantConditions
		final String tooltip = FMLClientHandler.instance().enhanceServerListEntry((ServerListEntryNormal) (Object) this, server, x, listWidth, y, i1, j1);
		if (tooltip != null)
			owner.setHoveringText(tooltip);
		else if (i1 >= listWidth - 15 && i1 <= listWidth - 5 && j1 >= 0 && j1 <= 8)
			owner.setHoveringText(pingHoverText);
		else if (i1 >= listWidth - j - 15 - 2 && i1 <= listWidth - 15 - 2 && j1 >= 0 && j1 <= 8)
			owner.setHoveringText(playerList);

		if (mc.gameSettings.touchscreen || isSelected)
		{
			mc.getTextureManager().bindTexture(SERVER_SELECTION_BUTTONS);
			Gui.drawRect(x, y, x + 32, y + 32, -1601138544);
			GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
			final int k1 = mouseX - x;
			if (func_178013_b())
				Gui.drawModalRectWithCustomSizedTexture(x, y, 0.0F, k1 < 32 && k1 > 16 ? 32.0F : 0.0F, 32, 32, 256.0F, 256.0F);

			final int l1 = mouseY - y;
			// noinspection ConstantConditions
			if (owner.func_175392_a((ServerListEntryNormal) (Object) this, slotIndex))
				Gui.drawModalRectWithCustomSizedTexture(x, y, 96.0F, k1 < 16 && l1 < 16 ? 32.0F : 0.0F, 32, 32, 256.0F, 256.0F);

			// noinspection ConstantConditions
			if (owner.func_175394_b((ServerListEntryNormal) (Object) this, slotIndex))
				Gui.drawModalRectWithCustomSizedTexture(x, y, 64.0F, k1 < 16 && l1 > 16 ? 32.0F : 0.0F, 32, 32, 256.0F, 256.0F);
		}

	}
}
