/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.network;

import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayDeque;
import java.util.Collection;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EntityMovementEvent;
import net.ccbluex.liquidbounce.features.module.modules.combat.Velocity;
import net.ccbluex.liquidbounce.features.module.modules.misc.NoRotateSet;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.features.special.AntiModDisable;
import net.ccbluex.liquidbounce.injection.backend.EntityImplKt;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.RotationUtils;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook;
import net.minecraft.network.play.client.C17PacketCustomPayload;
import net.minecraft.network.play.client.C19PacketResourcePackStatus;
import net.minecraft.network.play.client.C19PacketResourcePackStatus.Action;
import net.minecraft.network.play.server.*;
import net.minecraft.network.play.server.S08PacketPlayerPosLook.EnumFlags;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.Explosion;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.event.ForgeEventFactory;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
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

	@Inject(method = "handleResourcePack", at = @At("HEAD"), cancellable = true)
	private void handleResourcePack(final S48PacketResourcePackSend p_handleResourcePack_1_, final CallbackInfo callbackInfo)
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
			netManager.sendPacket(new C19PacketResourcePackStatus(hash, Action.FAILED_DOWNLOAD));
			callbackInfo.cancel();
		}
	}

	@SuppressWarnings("ConstantConditions")
	@Inject(method = "handleJoinGame", at = @At("HEAD"), cancellable = true)
	private void handleJoinGameWithAntiForge(final S01PacketJoinGame packetIn, final CallbackInfo callbackInfo)
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
	@Overwrite
	public void handlePlayerPosLook(final S08PacketPlayerPosLook packetIn)
	{
		PacketThreadUtil.checkThreadAndEnqueue(packetIn, (INetHandlerPlayClient) this, gameController);
		final EntityPlayer entityplayer = gameController.thePlayer;

		double x = packetIn.getX();
		double y = packetIn.getY();
		double z = packetIn.getZ();
		float yaw = packetIn.getYaw();
		float pitch = packetIn.getPitch();
		final NoRotateSet noRotateSet = (NoRotateSet) LiquidBounce.moduleManager.get(NoRotateSet.class);

		if (packetIn.func_179834_f().contains(EnumFlags.X))
			x += entityplayer.posX;
		else
			entityplayer.motionX = 0.0D;

		if (packetIn.func_179834_f().contains(EnumFlags.Y))
			y += entityplayer.posY;
		else
			entityplayer.motionY = 0.0D;

		if (packetIn.func_179834_f().contains(EnumFlags.Z))
			z += entityplayer.posZ;
		else
			entityplayer.motionZ = 0.0D;

		final boolean relativePitch = packetIn.func_179834_f().contains(EnumFlags.X_ROT);
		final boolean relativeYaw = packetIn.func_179834_f().contains(EnumFlags.Y_ROT);

		if (relativePitch)
			pitch += entityplayer.rotationPitch;
		if (relativeYaw)
			yaw += entityplayer.rotationYaw;

		LiquidBounce.hud.clearNotifications();
		LiquidBounce.hud.addNotification("Movement Check", "Set-back detected.", Color.yellow, 500L);

		if (noRotateSet.getState() && !(noRotateSet.getNoZeroValue().get() && !relativeYaw && yaw == 0.0f && !relativePitch && pitch == 0.0f))
		{
			entityplayer.setPosition(x, y, z);
			// Send (Spoofed) Responce Packet
			netManager.sendPacket(noRotateSet.getConfirmValue().get() && (noRotateSet.getConfirmIllegalRotationValue().get() || pitch >= -90 && pitch <= 90) && (RotationUtils.serverRotation == null || yaw % 360.0F != RotationUtils.serverRotation.getYaw() || pitch != RotationUtils.serverRotation.getPitch()) ? new C06PacketPlayerPosLook(entityplayer.posX, entityplayer.getEntityBoundingBox().minY, entityplayer.posZ, yaw % 360.0F, pitch % 360.0F, false) : new C06PacketPlayerPosLook(entityplayer.posX, entityplayer.getEntityBoundingBox().minY, entityplayer.posZ, entityplayer.rotationYaw % 360.0F, entityplayer.rotationPitch % 360.0F, false));
		}
		else
		{
			entityplayer.setPositionAndRotation(x, y, z, yaw, pitch);
			netManager.sendPacket(new C06PacketPlayerPosLook(entityplayer.posX, entityplayer.getEntityBoundingBox().minY, entityplayer.posZ, entityplayer.rotationYaw, entityplayer.rotationPitch, false));
		}

		if (!doneLoadingTerrain)
		{
			gameController.thePlayer.prevPosX = gameController.thePlayer.posX;
			gameController.thePlayer.prevPosY = gameController.thePlayer.posY;
			gameController.thePlayer.prevPosZ = gameController.thePlayer.posZ;
			doneLoadingTerrain = true;
			gameController.displayGuiScreen(null);
		}
	}

	/**
	 * @author Eric0210
	 * @reason Velocity
	 * @see    Velocity
	 */
	@Overwrite
	public void handleExplosion(final S27PacketExplosion packetIn)
	{
		PacketThreadUtil.checkThreadAndEnqueue(packetIn, (INetHandlerPlayClient) this, gameController);
		final Explosion explosion = new Explosion(gameController.theWorld, null, packetIn.getX(), packetIn.getY(), packetIn.getZ(), packetIn.getStrength(), packetIn.getAffectedBlockPositions());
		explosion.doExplosionB(true);
		final Minecraft mc = Minecraft.getMinecraft();
		double motionX = packetIn.func_149149_c();
		double motionY = packetIn.func_149144_d();
		double motionZ = packetIn.func_149147_e();

		// Hypixel Explosion-packet Velocity //
		if (mc.thePlayer != null)
		{
			final Velocity velocity = (Velocity) LiquidBounce.moduleManager.get(Velocity.class);
			velocity.getVelocityTimer().reset();

			switch (velocity.getModeValue().get().toLowerCase())
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
					if (mc.thePlayer.onGround)
					{
						velocity.setVelocityInput(true);
						return;
					}
					break;
			}
		}

		gameController.thePlayer.motionX += motionX;
		gameController.thePlayer.motionY += motionY;
		gameController.thePlayer.motionZ += motionZ;
	}

	/**
	 * @author CCBlueX
	 * @reason Chat Alerts
	 */
	@Overwrite
	public void handleChat(final S02PacketChat packetIn)
	{
		PacketThreadUtil.checkThreadAndEnqueue(packetIn, (INetHandlerPlayClient) this, gameController);

		final String text = packetIn.getChatComponent().getUnformattedText().toLowerCase();
		final HUD hud = (HUD) LiquidBounce.moduleManager.get(HUD.class);
		final boolean alerts = hud.getAlertsValue().get();
		if (alerts)
		{
			if (isHackerChat(text))
				LiquidBounce.hud.addNotification("Player Warning", "Someone called you a hacker.", Color.yellow, 500L);

			if (text.contains("ground items will be removed in"))
			{
				final String message = text.substring(text.lastIndexOf("in "));
				LiquidBounce.hud.addNotification("Clearlag Warning", "Clearlag " + message, null, 500L);
			}

			if (text.contains("removed ") && text.contains("entities"))
			{
				final String message = text.substring(text.lastIndexOf("removed "));
				LiquidBounce.hud.addNotification("Clearlag Removal", message, null, 500L);
			}

			if (text.contains("you are now in "))
			{
				final String message = text.substring(text.lastIndexOf("in ") + 3);
				LiquidBounce.hud.addNotification("Faction Warning", "Chunk: " + message, null, 500L);
			}

			if (text.contains("now entering"))
			{
				final String message = text.substring(text.lastIndexOf(": ") + 4);
				LiquidBounce.hud.addNotification("Faction Warning", "Chunk: " + message, null, 500L);
			}
		}

		final IChatComponent message = ForgeEventFactory.onClientChat(packetIn.getType(), packetIn.getChatComponent());
		if (message == null)
			return;

		if (packetIn.getType() == 2)
			gameController.ingameGUI.setRecordPlaying(message, false);
		else
			gameController.ingameGUI.getChatGUI().printChatMessage(message);
	}

	private boolean isHackerChat(final String text)
	{
		return hackerChats.parallelStream().anyMatch(text::contains) && text.contains(Minecraft.getMinecraft().thePlayer.getName().toLowerCase()) && hackerChatWhitelists.parallelStream().noneMatch(text::contains);
	}

	private static final Collection<String> hackerChats = new ArrayDeque<>(20);
	private static final Collection<String> hackerChatWhitelists = new ArrayDeque<>(3);

	static
	{
		hackerChats.add("hack");
		hackerChats.add("h@ck");
		hackerChats.add("h4ck");
		hackerChats.add("hax");
		hackerChats.add("h@x");
		hackerChats.add("h4x");
		hackerChats.add("hkr");
		hackerChats.add("cheat");
		hackerChats.add("che@t");
		hackerChats.add("hqr");
		hackerChats.add("haq");

		hackerChats.add("fly");
		hackerChats.add("aura");
		hackerChats.add("reach");
		hackerChats.add("antikb");
		hackerChats.add("antiknock");

		hackerChats.add("mod");
		hackerChats.add("ban");
		hackerChats.add("report");
		hackerChats.add("record");
		hackerChats.add("ticket");

		hackerChatWhitelists.add("ncp");
		hackerChatWhitelists.add("aac");
		hackerChatWhitelists.add("anticheat");
	}
}
