/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.common.GlobalFramebuffer;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoClicker;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleNoMissCooldown;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions;
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleMiddleClickAction;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoBreak;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.ccbluex.liquidbounce.integration.IntegrationListener;
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager;
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings;
import net.ccbluex.liquidbounce.render.engine.RenderingFlags;
import net.ccbluex.liquidbounce.utils.client.vfp.VfpCompatibility;
import net.ccbluex.liquidbounce.utils.combat.CombatManager;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.session.Session;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Util;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.profiler.Profiler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.annotation.Nullable;

import static net.ccbluex.liquidbounce.utils.client.ProtocolUtilKt.getUsesViaFabricPlus;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Shadow
    @Nullable
    public ClientPlayerEntity player;
    @Shadow
    @Nullable
    public HitResult crosshairTarget;
    @Shadow
    @Final
    public GameOptions options;
    @Shadow
    @Nullable
    private IntegratedServer server;
    @Shadow
    private int itemUseCooldown;
    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;

    @Inject(method = "isAmbientOcclusionEnabled()Z", at = @At("HEAD"), cancellable = true)
    private static void injectXRayFullBright(CallbackInfoReturnable<Boolean> callback) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning() || !module.getFullBright()) {
            return;
        }

        callback.setReturnValue(false);
        callback.cancel();
    }

    @Shadow
    @Nullable
    public abstract ClientPlayNetworkHandler getNetworkHandler();

    @Shadow
    public abstract @org.jetbrains.annotations.Nullable ServerInfo getCurrentServerEntry();

    @Shadow
    public abstract Window getWindow();

    @Shadow
    public abstract void setScreen(@org.jetbrains.annotations.Nullable Screen screen);

    @Shadow
    public abstract int getCurrentFps();

    @Shadow
    public abstract Session getSession();

    @Shadow
    @org.jetbrains.annotations.Nullable
    public Screen currentScreen;

    @Shadow
    protected abstract void handleBlockBreaking(boolean breaking);

    @Shadow
    private @org.jetbrains.annotations.Nullable Overlay overlay;

    @Shadow
    @org.jetbrains.annotations.Nullable
    public ClientWorld world;

    /**
     * Entry point of our hacked client
     *
     * @param callback not needed
     */
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;onResolutionChanged()V"))
    private void startClient(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(ClientStartEvent.INSTANCE);
    }

    /**
     * Exit point of our hacked client
     *
     * @param callback not needed
     */
    @Inject(method = "stop", at = @At("HEAD"))
    private void stopClient(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(ClientShutdownEvent.INSTANCE);
    }

    @Inject(method = "<init>", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;profileKeys:Lnet/minecraft/client/session/ProfileKeys;",
            ordinal = 0, shift = At.Shift.AFTER))
    private void onSessionInit(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(new SessionEvent(getSession()));
    }

    /**
     * Modify window title to our client title.
     * Example: LiquidBounce v1.0.0 | 1.16.3
     *
     * @param callback our window title
     *                 <p>
     *                 todo: modify constant Minecraft instead
     */
    @Inject(method = "getWindowTitle", at = @At(
            value = "INVOKE",
            target = "Ljava/lang/StringBuilder;append(Ljava/lang/String;)Ljava/lang/StringBuilder;",
            ordinal = 1),
            cancellable = true)
    private void getClientTitle(CallbackInfoReturnable<String> callback) {
        if (HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        LiquidBounce.INSTANCE.getLogger().debug("Modifying window title");

        StringBuilder titleBuilder = new StringBuilder(LiquidBounce.CLIENT_NAME);
        titleBuilder.append(" v");
        titleBuilder.append(LiquidBounce.INSTANCE.getClientVersion());
        titleBuilder.append(" ");

        if (LiquidBounce.IN_DEVELOPMENT) {
            titleBuilder.append("(dev) ");
        }

        titleBuilder.append(LiquidBounce.INSTANCE.getClientCommit());

        titleBuilder.append(" | ");

        // ViaFabricPlus compatibility
        if (getUsesViaFabricPlus()) {
            var protocolVersion = VfpCompatibility.INSTANCE.unsafeGetProtocolVersion();

            if (protocolVersion != null) {
                titleBuilder.append(protocolVersion.getName());
            } else {
                titleBuilder.append(SharedConstants.getGameVersion().getName());
            }
        } else {
            titleBuilder.append(SharedConstants.getGameVersion().getName());
        }

        // For debugging purposes, will be removed until we have a stable release
        if (Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS) {
            if (BrowserBackendManager.INSTANCE.getBrowserBackend().isInitialized() &&
                    BrowserBackendManager.INSTANCE.getBrowserBackend().isAccelerationSupported()) {
                var accelerated = GlobalBrowserSettings.INSTANCE.getAccelerated();

                if (accelerated != null && accelerated.get()) {
                    titleBuilder.append(" | (UI Renderer Acceleration is ON");
                    // Hotkey only works when not in-game
                    if (this.world == null && this.player == null) {
                        titleBuilder.append(" - Toggle with F12");
                    }
                    titleBuilder.append(")");
                }
            }
        }

        ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
        if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
            titleBuilder.append(" - ");
            ServerInfo serverInfo = this.getCurrentServerEntry();
            if (this.server != null && !this.server.isRemote()) {
                titleBuilder.append(I18n.translate("title.singleplayer"));
            } else if (serverInfo != null && serverInfo.isRealm()) {
                titleBuilder.append(I18n.translate("title.multiplayer.realms"));
            } else if (this.server == null && (serverInfo == null || !serverInfo.isLocal())) {
                titleBuilder.append(I18n.translate("title.multiplayer.other"));
            } else {
                titleBuilder.append(I18n.translate("title.multiplayer.lan"));
            }
        }

        callback.setReturnValue(titleBuilder.toString());
    }

    /**
     * Fixes recursive screen opening,
     * this is usually caused by another mod such as Lunar Client.
     * Can also happen when opening a screen during [ScreenEvent].
     */
    @Unique
    private boolean recursiveScreenOpening = false;

    /**
     * Handle opening screens
     *
     * @param screen       to be opened (null = no screen at all)
     * @param callbackInfo callback
     */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void hookScreen(Screen screen, CallbackInfo callbackInfo) {
        if (recursiveScreenOpening) {
            return;
        }

        try {
            recursiveScreenOpening = true;

            var event = EventManager.INSTANCE.callEvent(new ScreenEvent(screen));
            if (event.isCancelled()) {
                callbackInfo.cancel();
            }
        } finally {
            recursiveScreenOpening = false;
        }

        // Who need this GUI?
        if (screen instanceof AccessibilityOnboardingScreen) {
            callbackInfo.cancel();
            this.setScreen(new TitleScreen(true));
        }
    }

    /**
     * Hook game tick event at HEAD
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void hookTickEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(GameTickEvent.INSTANCE);
    }

    /**
     * Hook game render task queue event
     */
    @Inject(method = "render", at = @At("HEAD"))
    private void hookRenderTaskQueue(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(GameRenderTaskQueueEvent.INSTANCE);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;runTasks()V", shift = At.Shift.BEFORE))
    private void hookPacketProcess(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(TickPacketProcessEvent.INSTANCE);
    }

    /**
     * Hook input handling
     */
    @Inject(method = "handleInputEvents", at = @At("RETURN"))
    private void hookHandleInputEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(InputHandleEvent.INSTANCE);
    }

    /**
     * Hook item use cooldown
     */
    @Inject(method = "doItemUse", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;itemUseCooldown:I", shift = At.Shift.AFTER))
    private void hookItemUseCooldown(CallbackInfo callbackInfo) {
        UseCooldownEvent useCooldownEvent = new UseCooldownEvent(itemUseCooldown);
        EventManager.INSTANCE.callEvent(useCooldownEvent);
        itemUseCooldown = useCooldownEvent.getCooldown();
    }

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void hookItemPick(CallbackInfo ci) {
        if (ModuleMiddleClickAction.Pearl.INSTANCE.cancelPick()) {
            ci.cancel();
        }
    }

    @Inject(method = "hasOutline", cancellable = true, at = @At("HEAD"))
    private void injectOutlineESPFix(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (RenderingFlags.isCurrentlyRenderingEntityOutline().get()) {
            cir.setReturnValue(true);
        }
    }

    @ModifyExpressionValue(method = "doAttack",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;attackCooldown:I", ordinal = 0))
    private int injectNoMissCooldown(int original) {
        if (ModuleNoMissCooldown.INSTANCE.getRunning() && ModuleNoMissCooldown.INSTANCE.getRemoveAttackCooldown()) {
            return 0;
        }

        if (ModuleAutoClicker.AttackButton.INSTANCE.getRunning()) {
            var clickAmount = ModuleAutoClicker.AttackButton.INSTANCE.getClicker().getClickAmount();
            if (clickAmount != null && clickAmount > 0) {
                return 0;
            }
        }

        return original;
    }

    @WrapWithCondition(method = "doAttack", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/MinecraftClient;attackCooldown:I", ordinal = 1))
    private boolean disableAttackCooldown(MinecraftClient instance, int value) {
        return !(ModuleNoMissCooldown.INSTANCE.getRunning() && ModuleNoMissCooldown.INSTANCE.getRemoveAttackCooldown());
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void injectCombatPause(CallbackInfoReturnable<Boolean> cir) {
        if (player == null || crosshairTarget == null || crosshairTarget.getType() == HitResult.Type.MISS) {
            if (ModuleNoMissCooldown.INSTANCE.getRunning() && ModuleNoMissCooldown.INSTANCE.getCancelAttackOnMiss()) {
                // Prevent swinging
                cir.setReturnValue(true);
            }
            return;
        }

        if (CombatManager.INSTANCE.getShouldPauseCombat()) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setWorld", at = @At("HEAD"))
    private void hookWorldChangeEvent(ClientWorld world, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new WorldChangeEvent(world));
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentFps:I",
            ordinal = 0, shift = At.Shift.AFTER))
    private void hookFpsChange(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new FpsChangeEvent(this.getCurrentFps()));
    }

    @Inject(method = "onFinishedLoading", at = @At("HEAD"))
    private void onFinishedLoading(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(ResourceReloadEvent.INSTANCE);
    }

    @ModifyExpressionValue(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean injectMultiActionsBreakingWhileUsing(boolean original) {
        return original && !ModuleMultiActions.mayBreakWhileUsing();
    }

    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    private boolean injectMultiActionsPlacingWhileBreaking(boolean original) {
        return original && !ModuleMultiActions.mayPlaceWhileBreaking();
    }

    /**
     * Alternative input handler of [handleInputEvents] while being inside a client-side screen.
     * @param ci
     */
    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentScreen:Lnet/minecraft/client/gui/screen/Screen;", ordinal = 4, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void passthroughInputHandler(CallbackInfo ci, @Local Profiler profiler) {
        if (this.overlay == null && this.player != null && this.world != null && IntegrationListener.isClientScreen(this.currentScreen)) {
            profiler.swap("Keybindings");

            if (ModuleAutoBreak.INSTANCE.getEnabled()) {
                this.handleBlockBreaking(this.options.attackKey.isPressed());
            }
        }
    }

    @ModifyExpressionValue(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0))
    private boolean injectMultiActionsAttackingWhileUsingAndEnforcedBlockingState(boolean isUsingItem) {
        if (isUsingItem) {
            if (!this.options.useKey.isPressed() && !(KillAuraAutoBlock.INSTANCE.getRunning() && KillAuraAutoBlock.INSTANCE.getBlockingStateEnforced())) {
                this.interactionManager.stopUsingItem(this.player);
            }

            if (!ModuleMultiActions.mayAttackWhileUsing()) {
                this.options.attackKey.timesPressed = 0;
            }

            this.options.pickItemKey.timesPressed = 0;
            this.options.useKey.timesPressed = 0;
        }

        return false;
    }

    @WrapWithCondition(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;attackCooldown:I", ordinal = 0))
    private boolean injectFixAttackCooldownOnVirtualBrowserScreen(MinecraftClient instance, int value) {
        // Do not reset attack cooldown when we are in the vr/browser screen, as this poses an
        // unintended modification to the attack cooldown, which is not intended.
        return !IntegrationListener.isClientScreen(this.currentScreen);
    }

    @Inject(method = "getFramebuffer", at = @At("HEAD"), cancellable = true)
    private void hookSpoofFramebuffer(CallbackInfoReturnable<Framebuffer> cir) {
        var framebuffer = GlobalFramebuffer.getSpoofedFramebuffer();
        if (framebuffer != null) {
            cir.setReturnValue(framebuffer);
        }
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"))
    private void handleDisconnection(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(DisconnectEvent.INSTANCE);
    }

}
