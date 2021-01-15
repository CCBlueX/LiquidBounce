/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
package net.ccbluex.liquidbounce.injection.mixins;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.ScreenEvent;
import net.ccbluex.liquidbounce.renderer.engine.GlyphPage;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.resource.ClientBuiltinResourcePackProvider;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

import java.io.IOException;
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
    public abstract ClientBuiltinResourcePackProvider getResourcePackDownloader();

    @Shadow
    public abstract void openScreen(Screen screen);

    /**
     * Entry point of our hacked client
     *
     * @param callback not needed
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void startClient(final CallbackInfo callback) {
        LiquidBounce.INSTANCE.start();

        System.out.println("Max texture size " + GlyphPage.Companion.getMaxTextureSize().getValue());
    }

    /**
     * Exit point of our hacked client
     *
     * @param callback not needed
     */
    @Inject(method = "stop", at = @At("HEAD"))
    private void stopClient(final CallbackInfo callback) {
        LiquidBounce.INSTANCE.stop();
    }

    /**
     * Modify window title to our client title.
     * Example: LiquidBounce v1.0.0 | 1.16.3
     *
     * @param callback our window title
     */
    @Inject(method = "getWindowTitle", at = @At("HEAD"), cancellable = true)
    private void getClientTitle(final CallbackInfoReturnable<String> callback) {
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
     *
     * @param args arguments of target method
     */
    @ModifyArgs(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;setIcon(Ljava/io/InputStream;Ljava/io/InputStream;)V"))
    private void setupIcon(final Args args) {
        try {
            LiquidBounce.INSTANCE.getLogger().debug("Loading client icons");

            // Load client icons
            final InputStream stream32 = getResourcePackDownloader().getPack().open(ResourceType.CLIENT_RESOURCES,
                    new Identifier("liquidbounce:icon_16x16.png"));
            final InputStream stream64 = getResourcePackDownloader().getPack().open(ResourceType.CLIENT_RESOURCES,
                    new Identifier("liquidbounce:icon_32x32.png"));

            args.setAll(stream32, stream64);
        } catch (final IOException e) {
            LiquidBounce.INSTANCE.getLogger().error("Unable to load client icons.", e);

            // => Fallback to minecraft icons
        }
    }

    /**
     * Handle opening screens
     *
     * @param screen to be opened (null = no screen at all)
     * @param callbackInfo          callback
     */
    @Inject(method = "openScreen", at = @At("HEAD"))
    private void onScreen(final Screen screen, final CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new ScreenEvent(screen));
    }

}
