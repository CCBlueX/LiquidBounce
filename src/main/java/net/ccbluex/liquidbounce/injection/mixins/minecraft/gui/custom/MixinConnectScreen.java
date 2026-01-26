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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager;
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinScreen;
import net.ccbluex.liquidbounce.utils.text.PlainText;
import net.ccbluex.liquidbounce.utils.text.TextList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.TransferState;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;
import java.util.ArrayList;

import static net.ccbluex.liquidbounce.utils.client.TextExtensionsKt.hideSensitiveAddress;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen extends MixinScreen {

    @Shadow
    volatile @Nullable Connection connection;

    @Shadow
    public abstract void connect(
        Minecraft client, ServerAddress address, ServerData info, @Nullable TransferState cookieStorage);

    @Unique
    private ServerAddress serverAddress = null;

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
    private void injectRender(GuiGraphics context, int mouseX, int mouseY, float delta, final CallbackInfo callback) {
        /*
         * Make a text demonstration of the connection status
         * This is useful for debugging the connection trace
         *
         * Looks like this: Client <> Proxy <> Server
         *
         * For Client, it should show the actual client IP
         * For Proxy, it should show the proxy IP
         * For Server, it should show the server IP
         */

        var clientConnection = this.connection;
        var serverAddress = this.serverAddress;

        if (clientConnection == null || this.serverAddress == null || HideAppearance.INSTANCE.isHidingNow()) {
            return;
        }

        var connectionDetails = getConnectionDetails(clientConnection, serverAddress);
        context.drawCenteredString(this.font, connectionDetails, this.width / 2,
            this.height / 2 - 60, -1);
    }


    @Inject(method = "connect(Lnet/minecraft/client/Minecraft;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/client/multiplayer/TransferState;)V", at = @At("HEAD"), cancellable = true)
    private void injectConnect(Minecraft client, ServerAddress address, ServerData info, TransferState cookieStorage, CallbackInfo ci) {
        this.serverAddress = address;
        var event = EventManager.INSTANCE.callEvent(new ServerConnectEvent((ConnectScreen) (Object) this, address, info, cookieStorage));

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyConstant(method = "render", constant = @Constant(intValue = 50))
    private int modifyStatusY(int original) {
        return original + 30;
    }

    @Unique
    private Component getConnectionDetails(Connection clientConnection, ServerAddress serverAddress) {
        // This will either be the socket address or the server address
        var socketAddr = getSocketAddress(clientConnection, serverAddress);
        var serverAddr = String.format(
                "%s:%s",
                hideSensitiveAddress(serverAddress.getHost()),
                serverAddress.getPort()
        );
        var ipInfo = IpInfoApi.INSTANCE.getCurrent();

        var spacer = PlainText.of(" ⟺ ", ChatFormatting.DARK_GRAY);

        var textParts = new ArrayList<Component>();

        var client = PlainText.of("Client", ChatFormatting.BLUE);
        textParts.add(client);

        if (ipInfo != null) {
            var country = ipInfo.getCountry();

            if (country != null) {
                textParts.add(PlainText.of(" (", ChatFormatting.DARK_GRAY));
                textParts.add(PlainText.of(country, ChatFormatting.BLUE));
                textParts.add(PlainText.of(")", ChatFormatting.DARK_GRAY));
            }
        }
        textParts.add(spacer);

        var socket = Component.literal(socketAddr);
        if (ProxyManager.INSTANCE.getCurrentProxy() != null) {
            socket.withStyle(ChatFormatting.GOLD); // Proxy good
        } else {
            socket.withStyle(ChatFormatting.RED); // No proxy - shows server address
        }
        textParts.add(socket);
        textParts.add(spacer);

        var server = PlainText.of(serverAddr, ChatFormatting.GREEN);
        textParts.add(server);

        return TextList.of(textParts);
    }

    @Unique
    private static String getSocketAddress(Connection clientConnection, ServerAddress serverAddress) {
        String socketAddr;
        if (clientConnection.getRemoteAddress() instanceof InetSocketAddress address) {
            // In this we do not redact the host string - it is usually not sensitive
            var hostString = address.getHostString();
            var hostAddress = address.isUnresolved() ?
                    "<unresolved>" :
                    address.getAddress().getHostAddress();

            if (hostString.equals(serverAddress.getHost())) {
                socketAddr = String.format("%s:%s", hostAddress, address.getPort());
            } else {
                socketAddr = String.format("%s/%s:%s", hostString, hostAddress, address.getPort());
            }
        } else {
            socketAddr = "<unknown>";
        }
        return socketAddr;
    }

}
