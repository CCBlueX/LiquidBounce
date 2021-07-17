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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.TransferOrigin;
import net.ccbluex.liquidbounce.features.misc.ProxyManager;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.Packet;
import net.minecraft.util.Lazy;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow @Final public static Lazy<EpollEventLoopGroup> EPOLL_CLIENT_IO_GROUP;

    @Shadow @Final public static Lazy<NioEventLoopGroup> CLIENT_IO_GROUP;

    /**
     * Handle sending packets
     *
     * @param packet packet to send
     * @param callbackInfo callback
     */
    @Inject(method = "send(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void hookSendingPacket(Packet<?> packet, final CallbackInfo callbackInfo) {
        final PacketEvent event = new PacketEvent(TransferOrigin.SEND, packet);

        EventManager.INSTANCE.callEvent(event);

        if (event.isCancelled())
            callbackInfo.cancel();
    }

    /**
     * Handle receiving packets
     *
     * @param channelHandlerContext channel context
     * @param packet                packet to receive
     * @param callbackInfo          callback
     */
    @Inject(method = "channelRead0", at = @At("HEAD"), cancellable = true)
    private void hookReceivingPacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
        final PacketEvent event = new PacketEvent(TransferOrigin.RECEIVE, packet);

        EventManager.INSTANCE.callEvent(event);

        if (event.isCancelled())
            callbackInfo.cancel();
    }

    /**
     * Hook custom netty connection
     * @author mojang
     */
    @Overwrite
    @Environment(EnvType.CLIENT)
    public static ClientConnection connect(InetSocketAddress address, boolean useEpoll) {
        final ClientConnection clientConnection = new ClientConnection(NetworkSide.CLIENTBOUND);
        Class class2;
        Lazy lazy2;
        if (Epoll.isAvailable() && useEpoll) {
            class2 = EpollSocketChannel.class;
            lazy2 = EPOLL_CLIENT_IO_GROUP;
        } else {
            class2 = NioSocketChannel.class;
            lazy2 = CLIENT_IO_GROUP;
        }

        new Bootstrap()
                .group((EventLoopGroup)lazy2.get())
                .handler(ProxyManager.INSTANCE.setupConnect(clientConnection))
                .channel(class2)
                .connect(address.getAddress(), address.getPort())
                .syncUninterruptibly();
        return clientConnection;
    }

}
