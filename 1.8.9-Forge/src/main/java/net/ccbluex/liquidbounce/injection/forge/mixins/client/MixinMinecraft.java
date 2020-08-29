/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoClicker;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AbortBreaking;
import net.ccbluex.liquidbounce.features.module.modules.exploit.MultiActions;
import net.ccbluex.liquidbounce.features.module.modules.world.FastPlace;
import net.ccbluex.liquidbounce.injection.backend.EnumFacingImplKt;
import net.ccbluex.liquidbounce.injection.backend.GuiScreenImplKt;
import net.ccbluex.liquidbounce.injection.backend.WorldClientImplKt;
import net.ccbluex.liquidbounce.injection.backend.WrapperImpl;
import net.ccbluex.liquidbounce.injection.backend.utils.BackendExtentionsKt;
import net.ccbluex.liquidbounce.injection.forge.SplashProgressLock;
import net.ccbluex.liquidbounce.ui.client.GuiMainMenu;
import net.ccbluex.liquidbounce.ui.client.GuiUpdate;
import net.ccbluex.liquidbounce.ui.client.GuiWelcome;
import net.ccbluex.liquidbounce.utils.CPSCounter;
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
import net.minecraft.util.Util;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.Sys;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(Minecraft.class)
@SideOnly(Side.CLIENT)
public abstract class MixinMinecraft {

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

    @Inject(method = "run", at = @At("HEAD"))
    private void init(CallbackInfo callbackInfo) {
        if (displayWidth < 1067)
            displayWidth = 1067;

        if (displayHeight < 622)
            displayHeight = 622;
    }

    @Inject(method = "<init>", at = @At(value = "RETURN"))
    private void injectWrapperInitializator(CallbackInfo ci) {
        // Set Wrapper
        LiquidBounce.wrapper = WrapperImpl.INSTANCE;
    }

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;checkGLError(Ljava/lang/String;)V", ordinal = 2, shift = At.Shift.AFTER))
    private void startGame(CallbackInfo callbackInfo) {
        LiquidBounce.INSTANCE.startClient();
    }

    @Inject(method = "startGame", at = @At(value = "NEW", target = "net/minecraft/client/renderer/texture/TextureManager"))
    private void waitForLock(CallbackInfo ci) {
        long end = System.currentTimeMillis() + 20000;

        while (end < System.currentTimeMillis() && SplashProgressLock.INSTANCE.isAnimationRunning()) {
            synchronized (SplashProgressLock.INSTANCE) {
                try {
                    SplashProgressLock.INSTANCE.wait(10000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Inject(method = "startGame", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;displayGuiScreen(Lnet/minecraft/client/gui/GuiScreen;)V", shift = At.Shift.AFTER))
    private void afterMainScreen(CallbackInfo callbackInfo) {
//        if (!LiquidBounce.scriptManager.getLateInitScripts().isEmpty())
//            LiquidBounce.wrapper.getMinecraft().displayGuiScreen(LiquidBounce.wrapper.getClassProvider().wrapGuiScreen(new GuiUnsignedScripts()));
        if (LiquidBounce.fileManager.firstStart)
            LiquidBounce.wrapper.getMinecraft().displayGuiScreen(LiquidBounce.wrapper.getClassProvider().wrapGuiScreen(new GuiWelcome()));
        else if (LiquidBounce.INSTANCE.getLatestVersion() > LiquidBounce.CLIENT_VERSION - (LiquidBounce.IN_DEV ? 1 : 0))
            LiquidBounce.wrapper.getMinecraft().displayGuiScreen(LiquidBounce.wrapper.getClassProvider().wrapGuiScreen(new GuiUpdate()));
    }

    @Inject(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void createDisplay(CallbackInfo callbackInfo) {
        Display.setTitle(LiquidBounce.CLIENT_NAME + " b" + LiquidBounce.CLIENT_VERSION + " | " + LiquidBounce.MINECRAFT_VERSION + (LiquidBounce.IN_DEV ? " | DEVELOPMENT BUILD" : ""));
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", shift = At.Shift.AFTER))
    private void displayGuiScreen(CallbackInfo callbackInfo) {
        if(currentScreen instanceof net.minecraft.client.gui.GuiMainMenu || (currentScreen != null && currentScreen.getClass().getName().startsWith("net.labymod") && currentScreen.getClass().getSimpleName().equals("ModGuiMainMenu"))) {
            currentScreen = GuiScreenImplKt.unwrap(LiquidBounce.wrapper.getClassProvider().wrapGuiScreen(new GuiMainMenu()));

            ScaledResolution scaledResolution = new ScaledResolution(Minecraft.getMinecraft());
            currentScreen.setWorldAndResolution(Minecraft.getMinecraft(), scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
            skipRenderWorld = false;
        }

        LiquidBounce.eventManager.callEvent(new ScreenEvent(currentScreen == null ? null : GuiScreenImplKt.wrap(currentScreen)));
    }

    private long lastFrame = getTime();

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void runGameLoop(final CallbackInfo callbackInfo) {
        final long currentTime = getTime();
        final int deltaTime = (int) (currentTime - lastFrame);
        lastFrame = currentTime;

        RenderUtils.deltaTime = deltaTime;
    }

    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;joinPlayerCounter:I", shift = At.Shift.BEFORE))
    private void onTick(final CallbackInfo callbackInfo) {
        LiquidBounce.eventManager.callEvent(new TickEvent());
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V", shift = At.Shift.AFTER))
    private void onKey(CallbackInfo callbackInfo) {
        if(Keyboard.getEventKeyState() && currentScreen == null)
            LiquidBounce.eventManager.callEvent(new KeyEvent(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey()));
    }

    @Inject(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovingObjectPosition;getBlockPos()Lnet/minecraft/util/BlockPos;"))
    private void onClickBlock(CallbackInfo callbackInfo) {
        if(this.leftClickCounter == 0 && theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock().getMaterial() != Material.air) {
            LiquidBounce.eventManager.callEvent(new ClickBlockEvent(BackendExtentionsKt.wrap(objectMouseOver.getBlockPos()), EnumFacingImplKt.wrap(this.objectMouseOver.sideHit)));
        }
    }

    @Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
    private void setWindowIcon(CallbackInfo callbackInfo) {
        if(Util.getOSType() != Util.EnumOS.OSX) {
            final ByteBuffer[] liquidBounceFavicon = IconUtils.getFavicon();
            if(liquidBounceFavicon != null) {
                Display.setIcon(liquidBounceFavicon);
                callbackInfo.cancel();
            }
        }
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void shutdown(CallbackInfo callbackInfo) {
        LiquidBounce.INSTANCE.stopClient();
    }

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void clickMouse(CallbackInfo callbackInfo) {
        CPSCounter.registerClick(CPSCounter.MouseButton.LEFT);

        if (LiquidBounce.moduleManager.getModule(AutoClicker.class).getState())
            leftClickCounter = 0;
    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"))
    private void middleClickMouse(CallbackInfo ci) {
        CPSCounter.registerClick(CPSCounter.MouseButton.MIDDLE);
    }

    @Inject(method = "rightClickMouse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelayTimer:I", shift = At.Shift.AFTER))
    private void rightClickMouse(final CallbackInfo callbackInfo) {
        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT);

        final FastPlace fastPlace = (FastPlace) LiquidBounce.moduleManager.getModule(FastPlace.class);

        if (fastPlace.getState())
            rightClickDelayTimer = fastPlace.getSpeedValue().get();
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void loadWorld(WorldClient p_loadWorld_1_, String p_loadWorld_2_, final CallbackInfo callbackInfo) {
        if (theWorld != null) {
            MiniMapRegister.INSTANCE.unloadAllChunks();
        }

        LiquidBounce.eventManager.callEvent(new WorldEvent(p_loadWorld_1_ == null ? null : WorldClientImplKt.wrap(p_loadWorld_1_)));
    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    private void sendClickBlockToController(boolean leftClick) {
        if(!leftClick)
            this.leftClickCounter = 0;

        if (this.leftClickCounter <= 0 && (!this.thePlayer.isUsingItem() || LiquidBounce.moduleManager.getModule(MultiActions.class).getState())) {
            if(leftClick && this.objectMouseOver != null && this.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos blockPos = this.objectMouseOver.getBlockPos();

                if(this.leftClickCounter == 0)
                    LiquidBounce.eventManager.callEvent(new ClickBlockEvent(BackendExtentionsKt.wrap(blockPos), EnumFacingImplKt.wrap(this.objectMouseOver.sideHit)));


                if(this.theWorld.getBlockState(blockPos).getBlock().getMaterial() != Material.air && this.playerController.onPlayerDamageBlock(blockPos, this.objectMouseOver.sideHit)) {
                    this.effectRenderer.addBlockHitEffects(blockPos, this.objectMouseOver.sideHit);
                    this.thePlayer.swingItem();
                }
            } else if (!LiquidBounce.moduleManager.getModule(AbortBreaking.class).getState()) {
                this.playerController.resetBlockRemoving();
            }
        }
    }

    /**
     * @author
     */
    @Overwrite
    public int getLimitFramerate() {
        return this.theWorld == null && this.currentScreen != null ? 60 : this.gameSettings.limitFramerate;
    }
}