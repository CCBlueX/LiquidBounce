/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import java.nio.ByteBuffer;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoClicker;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AbortBreaking;
import net.ccbluex.liquidbounce.features.module.modules.exploit.MultiActions;
import net.ccbluex.liquidbounce.features.module.modules.world.FastPlace;
import net.ccbluex.liquidbounce.injection.backend.EnumFacingImplKt;
import net.ccbluex.liquidbounce.injection.backend.GuiScreenImplKt;
import net.ccbluex.liquidbounce.injection.backend.WorldClientImplKt;
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl;
import net.ccbluex.liquidbounce.injection.backend.utils.BackendExtentionsKt;
import net.ccbluex.liquidbounce.ui.client.GuiMainMenu;
import net.ccbluex.liquidbounce.ui.client.GuiUpdate;
import net.ccbluex.liquidbounce.ui.client.GuiWelcome;
import net.ccbluex.liquidbounce.utils.CPSCounter;
import net.ccbluex.liquidbounce.utils.CPSCounter.MouseButton;
import net.ccbluex.liquidbounce.utils.render.IconUtils;
import net.ccbluex.liquidbounce.utils.render.MiniMapRegister;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.util.EnumHand;
import net.minecraft.util.Util;
import net.minecraft.util.Util.EnumOS;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.RayTraceResult.Type;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
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
	public RayTraceResult objectMouseOver;
	@Shadow
	public WorldClient world;
	@Shadow
	public EntityPlayerSP player;
	@Shadow
	public ParticleManager effectRenderer;
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
	@Shadow
	private int leftClickCounter;
	private long lastFrame = getTime();

	@Inject(method = "run", at = @At("HEAD"))
	private void init(final CallbackInfo callbackInfo)
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
		LiquidBounce.wrapper = WrapperImpl.INSTANCE;
	}

	@Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V", ordinal = 2, shift = Shift.AFTER))
	private void startGame(final CallbackInfo callbackInfo)
	{
		LiquidBounce.INSTANCE.startClient();
	}

	@Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", shift = Shift.AFTER))
	private void afterMainScreen(final CallbackInfo callbackInfo)
	{
		if (LiquidBounce.fileManager.firstStart)
			LiquidBounce.wrapper.getMinecraft().displayGuiScreen(LiquidBounce.wrapper.getClassProvider().wrapGuiScreen(new GuiWelcome()));
		else if (LiquidBounce.INSTANCE.getLatestVersion() > LiquidBounce.CLIENT_VERSION - (LiquidBounce.IN_DEV ? 1 : 0))
			LiquidBounce.wrapper.getMinecraft().displayGuiScreen(LiquidBounce.wrapper.getClassProvider().wrapGuiScreen(new GuiUpdate()));
	}

	@Inject(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", shift = Shift.AFTER))
	private void createDisplay(final CallbackInfo callbackInfo)
	{
		Display.setTitle(LiquidBounce.CLIENT_NAME + " b" + LiquidBounce.CLIENT_VERSION + " | " + LiquidBounce.MINECRAFT_VERSION + (LiquidBounce.IN_DEV ? " | DEVELOPMENT BUILD" : ""));
	}

	@Inject(method = "displayGuiScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", shift = Shift.AFTER))
	private void displayGuiScreen(final CallbackInfo callbackInfo)
	{
		if (currentScreen instanceof net.minecraft.client.gui.GuiMainMenu || currentScreen != null && currentScreen.getClass().getName().startsWith("net.labymod") && currentScreen.getClass().getSimpleName().equals("ModGuiMainMenu"))
		{
			currentScreen = GuiScreenImplKt.unwrap(LiquidBounce.wrapper.getClassProvider().wrapGuiScreen(new GuiMainMenu()));

			final ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
			currentScreen.setWorldAndResolution(Minecraft.getMinecraft(), scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
			skipRenderWorld = false;
		}

		LiquidBounce.eventManager.callEvent(new ScreenEvent(currentScreen == null ? null : GuiScreenImplKt.wrap(currentScreen)));
	}

	@Inject(method = "runGameLoop", at = @At("HEAD"))
	private void runGameLoop(final CallbackInfo callbackInfo)
	{
		final long currentTime = getTime();
		final int deltaTime = (int) (currentTime - lastFrame);
		lastFrame = currentTime;

		RenderUtils.deltaTime = deltaTime;
	}

	public long getTime()
	{
		return Sys.getTime() * 1000 / Sys.getTimerResolution();
	}

	@Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;joinPlayerCounter:I", shift = Shift.BEFORE))
	private void onTick(final CallbackInfo callbackInfo)
	{
		LiquidBounce.eventManager.callEvent(new TickEvent());
	}

	@Inject(method = "runTickKeyboard", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V", shift = Shift.AFTER))
	private void onKey(final CallbackInfo callbackInfo)
	{
		if (Keyboard.getEventKeyState() && currentScreen == null)
			LiquidBounce.eventManager.callEvent(new KeyEvent(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey()));
	}

	@Inject(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/RayTraceResult;getBlockPos()Lnet/minecraft/util/math/BlockPos;"))
	private void onClickBlock(final CallbackInfo callbackInfo)
	{
		final IBlockState blockState = world.getBlockState(objectMouseOver.getBlockPos());

		if (leftClickCounter == 0 && blockState.getBlock().getMaterial(blockState) != Material.AIR)
		{
			LiquidBounce.eventManager.callEvent(new ClickBlockEvent(BackendExtentionsKt.wrap(objectMouseOver.getBlockPos()), EnumFacingImplKt.wrap(objectMouseOver.sideHit)));
		}
	}

	@Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
	private void setWindowIcon(final CallbackInfo callbackInfo)
	{
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
	private void shutdown(final CallbackInfo callbackInfo)
	{
		LiquidBounce.INSTANCE.stopClient();
	}

	@Inject(method = "clickMouse", at = @At("HEAD"))
	private void clickMouse(final CallbackInfo callbackInfo)
	{
		CPSCounter.registerClick(MouseButton.LEFT);

		if (LiquidBounce.moduleManager.get(AutoClicker.class).getState())
			leftClickCounter = 0;
	}

	@Inject(method = "middleClickMouse", at = @At("HEAD"))
	private void middleClickMouse(final CallbackInfo ci)
	{
		CPSCounter.registerClick(MouseButton.MIDDLE);
	}

	@Inject(method = "rightClickMouse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelayTimer:I", shift = Shift.AFTER))
	private void rightClickMouse(final CallbackInfo callbackInfo)
	{
		CPSCounter.registerClick(MouseButton.RIGHT);

		final FastPlace fastPlace = (FastPlace) LiquidBounce.moduleManager.get(FastPlace.class);

		if (fastPlace.getState())
			rightClickDelayTimer = fastPlace.getSpeedValue().get();
	}

	@Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
	private void loadWorld(final WorldClient p_loadWorld_1_, final String p_loadWorld_2_, final CallbackInfo callbackInfo)
	{
		if (world != null)
		{
			MiniMapRegister.INSTANCE.unloadAllChunks();
		}

		LiquidBounce.eventManager.callEvent(new WorldEvent(p_loadWorld_1_ == null ? null : WorldClientImplKt.wrap(p_loadWorld_1_)));
	}

	/**
	 * @author CCBlueX
	 */
	@Overwrite
	private void sendClickBlockToController(final boolean leftClick)
	{
		if (!leftClick)
			leftClickCounter = 0;

		if (leftClickCounter <= 0 && (!player.isHandActive() || LiquidBounce.moduleManager.get(MultiActions.class).getState()))
		{
			if (leftClick && objectMouseOver != null && objectMouseOver.typeOfHit == Type.BLOCK)
			{
				final BlockPos blockPos = objectMouseOver.getBlockPos();

				if (leftClickCounter == 0)
					LiquidBounce.eventManager.callEvent(new ClickBlockEvent(BackendExtentionsKt.wrap(blockPos), EnumFacingImplKt.wrap(objectMouseOver.sideHit)));

				final IBlockState bs = world.getBlockState(blockPos);

				if (bs.getBlock().getMaterial(bs) != Material.AIR && playerController.onPlayerDamageBlock(blockPos, objectMouseOver.sideHit))
				{
					effectRenderer.addBlockHitEffects(blockPos, objectMouseOver.sideHit);
					player.swingArm(EnumHand.MAIN_HAND);
				}
			}
			else if (!LiquidBounce.moduleManager.get(AbortBreaking.class).getState())
			{
				playerController.resetBlockRemoving();
			}
		}
	}

	/**
	 * @author
	 */
	@Overwrite
	public int getLimitFramerate()
	{
		return world == null && currentScreen != null ? 60 : gameSettings.limitFramerate;
	}
}
