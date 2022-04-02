/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.common.RenderingFlags;
import net.ccbluex.liquidbounce.event.*;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.client.util.Window;
import net.minecraft.entity.Entity;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.io.InputStream;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient {

    @Shadow
    @Nullable
    public abstract ClientPlayNetworkHandler getNetworkHandler();

    @Shadow
    @Nullable
    private IntegratedServer server;

    @Shadow
    public abstract boolean isConnectedToRealms();

    @Shadow
    @Nullable
    private ServerInfo currentServerEntry;

    @Shadow
    private int itemUseCooldown;

    @Inject(method = "isAmbientOcclusionEnabled()Z", at = @At("HEAD"), cancellable = true)
    private static void injectXRayFullBright(CallbackInfoReturnable<Boolean> callback) {
        ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getEnabled() || !module.getFullBright()) {
            return;
        }

        callback.setReturnValue(false);
        callback.cancel();
    }

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

    /**
     * Modify window title to our client title.
     * Example: LiquidBounce v1.0.0 | 1.16.3
     *
     * @param callback our window title
     */
    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getClientTitle(CallbackInfoReturnable<String> callback) {
        LiquidBounce.INSTANCE.getLogger().debug("Modifying window title");

        final StringBuilder titleBuilder = new StringBuilder(LiquidBounce.CLIENT_NAME);
        titleBuilder.append(" v");
        titleBuilder.append(LiquidBounce.CLIENT_VERSION);
        titleBuilder.append(" | ");
        titleBuilder.append(SharedConstants.getGameVersion().getName());

        final ClientPlayNetworkHandler clientPlayNetworkHandler = getNetworkHandler();
        if (clientPlayNetworkHandler != null && clientPlayNetworkHandler.getConnection().isOpen()) {
            titleBuilder.append(" | ");

            if (this.server != null && !this.server.isRemote()) {
                titleBuilder.append(I18n.translate("title.singleplayer"));
            } else if (this.isConnectedToRealms()) {
                titleBuilder.append(I18n.translate("title.multiplayer.realms"));
            } else if (this.server == null && (this.currentServerEntry == null || !this.currentServerEntry.isLocal())) {
                titleBuilder.append(I18n.translate("title.multiplayer.other"));
            } else {
                titleBuilder.append(I18n.translate("title.multiplayer.lan"));
            }
        }

        callback.setReturnValue(titleBuilder.toString());
    }

    /**
     * Set window icon to our client icon.
     */
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Ljava/io/InputStream;Ljava/io/InputStream;)V"))
    private void setupIcon(Window instance, InputStream icon16, InputStream icon32) {
        LiquidBounce.INSTANCE.getLogger().debug("Loading client icons");

        // Find client icons
        final InputStream stream16 = LiquidBounce.class.getResourceAsStream("/assets/liquidbounce/icon_16x16.png");
        final InputStream stream32 = LiquidBounce.class.getResourceAsStream("/assets/liquidbounce/icon_32x32.png");

        // In case one of the icons are not found
        if (stream16 == null || stream32 == null) {
            LiquidBounce.INSTANCE.getLogger().error("Unable to find client icons.");
            // => Fallback to minecraft icons
            instance.setIcon(icon16, icon32);
            return;
        }

        // Load client icons
        instance.setIcon(stream16, stream32);
    }

    /**
     * Handle opening screens
     *
     * @param screen       to be opened (null = no screen at all)
     * @param callbackInfo callback
     */
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void hookScreen(Screen screen, CallbackInfo callbackInfo) {
        final ScreenEvent event = new ScreenEvent(screen);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled())
            callbackInfo.cancel();
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
        final UseCooldownEvent useCooldownEvent = new UseCooldownEvent(itemUseCooldown);
        EventManager.INSTANCE.callEvent(useCooldownEvent);
        itemUseCooldown = useCooldownEvent.getCooldown();
    }

    @Inject(method = "hasOutline", cancellable = true, at = @At("HEAD"))
    private void injectOutlineESPFix(Entity entity, CallbackInfoReturnable<Boolean> cir) {
        if (RenderingFlags.isCurrentlyRenderingEntityOutline().get()) {
            cir.setReturnValue(true);
        }
    }

}
