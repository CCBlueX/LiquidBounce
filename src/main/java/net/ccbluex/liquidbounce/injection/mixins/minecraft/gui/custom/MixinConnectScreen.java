/*
 *
 *  * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *  *
 *  * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen extends MixinScreen {

    @Shadow
    private Text status;

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
                this.height / 2 + 20, 0xFFFFFF);
    }

    @Inject(method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;)V", at = @At("HEAD"))
    private void injectConnect(final MinecraftClient client, final ServerAddress address, @Nullable final ServerInfo info, final CallbackInfo callback) {
        this.serverAddress = address;
    }

    @Unique
    private String getConnectionDetails(ClientConnection clientConnection, ServerAddress serverAddress) {
        var ipInfo = IpInfoApi.INSTANCE.getLocalIpInfo();
        var localAddress = ipInfo == null ? "Client" : ipInfo.getIp(); // If we don't have an IP, just say "Client"
        // This will either be the proxy address or the server address
        var proxyAddr = clientConnection.getAddress();
        var serverAddr = String.format("%s:%s", serverAddress.getAddress(), serverAddress.getPort());

        return String.format("%s <> %s <> %s", localAddress, proxyAddr, serverAddr);
    }

}
