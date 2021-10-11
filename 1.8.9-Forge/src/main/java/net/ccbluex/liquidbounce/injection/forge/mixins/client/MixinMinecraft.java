/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import static net.ccbluex.liquidbounce.LiquidBounce.wrapper;

import java.nio.ByteBuffer;
import java.util.Optional;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.IClassProvider;
import net.ccbluex.liquidbounce.api.minecraft.client.IMinecraft;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoClicker;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AbortBreaking;
import net.ccbluex.liquidbounce.features.module.modules.exploit.MultiActions;
import net.ccbluex.liquidbounce.features.module.modules.render.HUD;
import net.ccbluex.liquidbounce.features.module.modules.world.FastPlace;
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl;
import net.ccbluex.liquidbounce.injection.backend.minecraft.client.gui.GuiScreenImplKt;
import net.ccbluex.liquidbounce.injection.backend.minecraft.client.multiplayer.WorldClientImplKt;
import net.ccbluex.liquidbounce.injection.backend.minecraft.util.EnumFacingImplKt;
import net.ccbluex.liquidbounce.injection.backend.utils.BackendExtensionKt;
import net.ccbluex.liquidbounce.injection.forge.SplashProgressLock;
import net.ccbluex.liquidbounce.ui.client.GuiMainMenu;
import net.ccbluex.liquidbounce.ui.client.GuiUpdate;
import net.ccbluex.liquidbounce.ui.client.GuiWelcome;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotificationIcon;
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notifications;
import net.ccbluex.liquidbounce.utils.CPSCounter;
import net.ccbluex.liquidbounce.utils.CPSCounter.MouseButton;
import net.ccbluex.liquidbounce.utils.render.IconUtils;
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Util;
import net.minecraft.util.Util.EnumOS;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
@SideOnly(Side.CLIENT)
public abstract class MixinMinecraft
{

	@Shadow
	public GuiScreen currentScreen;

	@Shadow
	public boolean skipRenderWorld;

	@Shadow
	private int leftClickCounter;

	@Shadow
	public MovingObjectPosition objectMouseOver;

	@Shadow
	public WorldClient theWorld;

	@Shadow
	public EntityPlayerSP thePlayer;

	@Shadow
	public EffectRenderer effectRenderer;

	@Shadow
	public PlayerControllerMP playerController;

	@Shadow
	public int displayWidth;

	@Shadow
	public int displayHeight;

	@Shadow
	public int rightClickDelayTimer;

	@Shadow
	public GameSettings gameSettings;

	private long lastFrame = getTime();

	public long getTime()
	{
		return Sys.getTime() * 1000 / Sys.getTimerResolution();
	}

	@Inject(method = "run", at = @At("HEAD"))
	private void resizeWindow(final CallbackInfo callbackInfo)
	{
		if (displayWidth < 1067)
			displayWidth = 1067;

		if (displayHeight < 622)
			displayHeight = 622;
	}

	@Inject(method = "<init>", at = @At("RETURN"))
	private void injectWrapperInitializator(final CallbackInfo ci)
	{
		// Set Wrapper
		wrapper = WrapperImpl.INSTANCE;
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V", ordinal = 2, shift = Shift.AFTER))
	private void injectLiquidBounceEntryPoint(final CallbackInfo callbackInfo)
	{
		LiquidBounce.INSTANCE.startClient();
	}

	@Inject(method = "startGame", at = @At(value = "NEW", target = "net/minecraft/client/renderer/texture/TextureManager"))
	private void waitForLock(final CallbackInfo ci)
	{
		final long end = System.currentTimeMillis() + 20000;

		while (end < System.currentTimeMillis() && SplashProgressLock.INSTANCE.isAnimationRunning())
			synchronized (SplashProgressLock.INSTANCE)
			{
				try
				{
					SplashProgressLock.INSTANCE.wait(10000);
				}
				catch (final InterruptedException e)
				{
					e.printStackTrace();
				}
			}
	}

	@Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", shift = Shift.AFTER))
	private void afterMainScreen(final CallbackInfo callbackInfo)
	{
		final IMinecraft mc = wrapper.getMinecraft();
		final IClassProvider provider = wrapper.getClassProvider();

		// Display welcome screen
		if (LiquidBounce.fileManager.firstStart)
			mc.displayGuiScreen(provider.wrapGuiScreen(new GuiWelcome()));

		// Display update screen
		else if (LiquidBounce.INSTANCE.getLatestVersion() > LiquidBounce.CLIENT_VERSION - (LiquidBounce.IN_DEV ? 1 : 0))
			mc.displayGuiScreen(provider.wrapGuiScreen(new GuiUpdate()));
	}

	@Inject(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", shift = Shift.AFTER))
	private void injectLiquidBounceTitle(final CallbackInfo callbackInfo)
	{
		// Set the window title
		Display.setTitle(LiquidBounce.getTitle());
	}

	@Inject(method = "displayGuiScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", shift = Shift.AFTER))
	private void handleScreenEvent(final CallbackInfo callbackInfo)
	{
		// Replace Main Menu screen
		if (currentScreen instanceof net.minecraft.client.gui.GuiMainMenu || currentScreen != null && currentScreen.getClass().getName().startsWith("net.labymod") && "ModGuiMainMenu".equals(currentScreen.getClass().getSimpleName()))
		{
			currentScreen = GuiScreenImplKt.unwrap(wrapper.getClassProvider().wrapGuiScreen(new GuiMainMenu()));

			final ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
			currentScreen.setWorldAndResolution(Minecraft.getMinecraft(), scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
			skipRenderWorld = false;
		}

		LiquidBounce.eventManager.callEvent(new ScreenEvent(Optional.ofNullable(currentScreen).map(GuiScreenImplKt::wrap).orElse(null)));
	}

	@Inject(method = "runGameLoop", at = @At("HEAD"))
	private void injectFrameTimeCalculation(final CallbackInfo callbackInfo)
	{
		final long currentTime = getTime();
		final int frameTime = (int) (currentTime - lastFrame);
		lastFrame = currentTime;

		RenderUtils.setFrameTime(frameTime);
	}

	@Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;joinPlayerCounter:I", shift = Shift.BEFORE))
	private void handleTickEvent(final CallbackInfo callbackInfo)
	{
		LiquidBounce.eventManager.callEvent(new TickEvent(), true);
	}

	@Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V", shift = Shift.AFTER))
	private void handleKeyEvent(final CallbackInfo callbackInfo)
	{
		if (Keyboard.getEventKeyState() && currentScreen == null)
			LiquidBounce.eventManager.callEvent(new KeyEvent(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey()), true);
	}

	@Inject(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovingObjectPosition;getBlockPos()Lnet/minecraft/util/BlockPos;"))
	private void handleClickBlockEvent(final CallbackInfo callbackInfo)
	{
		if (leftClickCounter == 0 && theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock().getMaterial() != Material.air)
			LiquidBounce.eventManager.callEvent(new ClickBlockEvent(BackendExtensionKt.wrap(objectMouseOver.getBlockPos()), EnumFacingImplKt.wrap(objectMouseOver.sideHit)));
	}

	@Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
	private void injectLiquidBounceFavicon(final CallbackInfo callbackInfo)
	{
		// Replace window icon
		if (Util.getOSType() != EnumOS.OSX)
		{
			final ByteBuffer[] liquidBounceFavicon = IconUtils.getFavicon();
			if (liquidBounceFavicon != null)
			{
				Display.setIcon(liquidBounceFavicon);
				callbackInfo.cancel();
			}
		}
	}

	@Inject(method = "shutdown", at = @At("HEAD"))
	private void injectLiquidBounceExitPoint(final CallbackInfo callbackInfo)
	{
		LiquidBounce.INSTANCE.stopClient();
	}

	@Inject(method = "clickMouse", at = @At("HEAD"))
	private void injectCPSCounterLeft(final CallbackInfo callbackInfo)
	{
		CPSCounter.registerClick(MouseButton.LEFT);

		if (LiquidBounce.moduleManager.get(AutoClicker.class).getState())
			leftClickCounter = 0;
	}

	@Inject(method = "middleClickMouse", at = @At("HEAD"))
	private void injectCPSCounterMiddle(final CallbackInfo ci)
	{
		CPSCounter.registerClick(MouseButton.MIDDLE);
	}

	@Inject(method = "rightClickMouse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelayTimer:I", shift = Shift.AFTER))
	private void injectCPSCounterRight(final CallbackInfo callbackInfo)
	{
		CPSCounter.registerClick(MouseButton.RIGHT);

		final FastPlace fastPlace = (FastPlace) LiquidBounce.moduleManager.get(FastPlace.class);

		if (fastPlace.getState())
			rightClickDelayTimer = fastPlace.getSpeedValue().get();
	}

	@Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
	private void handleWorldEvent(final WorldClient world, final String loadingMessage, final CallbackInfo callbackInfo)
	{
		if (theWorld != null)
			MiniMapRegister.INSTANCE.unloadAllChunks();

		// Trigger WorldEvent
		LiquidBounce.eventManager.callEvent(new WorldEvent(Optional.ofNullable(world).map(WorldClientImplKt::wrap).orElse(null)));

		final HUD hud = (HUD) LiquidBounce.moduleManager.get(HUD.class);

		if (hud.getNotificationWorldChangeValue().get() && LiquidBounce.hud.getNotifications().size() <= Notifications.Companion.getMaxRendered().get())
			LiquidBounce.hud.addNotification(NotificationIcon.INFORMATION, "World Change", "(" + theWorld + ") -> (" + world + ")", 2000L);
	}

	/**
	 * @author CCBlueX
	 * @reason MultiActions
	 */
	@Redirect(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/EntityPlayerSP;isUsingItem()Z"))
	private boolean injectMultiActions(final EntityPlayerSP thePlayer)
	{
		return thePlayer.isUsingItem() && !LiquidBounce.moduleManager.get(MultiActions.class).getState();
	}

	/**
	 * @author CCBlueX
	 * @reason ClickBlockEvent
	 */
	@ModifyVariable(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/WorldClient;getBlockState(Lnet/minecraft/util/BlockPos;)Lnet/minecraft/block/state/IBlockState;", shift = Shift.BEFORE), require = 1, ordinal = 0)
	private BlockPos handleClickBlockEvent(final BlockPos blockpos)
	{
		if (leftClickCounter == 0)
			LiquidBounce.eventManager.callEvent(new ClickBlockEvent(BackendExtensionKt.wrap(blockpos), EnumFacingImplKt.wrap(objectMouseOver.sideHit)));

		return blockpos;
	}

	/**
	 * @author CCBlueX
	 * @reason AbortBreaking
	 */
	@Inject(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/PlayerControllerMP;resetBlockRemoving()V", shift = Shift.BEFORE), cancellable = true)
	private void injectAbortBreaking(final CallbackInfo ci)
	{
		if (LiquidBounce.moduleManager.get(AbortBreaking.class).getState())
			ci.cancel();
	}

	/**
	 * @author CCBlueX
	 * @reason Limit framerate to 60 if any kind of gui is open
	 */
	@ModifyConstant(method = "getLimitFramerate", constant = @Constant(intValue = 30, ordinal = 0), require = 1)
	public int vsyncFrameTo60(final int prevFrame)
	{
		return 60;
	}
}
