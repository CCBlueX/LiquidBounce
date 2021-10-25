/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Locale;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EntityMovementEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity;
import net.ccbluex.liquidbounce.features.module.modules.misc.NoRotateSet;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.features.special.AntiModDisable;
import net.ccbluex.liquidbounce.injection.backend.EntityImplKt;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon;
import net.ccbluex.liquidbounce.utils.AsyncUtilsKt;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.CPacketConfirmTeleport;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.network.play.client.CPacketPlayer.PositionRotation;
import net.minecraft.network.play.client.CPacketResourcePackStatus;
import net.minecraft.network.play.client.CPacketResourcePackStatus.Action;
import net.minecraft.network.play.server.*;
import net.minecraft.network.play.server.SPacketPlayerPosLook.EnumFlags;
import net.minecraft.world.WorldSettings;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
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

	@Shadow
	private boolean doneLoadingTerrain;

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
		if (!AntiModDisable.Companion.getEnabled() || !AntiModDisable.Companion.getBlockFMLPackets() || Minecraft.getMinecraft().isIntegratedServerRunning())
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

	/**
	 * @author Mojang, eric0210
	 * @reason NoRotateSet, SetBack Alert
	 * @see    NoRotateSet
	 */
	@Overwrite
	public void handlePlayerPosLook(final SPacketPlayerPosLook packetIn)
	{
		// noinspection CastToIncompatibleInterface
		PacketThreadUtil.checkThreadAndEnqueue(packetIn, (INetHandlerPlayClient) this, gameController);

		final EntityPlayerSP thePlayer = gameController.player;

		double x = packetIn.getX();
		double y = packetIn.getY();
		double z = packetIn.getZ();
		float yaw = packetIn.getYaw();
		float pitch = packetIn.getPitch();
		final NoRotateSet noRotateSet = (NoRotateSet) LiquidBounce.moduleManager.get(NoRotateSet.class);

		final double prevPosX = thePlayer.posX;
		final double prevPosY = thePlayer.posY;
		final double prevPosZ = thePlayer.posZ;

		if (packetIn.getFlags().contains(EnumFlags.X))
			x += prevPosX;
		else
			thePlayer.motionX = 0.0D;

		if (packetIn.getFlags().contains(EnumFlags.Y))
			y += prevPosY;
		else
			thePlayer.motionY = 0.0D;

		if (packetIn.getFlags().contains(EnumFlags.Z))
			z += prevPosZ;
		else
			thePlayer.motionZ = 0.0D;

		final boolean relativePitch = packetIn.getFlags().contains(EnumFlags.X_ROT);
		final boolean relativeYaw = packetIn.getFlags().contains(EnumFlags.Y_ROT);

		final float prevYaw = thePlayer.rotationYaw;
		final float prevPitch = thePlayer.rotationPitch;

		if (relativePitch)
			pitch += prevPitch;

		if (relativeYaw)
			yaw += prevYaw;

		final float newYaw = yaw % 360.0F;

		if (noRotateSet.getState() && !(noRotateSet.getNoZeroValue().get() && !relativeYaw && yaw == 0.0f && !relativePitch && pitch == 0.0f))
		{
			thePlayer.setPosition(x, y, z);

			// Send (Spoofed) Responce Packet
			netManager.sendPacket(new CPacketConfirmTeleport(packetIn.getTeleportId()));
			netManager.sendPacket(noRotateSet.getConfirmValue().get() && (noRotateSet.getConfirmIllegalRotationValue().get() || pitch >= -90 && pitch <= 90) && (newYaw != RotationUtils.serverRotation.getYaw() || pitch != RotationUtils.serverRotation.getPitch()) ? new PositionRotation(thePlayer.posX, thePlayer.getEntityBoundingBox().minY, thePlayer.posZ, newYaw, pitch % 360.0F, false) : new PositionRotation(thePlayer.posX, thePlayer.getEntityBoundingBox().minY, thePlayer.posZ, prevYaw % 360.0F, thePlayer.rotationPitch % 360.0F, false));
		}
		else
		{
			thePlayer.setPositionAndRotation(x, y, z, yaw, pitch);
			netManager.sendPacket(new PositionRotation(thePlayer.posX, thePlayer.getEntityBoundingBox().minY, thePlayer.posZ, prevYaw, thePlayer.rotationPitch, false));
		}

		if (!doneLoadingTerrain)
		{
			thePlayer.prevPosX = thePlayer.posX;
			thePlayer.prevPosY = thePlayer.posY;
			thePlayer.prevPosZ = thePlayer.posZ;
			doneLoadingTerrain = true;
			gameController.displayGuiScreen(null);
		}
	}

	/**
	 * @author Mojang, Eric0210
	 * @reason Velocity
	 * @see    Velocity
	 */
	@Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Explosion;doExplosionB(Z)V", shift = Shift.AFTER), cancellable = true)
	public void handleExplosion(final SPacketExplosion packetIn, final CallbackInfo ci)
	{
		final EntityPlayerSP thePlayer = gameController.player;

		double motionX = packetIn.getMotionX();
		double motionY = packetIn.getMotionY();
		double motionZ = packetIn.getMotionZ();

		// Hypixel Explosion-packet Velocity //
		if (thePlayer != null)
		{
			final Velocity velocity = (Velocity) LiquidBounce.moduleManager.get(Velocity.class);
			velocity.getVelocityTimer().reset();

			switch (velocity.getModeValue().get().toLowerCase(Locale.ENGLISH))
			{
				case "simple":
					final float horizontal = velocity.getHorizontalValue().get();
					final float vertical = velocity.getVerticalValue().get();

					if (horizontal == 0.0F && vertical == 0.0F)
						return;

					motionX *= horizontal;
					motionY *= vertical;
					motionZ *= horizontal;
					break;
				case "aac":
				case "reverse":
				case "smoothreverse":
				case "aaczero":
					velocity.setVelocityInput(true);
					break;

				case "glitch":
					if (thePlayer.onGround)
					{
						velocity.setVelocityInput(true);
						return;
					}
					break;
			}

			thePlayer.motionX += motionX;
			thePlayer.motionY += motionY;
			thePlayer.motionZ += motionZ;
		}

		ci.cancel();
	}

	/**
	 * @author CCBlueX
	 * @reason Chat Alerts
	 */
	@Inject(method = "handleChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V"))
	public void handleChat(final SPacketChat packetIn, final CallbackInfo ci)
	{
		if (((HUD) LiquidBounce.moduleManager.get(HUD.class)).getNotificationAlertsValue().get())
			AsyncUtilsKt.runAsync(() ->
			{
				final String text = packetIn.getChatComponent().getUnformattedText().toLowerCase(Locale.ENGLISH);

				if (isHackerChat(text))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "Chat", "Someone called you a hacker.", 2000L);

				if (text.contains("ground items will be removed in"))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "ClearLag", "ClearLag " + text.substring(text.lastIndexOf("in ")), 2000L);

				if (text.contains("removed ") && text.contains("entities"))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "ClearLag", text.substring(text.lastIndexOf("removed ")), 2000L);

				if (text.contains("you are now in "))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "Faction Warning", "Chunk: " + text.substring(text.lastIndexOf("in ") + 3), 2000L);

				if (text.contains("now entering"))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "Faction", "Chunk: " + text.substring(text.lastIndexOf(": ") + 4), 2000L);

				return null;
			});
	}

	private static boolean isHackerChat(final String text)
	{
		return HACKER_CHATS.stream().anyMatch(text::contains) && text.contains(Minecraft.getMinecraft().player.getName().toLowerCase(Locale.ENGLISH)) && HACKER_CHATS_WHITELIST.stream().noneMatch(text::contains);
	}

	private static final Collection<String> HACKER_CHATS = new ArrayDeque<>(20);
	private static final Collection<String> HACKER_CHATS_WHITELIST = new ArrayDeque<>(3);

	static
	{
		HACKER_CHATS.add("hack");
		HACKER_CHATS.add("h@ck");
		HACKER_CHATS.add("h4ck");
		HACKER_CHATS.add("hax");
		HACKER_CHATS.add("h@x");
		HACKER_CHATS.add("h4x");
		HACKER_CHATS.add("hkr");
		HACKER_CHATS.add("cheat");
		HACKER_CHATS.add("che@t");
		HACKER_CHATS.add("hqr");
		HACKER_CHATS.add("haq");

		HACKER_CHATS.add("fly");
		HACKER_CHATS.add("aura");
		HACKER_CHATS.add("reach");
		HACKER_CHATS.add("antikb");
		HACKER_CHATS.add("antiknock");

		HACKER_CHATS.add("mod");
		HACKER_CHATS.add("ban");
		HACKER_CHATS.add("report");
		HACKER_CHATS.add("record");
		HACKER_CHATS.add("ticket");

		HACKER_CHATS_WHITELIST.add("ncp");
		HACKER_CHATS_WHITELIST.add("aac");
		HACKER_CHATS_WHITELIST.add("anticheat");
	}
}
