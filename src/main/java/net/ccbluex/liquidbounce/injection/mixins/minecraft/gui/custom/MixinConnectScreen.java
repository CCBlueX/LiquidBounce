/*
 *
 *  * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *  *
 *  * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ConnectionDetailsEvent;
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent;
import net.ccbluex.liquidbounce.features.misc.HideAppearance;
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager;
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
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.net.InetSocketAddress;

import static net.ccbluex.liquidbounce.utils.client.TextExtensionsKt.hideSensitiveAddress;

@Mixin(ConnectScreen.class)
public abstract class MixinConnectScreen extends MixinScreen {

    @Shadow
    volatile @Nullable ClientConnection connection;
    @Unique
    private ServerAddress serverAddress = null;

    @Inject(method = "shouldCloseOnEsc", at = @At("HEAD"), cancellable = true)
    private void onShouldCloseOnEsc(CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }
    @Unique
    private static String getSocketAddress(ClientConnection clientConnection, ServerAddress serverAddress) {
        if (clientConnection.getAddress() instanceof InetSocketAddress addr) {
            String hostString = addr.getHostString();
            String hostAddress = addr.isUnresolved() ? "<unresolved>" : addr.getAddress().getHostAddress();
            if (hostString.equals(serverAddress.getAddress())) {
                return hostAddress + ":" + addr.getPort();
            } else {
                return hostString + "/" + hostAddress + ":" + addr.getPort();
            }
        }
        return "<unknown>";
    }

    @Shadow
    public abstract void connect(MinecraftClient client, ServerAddress address, ServerInfo info, @Nullable CookieStorage cookieStorage);

    @Inject(
            method = "render",
            at = @At("HEAD"),
            cancellable = true
    )
    private void onRenderHead(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!HideAppearance.INSTANCE.isHidingNow()) {

            if (this.connection != null && this.serverAddress != null && !HideAppearance.INSTANCE.isHidingNow()) {
                Text details = getConnectionDetails(this.connection, this.serverAddress);
                EventManager.INSTANCE.callEvent(new ConnectionDetailsEvent(details));
            }

            ci.cancel();
        }
    }

    @Redirect(
            method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V",
            at = @At(
                    value = "INVOKE",

                    target = "Ljava/lang/Thread;start()V"
            )
    )
    private void delayConnectorStart(Thread originalConnectorThread) {
        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
            originalConnectorThread.start();
        }, "Delayed-Connector-Starter").start();
    }

    @Inject(method = "connect(Lnet/minecraft/client/MinecraftClient;Lnet/minecraft/client/network/ServerAddress;Lnet/minecraft/client/network/ServerInfo;Lnet/minecraft/client/network/CookieStorage;)V", at = @At("HEAD"), cancellable = true)
    private void injectConnect(MinecraftClient client, ServerAddress address, ServerInfo info, CookieStorage cookieStorage, CallbackInfo ci) {
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

    /**
     * 组装并返回当前连接详情文本，同时内部也会触发一次 ConnectionDetailsEvent（以防有人直接调用此方法）。
     */
    @Unique
    private Text getConnectionDetails(ClientConnection clientConnection, ServerAddress serverAddress) {
        var socketAddr = getSocketAddress(clientConnection, serverAddress);
        var serverAddr = String.format(
                "%s:%s",
                hideSensitiveAddress(serverAddress.getAddress()),
                serverAddress.getPort()
        );

        var ipInfo = IpInfoApi.INSTANCE.getCurrent();
        var client = Text.literal("Client").formatted(Formatting.BLUE);
        if (ipInfo != null && ipInfo.getCountry() != null) {
            client.append(Text.literal(" (").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(ipInfo.getCountry()).formatted(Formatting.BLUE))
                    .append(Text.literal(")").formatted(Formatting.DARK_GRAY));
        }

        var spacer = Text.literal(" ⟺ ").formatted(Formatting.DARK_GRAY);
        var socket = Text.literal(socketAddr).formatted(
                ProxyManager.INSTANCE.getCurrentProxy() != null ? Formatting.GOLD : Formatting.RED
        );
        var server = Text.literal(serverAddr).formatted(Formatting.GREEN);

        Text result = Text.empty()
                .append(client)
                .append(spacer.copy())
                .append(socket)
                .append(spacer.copy())
                .append(server);


        EventManager.INSTANCE.callEvent(new ConnectionDetailsEvent(result));
        return result;
    }
}
