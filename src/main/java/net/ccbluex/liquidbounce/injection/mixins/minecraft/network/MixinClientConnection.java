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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import io.netty.channel.ChannelPipeline;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.DisconnectEvent;
import net.ccbluex.liquidbounce.event.events.PacketEvent;
import net.ccbluex.liquidbounce.event.events.PipelineEvent;
import net.ccbluex.liquidbounce.event.events.TransferOrigin;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.OffThreadException;
import net.minecraft.network.handler.PacketSizeLogger;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public abstract class MixinClientConnection {

    @Shadow
    protected static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener listener) {
    }

    /**
     * Handle sending packets
     *
     * @param packet       packet to send
     * @param callbackInfo callback
     */
    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void hookSendingPacket(Packet<?> packet, final CallbackInfo callbackInfo) {
        final PacketEvent event = new PacketEvent(TransferOrigin.SEND, packet, true);

        EventManager.INSTANCE.callEvent(event);

        if (event.isCancelled()) {
            callbackInfo.cancel();
        }
    }

    /**
     * Handle receiving packets
     */
    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true, require = 1)
    private static void hookReceivingPacket(Packet<?> packet, PacketListener listener, CallbackInfo ci) {
        if (packet instanceof BundleS2CPacket bundleS2CPacket) {
            // Cancel handling bundle packets since we take this in our own hands
            ci.cancel();

            // Handle each packet individually
            for (Packet<?> packetInBundle : bundleS2CPacket.getPackets()) {
                try {
                    // This will call this method again, but with a single packet instead of a bundle
                    handlePacket(packetInBundle, listener);
                } catch (OffThreadException ignored) {
                }
                // usually we also handle RejectedExecutionException and
                // ClassCastException, but both of them will disconnect the player
                // and therefore are handled by the upper layer
            }
            return;
        }

        final PacketEvent event = new PacketEvent(TransferOrigin.RECEIVE, packet, true);
        EventManager.INSTANCE.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    /**
     * Hook proxy
     */
    @Inject(method = "addHandlers", at = @At("HEAD"))
    private static void hookProxy(ChannelPipeline pipeline, NetworkSide side, boolean local, PacketSizeLogger packetSizeLogger, CallbackInfo ci) {
        if (side == NetworkSide.CLIENTBOUND) {
            final PipelineEvent event = new PipelineEvent(pipeline);
            EventManager.INSTANCE.callEvent(event);
        }
    }

    @Inject(method = "handleDisconnection", at = @At("HEAD"))
    private void handleDisconnection(CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new DisconnectEvent());
    }

}
