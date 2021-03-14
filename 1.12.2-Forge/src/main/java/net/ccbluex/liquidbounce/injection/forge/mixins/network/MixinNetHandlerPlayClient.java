/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import java.net.URI;
import java.net.URISyntaxException;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EntityMovementEvent;
import net.ccbluex.liquidbounce.features.special.AntiForge;
import net.ccbluex.liquidbounce.injection.backend.EntityImplKt;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.client.CPacketResourcePackStatus;
import net.minecraft.network.play.client.CPacketResourcePackStatus.Action;
import net.minecraft.network.play.server.SPacketEntity;
import net.minecraft.network.play.server.SPacketJoinGame;
import net.minecraft.network.play.server.SPacketResourcePackSend;
import net.minecraft.world.WorldSettings;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.netty.buffer.Unpooled;

@Mixin(NetHandlerPlayClient.class)
public abstract class MixinNetHandlerPlayClient
{

	@Shadow
	public int currentServerMaxPlayers;
	@Shadow
	@Final
	private NetworkManager netManager;
	@Shadow
	private Minecraft gameController;
	@Shadow
	private WorldClient clientWorldController;

	@Inject(method = "handleResourcePack", at = @At("HEAD"), cancellable = true)
	private void handleResourcePack(final SPacketResourcePackSend p_handleResourcePack_1_, final CallbackInfo callbackInfo)
	{
		final String url = p_handleResourcePack_1_.getURL();
		final String hash = p_handleResourcePack_1_.getHash();

		try
		{
			final String scheme = new URI(url).getScheme();
			final boolean isLevelProtocol = "level".equals(scheme);

			if (!"http".equals(scheme) && !"https".equals(scheme) && !isLevelProtocol)
				throw new URISyntaxException(url, "Wrong protocol");

			if (isLevelProtocol && (url.contains("..") || !url.endsWith("/resources.zip")))
				throw new URISyntaxException(url, "Invalid levelstorage resourcepack path");
		}
		catch (final URISyntaxException e)
		{
			ClientUtils.getLogger().error("Failed to handle resource pack", e);
			netManager.sendPacket(new CPacketResourcePackStatus(Action.FAILED_DOWNLOAD));
			callbackInfo.cancel();
		}
	}

	@Inject(method = "handleJoinGame", at = @At("HEAD"), cancellable = true)
	private void handleJoinGameWithAntiForge(final SPacketJoinGame packetIn, final CallbackInfo callbackInfo)
	{
		if (!AntiForge.enabled || !AntiForge.blockFML || Minecraft.getMinecraft().isIntegratedServerRunning())
			return;

		PacketThreadUtil.checkThreadAndEnqueue(packetIn, (NetHandlerPlayClient) (Object) this, gameController);
		gameController.playerController = new PlayerControllerMP(gameController, (NetHandlerPlayClient) (Object) this);
		clientWorldController = new WorldClient((NetHandlerPlayClient) (Object) this, new WorldSettings(0L, packetIn.getGameType(), false, packetIn.isHardcoreMode(), packetIn.getWorldType()), packetIn.getDimension(), packetIn.getDifficulty(), gameController.mcProfiler);
		gameController.gameSettings.difficulty = packetIn.getDifficulty();
		gameController.loadWorld(clientWorldController);
		gameController.player.dimension = packetIn.getDimension();
		gameController.displayGuiScreen(new GuiDownloadTerrain());
		gameController.player.setEntityId(packetIn.getPlayerId());
		currentServerMaxPlayers = packetIn.getMaxPlayers();
		gameController.player.setReducedDebug(packetIn.isReducedDebugInfo());
		gameController.playerController.setGameType(packetIn.getGameType());
		gameController.gameSettings.sendSettingsToServer();
		netManager.sendPacket(new CPacketCustomPayload("MC|Brand", new PacketBuffer(Unpooled.buffer()).writeString(ClientBrandRetriever.getClientModName())));
		callbackInfo.cancel();
	}

	@Inject(method = "handleEntityMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z"))
	private void handleEntityMovementEvent(final SPacketEntity packetIn, final CallbackInfo callbackInfo)
	{
		final Entity entity = packetIn.getEntity(clientWorldController);

		if (entity != null)
			LiquidBounce.eventManager.callEvent(new EntityMovementEvent(EntityImplKt.wrap(entity)));
	}
}
