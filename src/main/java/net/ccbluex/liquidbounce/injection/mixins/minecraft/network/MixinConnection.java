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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPipeline;
import io.netty.resolver.NoopAddressResolverGroup;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.PacketEvent;
import net.ccbluex.liquidbounce.event.events.PipelineEvent;
import net.ccbluex.liquidbounce.event.events.TransferOrigin;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoSwing;
import net.minecraft.network.BandwidthDebugMonitor;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(Connection.class)
public abstract class MixinConnection {

    @Shadow
    private static <T extends PacketListener> void genericsFtw(Packet<T> packet, PacketListener listener) {
    }

    /**
     * Handle sending packets
     *
     * @param packet       packet to send
     * @param callbackInfo callback
     */
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void hookSendingPacket(Packet<?> packet, final CallbackInfo callbackInfo) {
        if (packet instanceof ServerboundPunchPacket && ModuleNoSwing.INSTANCE.shouldHideForServer()) {
            callbackInfo.cancel();
            return;
        }

        final PacketEvent event = new PacketEvent(TransferOrigin.OUTGOING, packet, true);

        EventManager.INSTANCE.callEvent(event);

        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    /**
     * Handle receiving packets
     */
    @Inject(method = "genericsFtw", at = @At("HEAD"), cancellable = true, require = 1)
    private static void hookReceivingPacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (packet instanceof ClientboundBundlePacket bundleS2CPacket) {
            // Cancel handling bundle packets since we take this in our own hands
            ci.cancel();

            // Handle each packet individually
            for (Packet<?> packetInBundle : bundleS2CPacket.subPackets()) {
                try {
                    // This will call this method again, but with a single packet instead of a bundle
                    genericsFtw(packetInBundle, listener);
                } catch (RunningOnDifferentThreadException ignored) {
                }
                // usually we also handle RejectedExecutionException and
                // ClassCastException, but both of them will disconnect the player
                // and therefore are handled by the upper layer
            }
            return;
        }

        final PacketEvent event = new PacketEvent(TransferOrigin.INCOMING, packet, true);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * Hook proxy
     */
    @Inject(method = "configureSerialization", at = @At("HEAD"))
    private static void hookProxy(ChannelPipeline pipeline, PacketFlow side, boolean local, BandwidthDebugMonitor packetSizeLogger, CallbackInfo ci) {
        if (side == PacketFlow.CLIENTBOUND) {
            final PipelineEvent event = new PipelineEvent(pipeline, local);
            EventManager.INSTANCE.callEvent(event);
        }
    }

    /**
     * Vanilla connects to {@link InetSocketAddress#getAddress()}, which is null for an address left to the proxy
     * to resolve.
     */
    @WrapOperation(method = "connect(Ljava/net/InetSocketAddress;Lnet/minecraft/server/network/EventLoopGroupHolder;Lnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;",
        at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/Bootstrap;connect(Ljava/net/InetAddress;I)Lio/netty/channel/ChannelFuture;", remap = false))
    private static ChannelFuture hookUnresolvedConnect(Bootstrap bootstrap, InetAddress host, int port,
                                                       Operation<ChannelFuture> original,
                                                       @Local(argsOnly = true) InetSocketAddress address) {
        if (address.isUnresolved()) {
            return bootstrap.resolver(NoopAddressResolverGroup.INSTANCE).connect(address);
        }

        return original.call(bootstrap, host, port);
    }

}
