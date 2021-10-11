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
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.EntityImplKt;
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
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.client.C19PacketResourcePackStatus.Action;
import net.minecraft.network.play.server.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook.EnumFlags;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.WorldSettings;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
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
	@Final
	private NetworkManager netManager;

	@Shadow
	private Minecraft gameController;

	@Shadow
	private WorldClient clientWorldController;

	@Shadow
	public int currentServerMaxPlayers;

	@Shadow
	private boolean doneLoadingTerrain;

	@SuppressWarnings(
	{
			"ThrowCaughtLocally", "IfCanBeAssertion"
	})
	@Inject(method = "handleResourcePack", at = @At("HEAD"), cancellable = true)
	private void injectResourcePackFix(final S48PacketResourcePackSend packetResourcePack, final CallbackInfo callbackInfo)
	{
		final String url = packetResourcePack.getURL();
		final String hash = packetResourcePack.getHash();

		try
		{
			final String scheme = new URI(url).getScheme();
			final boolean isLevelProtocol = "level".equalsIgnoreCase(scheme);

			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme) && !isLevelProtocol)
				throw new URISyntaxException(url, "Wrong protocol");

			if (isLevelProtocol && (url.contains("..") || !url.toLowerCase(Locale.ROOT).endsWith("/resources.zip")))
				throw new URISyntaxException(url, "Invalid levelstorage resourcepack path");
		}
		catch (final URISyntaxException e)
		{
			ClientUtils.getLogger().error("Failed to handle resource pack", e);
			netManager.sendPacket(new C19PacketResourcePackStatus(hash, Action.FAILED_DOWNLOAD));
			callbackInfo.cancel();
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Inject(method = "handleJoinGame", at = @At("HEAD"), cancellable = true)
	private void injectAntiModDisable(final S01PacketJoinGame packetIn, final CallbackInfo callbackInfo)
	{
		if (!AntiModDisable.Companion.getEnabled() || !AntiModDisable.Companion.getBlockFMLPackets() || Minecraft.getMinecraft().isIntegratedServerRunning())
			return;

		// noinspection OverlyStrongTypeCast
		PacketThreadUtil.checkThreadAndEnqueue(packetIn, (NetHandlerPlayClient) (Object) this, gameController);

		gameController.playerController = new PlayerControllerMP(gameController, (NetHandlerPlayClient) (Object) this);

		clientWorldController = new WorldClient((NetHandlerPlayClient) (Object) this, new WorldSettings(0L, packetIn.getGameType(), false, packetIn.isHardcoreMode(), packetIn.getWorldType()), packetIn.getDimension(), packetIn.getDifficulty(), gameController.mcProfiler);

		gameController.gameSettings.difficulty = packetIn.getDifficulty();
		gameController.loadWorld(clientWorldController);
		gameController.thePlayer.dimension = packetIn.getDimension();
		gameController.displayGuiScreen(new GuiDownloadTerrain((NetHandlerPlayClient) (Object) this));
		gameController.thePlayer.setEntityId(packetIn.getEntityId());

		currentServerMaxPlayers = packetIn.getMaxPlayers();

		gameController.thePlayer.setReducedDebug(packetIn.isReducedDebugInfo());
		gameController.playerController.setGameType(packetIn.getGameType());
		gameController.gameSettings.sendSettingsToServer();

		netManager.sendPacket(new C17PacketCustomPayload("MC|Brand", new PacketBuffer(Unpooled.buffer()).writeString(ClientBrandRetriever.getClientModName())));

		callbackInfo.cancel();
	}

	@Inject(method = "handleEntityMovement", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/Entity;onGround:Z"))
	private void handleEntityMovementEvent(final S14PacketEntity packetIn, final CallbackInfo callbackInfo)
	{
		final Entity entity = packetIn.getEntity(clientWorldController);

		if (entity != null)
			LiquidBounce.eventManager.callEvent(new EntityMovementEvent(EntityImplKt.wrap(entity)));
	}

	/**
	 * @author eric0210
	 * @reason NoRotateSet, SetBack Alert
	 * @see    NoRotateSet
	 */
	@Inject(method = "handlePlayerPosLook", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift = Shift.AFTER), cancellable = true)
	public void injectNoRotateSet(final S08PacketPlayerPosLook packetIn, final CallbackInfo ci)
	{
		final NoRotateSet noRotateSet = (NoRotateSet) LiquidBounce.moduleManager.get(NoRotateSet.class);

		if (!noRotateSet.getState())
			return;

		final EntityPlayerSP thePlayer = gameController.thePlayer;

		double x = packetIn.getX();
		double y = packetIn.getY();
		double z = packetIn.getZ();
		float yaw = packetIn.getYaw();
		float pitch = packetIn.getPitch();

		final double prevPosX = thePlayer.posX;
		final double prevPosY = thePlayer.posY;
		final double prevPosZ = thePlayer.posZ;

		if (packetIn.func_179834_f().contains(EnumFlags.X))
			x += prevPosX;
		else
			thePlayer.motionX = 0.0D;

		if (packetIn.func_179834_f().contains(EnumFlags.Y))
			y += prevPosY;
		else
			thePlayer.motionY = 0.0D;

		if (packetIn.func_179834_f().contains(EnumFlags.Z))
			z += prevPosZ;
		else
			thePlayer.motionZ = 0.0D;

		final boolean relativePitch = packetIn.func_179834_f().contains(EnumFlags.X_ROT);
		final boolean relativeYaw = packetIn.func_179834_f().contains(EnumFlags.Y_ROT);

		final float prevYaw = thePlayer.rotationYaw;
		final float prevPitch = thePlayer.rotationPitch;

		if (relativePitch)
			pitch += prevPitch;

		if (relativeYaw)
			yaw += prevYaw;

		yaw %= 360.0F;

		if (noRotateSet.getNoZeroValue().get() && !relativeYaw && yaw == 0.0f && !relativePitch && pitch == 0.0f)
		{
			// NoZero
			thePlayer.setPositionAndRotation(x, y, z, 0.0f, 0.0f);
			netManager.sendPacket(new C06PacketPlayerPosLook(thePlayer.posX, thePlayer.getEntityBoundingBox().minY, thePlayer.posZ, prevYaw, thePlayer.rotationPitch, false));
		}
		else
		{
			thePlayer.setPosition(x, y, z);

			// Send (Spoofed) Responce Packet
			netManager.sendPacket(noRotateSet.getConfirmValue().get() && (noRotateSet.getConfirmIllegalRotationValue().get() || pitch >= -90 && pitch <= 90) && (yaw != RotationUtils.serverRotation.getYaw() || pitch != RotationUtils.serverRotation.getPitch()) ? new C06PacketPlayerPosLook(thePlayer.posX, thePlayer.getEntityBoundingBox().minY, thePlayer.posZ, yaw, pitch, false) : new C06PacketPlayerPosLook(thePlayer.posX, thePlayer.getEntityBoundingBox().minY, thePlayer.posZ, prevYaw % 360.0F, thePlayer.rotationPitch, false));
		}

		if (!doneLoadingTerrain)
		{
			thePlayer.prevPosX = thePlayer.posX;
			thePlayer.prevPosY = thePlayer.posY;
			thePlayer.prevPosZ = thePlayer.posZ;
			doneLoadingTerrain = true;
			gameController.displayGuiScreen(null);
		}

		ci.cancel();
	}

	/**
	 * @author Eric0210
	 * @reason Velocity
	 * @see    Velocity
	 */
	@Inject(method = "handleExplosion", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Explosion;doExplosionB(Z)V"), cancellable = true)
	public void injectVelocity(final S27PacketExplosion packetIn, final CallbackInfo ci)
	{
		final EntityPlayerSP thePlayer = gameController.thePlayer;

		if (thePlayer == null)
			return;

		double motionX = packetIn.func_149149_c();
		double motionY = packetIn.func_149144_d();
		double motionZ = packetIn.func_149147_e();

		final Velocity velocity = (Velocity) LiquidBounce.moduleManager.get(Velocity.class);

		if (!velocity.getState())
			return;

		ci.cancel();

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

	/**
	 * @author CCBlueX
	 * @reason Chat Alerts
	 */
	@Inject(method = "handleChat", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/PacketThreadUtil;checkThreadAndEnqueue(Lnet/minecraft/network/Packet;Lnet/minecraft/network/INetHandler;Lnet/minecraft/util/IThreadListener;)V", shift = Shift.AFTER))
	public void injectChatAlerts(final S02PacketChat packetIn, final CallbackInfo ci)
	{
		final IChatComponent messageComponent = packetIn.getChatComponent();

		final String text = messageComponent.getUnformattedText().toLowerCase(Locale.ENGLISH);
		final HUD hud = (HUD) LiquidBounce.moduleManager.get(HUD.class);
		final boolean alerts = hud.getNotificationAlertsValue().get();

		if (alerts)
			AsyncUtilsKt.runAsync(() ->
			{
				if (isHackerChat(text))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "Chat", "Someone called you a hacker.", 2000L);
				else if (text.contains("ground items will be removed in"))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "ClearLag", "ClearLag " + text.substring(text.lastIndexOf("in ")), 2000L);
				else if (text.contains("removed ") && text.contains("entities"))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "ClearLag", text.substring(text.lastIndexOf("removed ")), 2000L);
				else if (text.contains("you are now in "))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "Faction Warning", "Chunk: " + text.substring(text.lastIndexOf("in ") + 3), 2000L);
				else if (text.contains("now entering"))
					LiquidBounce.hud.addNotification(NotificationIcon.WARNING_YELLOW, "Faction", "Chunk: " + text.substring(text.lastIndexOf(": ") + 4), 2000L);
			});
	}

	private static boolean isHackerChat(final String text)
	{
		return HACKER_CHATS.stream().anyMatch(text::contains) && text.contains(Minecraft.getMinecraft().thePlayer.getName().toLowerCase(Locale.ENGLISH)) && HACKER_CHATS_WHITELIST.stream().noneMatch(text::contains);
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
