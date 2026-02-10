/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.platform.Window;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.CoroutineTicker;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.*;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleAutoClicker;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleNoMissCooldown;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleMultiActions;
import net.ccbluex.liquidbounce.features.module.modules.misc.ModuleMiddleClickAction;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAutoBreak;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoBlockInteract;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureSilentScreen;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.ccbluex.liquidbounce.integration.backend.BrowserBackendManager;
import net.ccbluex.liquidbounce.integration.backend.browser.GlobalBrowserSettings;
import net.ccbluex.liquidbounce.integration.screen.ScreenManager;
import net.ccbluex.liquidbounce.utils.client.vfp.VfpCompatibility;
import net.ccbluex.liquidbounce.utils.combat.CombatManager;
import net.minecraft.SharedConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.Options;
import net.minecraft.client.User;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.client.gui.screens.Overlay;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import static net.ccbluex.liquidbounce.utils.client.ProtocolUtilKt.getUsesViaFabricPlus;

@Mixin(Minecraft.class)
public abstract class MixinMinecraft {

    @Shadow
    @Nullable
    public LocalPlayer player;
    @Shadow
    @Nullable
    public HitResult hitResult;
    @Shadow
    @Final
    public Options options;
    @Shadow
    @Nullable
    private IntegratedServer singleplayerServer;
    @Shadow
    private int rightClickDelay;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;

    @Inject(method = "useAmbientOcclusion()Z", at = @At("HEAD"), cancellable = true)
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
    public abstract ClientPacketListener getConnection();

    @Shadow
    public abstract @org.jetbrains.annotations.Nullable ServerData getCurrentServer();

    @Shadow
    public abstract Window getWindow();

    @Shadow
    public abstract void setScreen(@org.jetbrains.annotations.Nullable Screen screen);

    @Shadow
    public abstract int getFps();

    @Shadow
    public abstract User getUser();

    @Shadow
    @org.jetbrains.annotations.Nullable
    public Screen screen;

    @Shadow
    protected abstract void continueAttack(boolean breaking);

    @Shadow
    private @org.jetbrains.annotations.Nullable Overlay overlay;

    @Shadow
    @org.jetbrains.annotations.Nullable
    public ClientLevel level;

    /**
     * Entry point of our hacked client
     *
     * @param callback not needed
     */
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;resizeDisplay()V"))
    private void startClient(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(ClientStartEvent.INSTANCE);
    }

    /**
     * Exit point of our hacked client
     *
     * @param callback not needed
     */
    @Inject(method = "destroy", at = @At("HEAD"))
    private void stopClient(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(ClientShutdownEvent.INSTANCE);
    }

    @Inject(method = "<init>", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;profileKeyPairManager:Lnet/minecraft/client/multiplayer/ProfileKeyPairManager;",
            ordinal = 0, shift = At.Shift.AFTER))
    private void onSessionInit(CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(new SessionEvent(getUser()));
    }

    /**
     * Modify window title to our client title.
     * Example: LiquidBounce v1.0.0 | 1.16.3
     *
     * @param callback our window title
     *                 <p>
     *                 todo: modify constant Minecraft instead
     */
    @Inject(method = "createTitle", at = @At(
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
                titleBuilder.append(SharedConstants.getCurrentVersion().name());
            }
        } else {
            titleBuilder.append(SharedConstants.getCurrentVersion().name());
        }

        // For debugging purposes, will be removed until we have a stable release
        var backend = BrowserBackendManager.INSTANCE.getBackend();
        if (backend != null && backend.isInitialized() && backend.getAccelerationFlags().isSupported()) {
            var accelerated = GlobalBrowserSettings.INSTANCE.getAccelerated();

            if (accelerated != null && accelerated.get()) {
                titleBuilder.append(" | Accelerated Paint is ON");
                // Hotkey only works when not in-game
                if (this.level == null && this.player == null) {
                    titleBuilder.append(" [Hotkey: F12]");
                }
            }
        }

        ClientPacketListener clientPlayNetworkHandler = this.getConnection();
        if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isConnected()) {
            titleBuilder.append(" - ");
            ServerData serverInfo = this.getCurrentServer();
            if (this.singleplayerServer != null && !this.singleplayerServer.isPublished()) {
                titleBuilder.append(I18n.get("title.singleplayer"));
            } else if (serverInfo != null && serverInfo.isRealm()) {
                titleBuilder.append(I18n.get("title.multiplayer.realms"));
            } else if (this.singleplayerServer == null && (serverInfo == null || !serverInfo.isLan())) {
                titleBuilder.append(I18n.get("title.multiplayer.other"));
            } else {
                titleBuilder.append(I18n.get("title.multiplayer.lan"));
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

    @Redirect(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MouseHandler;releaseMouse()V"))
    private void cancelScreenMouseForChestStealer(MouseHandler instance) {
        // Allows rotation.
        if (!LiquidBounce.INSTANCE.isInitialized() ||
            !FeatureSilentScreen.INSTANCE.getShouldHide() || FeatureSilentScreen.INSTANCE.getUnlockCursor()) {
            instance.releaseMouse();
        }
    }

    /**
     * Hook game tick event at HEAD
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void hookTickEvent(CallbackInfo callbackInfo) {
        CoroutineTicker.INSTANCE.tick();
        EventManager.INSTANCE.callEvent(GameTickEvent.INSTANCE);
    }

    /**
     * Hook game render task queue event
     */
    @Inject(method = "runTick", at = @At("HEAD"))
    private void hookRenderTaskQueue(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(GameRenderTaskQueueEvent.INSTANCE);
    }

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runAllTasks()V", shift = At.Shift.BEFORE))
    private void hookPacketProcess(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(TickPacketProcessEvent.INSTANCE);
    }

    /**
     * Hook input handling
     */
    @Inject(method = "handleKeybinds", at = @At("RETURN"))
    private void hookHandleInputEvent(CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(InputHandleEvent.INSTANCE);
    }

    /**
     * Hook item use cooldown
     */
    @Inject(method = "startUseItem", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;rightClickDelay:I", shift = At.Shift.AFTER))
    private void hookItemUseCooldown(CallbackInfo callbackInfo) {
        UseCooldownEvent useCooldownEvent = new UseCooldownEvent(rightClickDelay);
        EventManager.INSTANCE.callEvent(useCooldownEvent);
        rightClickDelay = useCooldownEvent.getCooldown();
    }

    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void hookItemPick(CallbackInfo ci) {
        if (ModuleMiddleClickAction.Pearl.INSTANCE.cancelPick()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "startAttack",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", ordinal = 0))
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

    @ModifyReceiver(
        method = "startAttack",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/item/component/AttackRange;isInRange(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;)Z"
        )
    )
    private AttackRange injectReachAttackRange(AttackRange instance, LivingEntity entity, Vec3 pos) {
        if (ModuleReach.INSTANCE.getRunning()) {
            return ModuleReach.INSTANCE.getEntity().adjustAttackRange(instance);
        }

        return instance;
    }

    @WrapWithCondition(method = "startAttack", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/Minecraft;missTime:I", ordinal = 1))
    private boolean disableAttackCooldown(Minecraft instance, int value) {
        return !(ModuleNoMissCooldown.INSTANCE.getRunning() && ModuleNoMissCooldown.INSTANCE.getRemoveAttackCooldown());
    }

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void injectCombatPause(CallbackInfoReturnable<Boolean> cir) {
        if (player == null || hitResult == null || hitResult.getType() == HitResult.Type.MISS) {
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

    @Inject(method = "updateLevelInEngines(Lnet/minecraft/client/multiplayer/ClientLevel;Z)V", at = @At("HEAD"))
    private void hookWorldChangeEvent(ClientLevel world, boolean bl, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new WorldChangeEvent(world));
    }

    @Inject(method = "runTick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;fps:I",
            ordinal = 0, shift = At.Shift.AFTER))
    private void hookFpsChange(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new FpsChangeEvent(this.getFps()));
    }

    @Inject(method = "onResourceLoadFinished", at = @At("HEAD"))
    private void onFinishedLoading(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(ResourceReloadEvent.INSTANCE);
    }

    @ModifyExpressionValue(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"))
    private boolean injectMultiActionsBreakingWhileUsing(boolean original) {
        return original && !ModuleMultiActions.mayBreakWhileUsing();
    }

    @ModifyExpressionValue(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;isDestroying()Z"))
    private boolean injectMultiActionsPlacingWhileBreaking(boolean original) {
        return original && !ModuleMultiActions.mayPlaceWhileBreaking();
    }

    /**
     * Alternative input handler of [handleInputEvents] while being inside a client-side screen.
     */
    @Inject(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;screen:Lnet/minecraft/client/gui/screens/Screen;", ordinal = 4, shift = At.Shift.BEFORE), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void passthroughInputHandler(CallbackInfo ci, @Local ProfilerFiller profiler) {
        if (this.overlay == null && this.player != null && this.level
            != null && ScreenManager.isClientScreen(this.screen)) {
            profiler.popPush("Keybindings");

            if (ModuleAutoBreak.INSTANCE.getEnabled()) {
                this.continueAttack(this.options.keyAttack.isDown());
            }
        }
    }

    @ModifyExpressionValue(method = "handleKeybinds", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z", ordinal = 0))
    private boolean injectMultiActionsAttackingWhileUsingAndEnforcedBlockingState(boolean isUsingItem) {
        if (isUsingItem) {
            if (!this.options.keyUse.isDown() && !(KillAuraAutoBlock.INSTANCE.getRunning() && KillAuraAutoBlock.INSTANCE.getBlockingStateEnforced())) {
                this.gameMode.releaseUsingItem(this.player);
            }

            if (!ModuleMultiActions.mayAttackWhileUsing()) {
                this.options.keyAttack.clickCount = 0;
            }

            this.options.keyPickItem.clickCount = 0;
            this.options.keyUse.clickCount = 0;
        }

        return false;
    }

    @WrapWithCondition(method = "tick", at = @At(value = "FIELD", target = "Lnet/minecraft/client/Minecraft;missTime:I", ordinal = 0))
    private boolean injectFixAttackCooldownOnVirtualBrowserScreen(Minecraft instance, int value) {
        // Do not reset attack cooldown when we are in the vr/browser screen, as this poses an
        // unintended modification to the attack cooldown, which is not intended.
        return !ScreenManager.isClientScreen(this.screen);
    }

    @Inject(method = "clearDownloadedResourcePacks", at = @At("HEAD"))
    private void handleDisconnection(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(DisconnectEvent.INSTANCE);
    }

    @Inject(method = "startUseItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;"), cancellable = true)
    private void hookBlockInteract(CallbackInfo ci) {
        final BlockHitResult blockHitResult = (BlockHitResult) this.hitResult;
        if (blockHitResult == null) return; // it should never be null

        if (ModuleNoBlockInteract.INSTANCE.getRunning() &&
                ModuleNoBlockInteract.INSTANCE.shouldSneak(blockHitResult)) {

            ModuleNoBlockInteract.INSTANCE.startSneaking();
            ci.cancel();
        }
    }
}
