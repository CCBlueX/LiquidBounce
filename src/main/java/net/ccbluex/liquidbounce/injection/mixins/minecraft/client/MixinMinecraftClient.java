/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.AutoBlock;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.ccbluex.liquidbounce.render.engine.RenderingFlags;
import net.ccbluex.liquidbounce.utils.combat.CombatManager;
import net.ccbluex.liquidbounce.web.integration.BrowserScreen;
import net.ccbluex.liquidbounce.web.integration.VrScreen;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.AccessibilityOnboardingScreen;
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
import net.minecraft.util.hit.HitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

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
        if (!module.getEnabled() || !module.getFullBright()) {
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

    /**
     * Entry point of our hacked client
     *
     * @param callback not needed
     */
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;onResolutionChanged()V"))
    private void startClient(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(new ClientStartEvent());
    }

    /**
     * Exit point of our hacked client
     *
     * @param callback not needed
     */
    @Inject(method = "stop", at = @At("HEAD"))
    private void stopClient(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(new ClientShutdownEvent());
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
        titleBuilder.append(SharedConstants.getGameVersion().getName());

        ClientPlayNetworkHandler clientPlayNetworkHandler = this.getNetworkHandler();
        if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
            titleBuilder.append(" - ");
            ServerInfo serverInfo = this.getCurrentServerEntry();
            if (this.server != null && !this.server.isRemote()) {
                titleBuilder.append(I18n.translate("title.singleplayer", new Object[0]));
            } else if (serverInfo != null && serverInfo.isRealm()) {
                titleBuilder.append(I18n.translate("title.multiplayer.realms", new Object[0]));
            } else if (this.server == null && (serverInfo == null || !serverInfo.isLocal())) {
                titleBuilder.append(I18n.translate("title.multiplayer.other", new Object[0]));
            } else {
                titleBuilder.append(I18n.translate("title.multiplayer.lan", new Object[0]));
            }
        }

        callback.setReturnValue(titleBuilder.toString());
    }

    /**
     * Handle opening screens
     *
     * @param screen       to be opened (null = no screen at all)
     * @param callbackInfo callback
     */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void hookScreen(Screen screen, CallbackInfo callbackInfo) {
        ScreenEvent event = new ScreenEvent(screen);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) callbackInfo.cancel();
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
        EventManager.INSTANCE.callEvent(new GameTickEvent());
    }

    /**
     * Hook input handling
     */
    @Inject(method = "handleInputEvents", at = @At("RETURN"))
    private void hookHandleInputEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new InputHandleEvent());
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

    @Inject(method = "hasOutline", cancellable = true, at = @At("HEAD"))
    private void injectOutlineESPFix(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (RenderingFlags.isCurrentlyRenderingEntityOutline().get()) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "doAttack", at = @At("HEAD"), cancellable = true)
    private void injectCombatPause(CallbackInfoReturnable<Boolean> cir) {
        if (player == null || crosshairTarget == null) {
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

    /**
     * Removes frame rate limit
     */
    @ModifyConstant(method = "getFramerateLimit", constant = @Constant(intValue = 60))
    private int getFramerateLimit(int original) {
        return getWindow().getFramerateLimit();
    }

    @Inject(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/MinecraftClient;currentFps:I",
            ordinal = 0, shift = At.Shift.AFTER))
    private void hookFpsChange(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new FpsChangeEvent(this.getCurrentFps()));
    }

    @Inject(method = "onFinishedLoading", at = @At("HEAD"))
    private void onFinishedLoading(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new ResourceReloadEvent());
    }

    @ModifyExpressionValue(method = "handleBlockBreaking", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z"))
    private boolean injectMultiActionsBreakingWhileUsing(boolean original) {
        return original && !(ModuleMultiActions.INSTANCE.handleEvents() && ModuleMultiActions.INSTANCE.getBreakingWhileUsing());
    }

    @ModifyExpressionValue(method = "doItemUse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerInteractionManager;isBreakingBlock()Z"))
    private boolean injectMultiActionsPlacingWhileBreaking(boolean original) {
        return original && !(ModuleMultiActions.INSTANCE.handleEvents() && ModuleMultiActions.INSTANCE.getPlacingWhileBreaking());
    }

    @ModifyExpressionValue(method = "handleInputEvents", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;isUsingItem()Z", ordinal = 0))
    private boolean injectMultiActionsAttackingWhileUsingAndEnforcedBlockingState(boolean isUsingItem) {
        if (isUsingItem) {
            if (!this.options.useKey.isPressed() && !(ModuleKillAura.INSTANCE.getEnabled()
                    && AutoBlock.INSTANCE.getEnabled() && AutoBlock.INSTANCE.getBlockingStateEnforced())) {
                this.interactionManager.stopUsingItem(this.player);
            }

            if (!ModuleMultiActions.INSTANCE.handleEvents() || !ModuleMultiActions.INSTANCE.getAttackingWhileUsing()) {
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
        return !(this.currentScreen instanceof BrowserScreen || this.currentScreen instanceof VrScreen);
    }

}
