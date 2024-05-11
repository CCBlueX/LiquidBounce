/*
 *
 *  * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *  *
 *  * Copyright (c) 2015 - 2024 CCBlueX
 *  *
 *  * LiquidBounce is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * LiquidBounce is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 *
 */

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import net.ccbluex.liquidbounce.api.IpInfoApi;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent;
import net.ccbluex.liquidbounce.features.misc.ProxyManager;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.network.CookieStorage;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen extends MixinScreen {

    @Shadow
    private volatile @Nullable ClientConnection connection;

    @Unique
    private ServerAddress serverAddress = null;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawCenteredTextWithShadow(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;III)V"))
    private void injectRender(DrawContext context, int mouseX, int mouseY, float delta, final CallbackInfo callback) {
        /*
         * Make a text demonstration of the connection status
         * This is useful for debugging the connection trace
         * 
         * Looks like this: Client <> Proxy <> Server
         * 
         * For client it should show the actual client IP
         * For Proxy it should show the proxy IP
         * For Server it should show the server IP
         */
        
        var clientConnection = this.connection;
        var serverAddress = this.serverAddress;
        
        if (clientConnection == null || this.serverAddress == null) {
            return;
        }

        var connectionDetails = getConnectionDetails(clientConnection, serverAddress);
        context.drawCenteredTextWithShadow(this.textRenderer, connectionDetails, this.width / 2,
                this.height / 2 - 60, 0xFFFFFF);
    }


    @Inject(method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V", at = @At("HEAD"))
    private void injectConnect(MinecraftClient client, ServerAddress address, ServerInfo info, CookieStorage cookieStorage, CallbackInfo ci) {
        this.serverAddress = address;
        EventManager.INSTANCE.callEvent(new ServerConnectEvent(info.name, info.address));
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 50))
    private int modifyStatusY(int original) {
        return original + 30;
    }

    @Unique
    private Text getConnectionDetails(ClientConnection clientConnection, ServerAddress serverAddress) {
        // This will either be the proxy address or the server address
        var proxyAddr = clientConnection.getAddress();
        var serverAddr = String.format("%s:%s", serverAddress.getAddress(), serverAddress.getPort());
        var ipInfo = IpInfoApi.INSTANCE.getLocalIpInfo();

        var client = Text.literal("Client").formatted(Formatting.BLUE);
        if (ipInfo != null) {
            var country = ipInfo.getCountry();

            if (country != null) {
                client.append(Text.literal(" (").formatted(Formatting.DARK_GRAY));
                client.append(Text.literal(country).formatted(Formatting.BLUE));
                client.append(Text.literal(")").formatted(Formatting.DARK_GRAY));
            }
        }
        var spacer = Text.literal(" ⟺ ").formatted(Formatting.DARK_GRAY);

        var proxy = Text.literal(proxyAddr == null ? "(Unknown)" : proxyAddr.toString());
        if (ProxyManager.INSTANCE.getCurrentProxy() != null) {
            proxy.formatted(Formatting.GOLD); // Proxy good
        } else {
            proxy.formatted(Formatting.RED); // No proxy - shows server address
        }

        var server = Text.literal(serverAddr).formatted(Formatting.GREEN);

        return Text.empty()
                .append(client)
                .append(spacer.copy())
                .append(proxy)
                .append(spacer.copy())
                .append(server);
    }

}
