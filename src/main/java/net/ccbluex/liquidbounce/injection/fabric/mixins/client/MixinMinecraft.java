/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.fabric.mixins.client;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.ClientUpdate;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoClicker;
import net.ccbluex.liquidbounce.features.module.modules.exploit.AbortBreaking;
import net.ccbluex.liquidbounce.features.module.modules.exploit.MultiActions;
import net.ccbluex.liquidbounce.features.module.modules.world.FastPlace;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.injection.fabric.SplashProgressLock;
import net.ccbluex.liquidbounce.ui.client.GuiClientConfiguration;
import net.ccbluex.liquidbounce.ui.client.GuiMainMenu;
import net.ccbluex.liquidbounce.ui.client.GuiUpdate;
import net.ccbluex.liquidbounce.ui.client.GuiWelcome;
import net.ccbluex.liquidbounce.utils.CPSCounter;
import net.ccbluex.liquidbounce.utils.ClientUtils;
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
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.item.BlockItem;
import net.minecraft.util.math.BlockPos;
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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

import static net.ccbluex.liquidbounce.utils.MinecraftInstance.mc;

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

    @Shadow
    public abstract void displayGuiScreen(GuiScreen guiScreenIn);

    @Inject(method = "run", at = @At("HEAD"))
    private void init(CallbackInfo callbackInfo) {
        if (displayWidth < 1067) displayWidth = 1067;

        if (displayHeight < 622) displayHeight = 622;
    }

    @Inject(method = "startGame", at = @At("TAIL"))
    private void example$onStartGame(CallbackInfo ci) {
         System.out.println("Hello, Fabric!");
    }

    @Inject(method = "runGameLoop", at = @At(value = "INVOKE", target = "Lnet/minecraft/profiler/Profiler;startSection(Ljava/lang/String;)V", ordinal = 1))
    private void hook(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new GameLoopEvent());
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
        if (FileManager.INSTANCE.getFirstStart()) {
            displayGuiScreen(new GuiWelcome());
        } else if (ClientUpdate.INSTANCE.hasUpdate()) {
            displayGuiScreen(new GuiUpdate());
        }
    }

    @Inject(method = "createDisplay", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/Display;setTitle(Ljava/lang/String;)V", shift = At.Shift.AFTER))
    private void createDisplay(CallbackInfo callbackInfo) {
        if (GuiClientConfiguration.Companion.getEnabledClientTitle()) {
            Display.setTitle(LiquidBounce.INSTANCE.getClientTitle());
        }
    }

    @Inject(method = "displayGuiScreen", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;currentScreen:Lnet/minecraft/client/gui/GuiScreen;", shift = At.Shift.AFTER))
    private void handleDisplayGuiScreen(CallbackInfo callbackInfo) {
        if (currentScreen instanceof net.minecraft.client.gui.GuiMainMenu || (currentScreen != null && currentScreen.getClass().getName().startsWith("net.labymod") && currentScreen.getClass().getSimpleName().equals("ModGuiMainMenu"))) {
            currentScreen = new GuiMainMenu();

            ScaledResolution scaledResolution = new ScaledResolution(mc);
            currentScreen.setWorldAndResolution(mc, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight());
            skipRenderWorld = false;
        }

        EventManager.INSTANCE.callEvent(new ScreenEvent(currentScreen));
    }

    private long lastFrame = getTime();

    @Inject(method = "runGameLoop", at = @At("HEAD"))
    private void runGameLoop(final CallbackInfo callbackInfo) {
        final long currentTime = getTime();
        final int deltaTime = (int) (currentTime - lastFrame);
        lastFrame = currentTime;

        RenderUtils.INSTANCE.setDeltaTime(deltaTime);
    }

    public long getTime() {
        return (Sys.getTime() * 1000) / Sys.getTimerResolution();
    }

    @Inject(method = "runTick", at = @At("HEAD"))
    private void injectGameRuntimeTicks(CallbackInfo ci) {
        ClientUtils.INSTANCE.setRunTimeTicks(ClientUtils.INSTANCE.getRunTimeTicks() + 1);
    }

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;joinPlayerCounter:I", ordinal = 0))
    private void onTick(final CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new GameTickEvent());
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;dispatchKeypresses()V", shift = At.Shift.AFTER))
    private void onKey(CallbackInfo callbackInfo) {
        if (Keyboard.getEventKeyState() && currentScreen == null)
            EventManager.INSTANCE.callEvent(new KeyEvent(Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey()));
    }

    @Inject(method = "sendClickBlockToController", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/MovingObjectPosition;getBlockPos()Lnet/minecraft/util/BlockPos;"))
    private void onClickBlock(CallbackInfo callbackInfo) {
        if (leftClickCounter == 0 && theWorld.getBlockState(objectMouseOver.getBlockPos()).getBlock().getMaterial() != Material.air) {
            EventManager.INSTANCE.callEvent(new ClickBlockEvent(objectMouseOver.getBlockPos(), objectMouseOver.sideHit));
        }
    }

    @Inject(method = "setWindowIcon", at = @At("HEAD"), cancellable = true)
    private void setWindowIcon(CallbackInfo callbackInfo) {
        if (Util.getOSType() != Util.EnumOS.OSX) {
            if (GuiClientConfiguration.Companion.getEnabledClientTitle()) {
                final ByteBuffer[] liquidBounceFavicon = IconUtils.INSTANCE.getFavicon();
                if (liquidBounceFavicon != null) {
                    Display.setIcon(liquidBounceFavicon);
                    callbackInfo.cancel();
                }
            }
        }
    }

    @Inject(method = "shutdown", at = @At("HEAD"))
    private void shutdown(CallbackInfo callbackInfo) {
        LiquidBounce.INSTANCE.stopClient();
    }

    @Inject(method = "clickMouse", at = @At("HEAD"))
    private void clickMouse(CallbackInfo callbackInfo) {
        if (AutoClicker.INSTANCE.handleEvents()) {
            leftClickCounter = 0;
        }

        if (leftClickCounter <= 0) {
            CPSCounter.INSTANCE.registerClick(CPSCounter.MouseButton.LEFT);
        }
    }

    @Inject(method = "middleClickMouse", at = @At("HEAD"))
    private void middleClickMouse(CallbackInfo ci) {
        CPSCounter.INSTANCE.registerClick(CPSCounter.MouseButton.MIDDLE);
    }

    @Inject(method = "rightClickMouse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelayTimer:I", shift = At.Shift.AFTER))
    private void rightClickMouse(final CallbackInfo callbackInfo) {
        CPSCounter.INSTANCE.registerClick(CPSCounter.MouseButton.RIGHT);

        final FastPlace fastPlace = FastPlace.INSTANCE;
        if (!fastPlace.handleEvents()) return;

        // Don't spam-click when the player isn't holding blocks
        if (fastPlace.getOnlyBlocks() && (thePlayer.getHeldItem() == null || !(thePlayer.getHeldItem().getItem() instanceof BlockItem)))
            return;

        if (objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            BlockPos blockPos = objectMouseOver.getBlockPos();
            IBlockState blockState = theWorld.getBlockState(blockPos);
            // Don't spam-click when interacting with a TileEntity (chests, ...)
            // Doesn't prevent spam-clicking anvils, crafting tables, ... (couldn't figure out a non-hacky way)
            if (blockState.getBlock().hasTileEntity(blockState)) return;
            // Return if not facing a block
        } else if (fastPlace.getFacingBlocks()) return;

        rightClickDelayTimer = fastPlace.getSpeed();
    }

    @Inject(method = "loadWorld(Lnet/minecraft/client/multiplayer/WorldClient;Ljava/lang/String;)V", at = @At("HEAD"))
    private void loadWorld(WorldClient p_loadWorld_1_, String p_loadWorld_2_, final CallbackInfo callbackInfo) {
        if (theWorld != null) {
            MiniMapRegister.INSTANCE.unloadAllChunks();
        }

        EventManager.INSTANCE.callEvent(new WorldEvent(p_loadWorld_1_));
    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    public void sendClickBlockToController(boolean leftClick) {
        if (!leftClick) leftClickCounter = 0;

        if (leftClickCounter <= 0 && (!thePlayer.isUsingItem() || MultiActions.INSTANCE.handleEvents())) {
            if (leftClick && objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                BlockPos blockPos = objectMouseOver.getBlockPos();

                if (leftClickCounter == 0)
                    EventManager.INSTANCE.callEvent(new ClickBlockEvent(blockPos, objectMouseOver.sideHit));


                if (theWorld.getBlockState(blockPos).getBlock().getMaterial() != Material.air && playerController.onPlayerDamageBlock(blockPos, objectMouseOver.sideHit)) {
                    effectRenderer.addBlockHitEffects(blockPos, objectMouseOver.sideHit);
                    thePlayer.swingItem();
                }
            } else if (!AbortBreaking.INSTANCE.handleEvents()) {
                playerController.resetBlockRemoving();
            }
        }
    }

    /**
     * @author CCBlueX
     */
    @ModifyConstant(method = "getLimitFramerate", constant = @Constant(intValue = 30))
    public int getLimitFramerate(int constant) {
        return 60;
    }
}