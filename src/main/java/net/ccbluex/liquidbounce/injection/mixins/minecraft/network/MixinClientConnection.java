/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.event.PipelineEvent;
import net.ccbluex.liquidbounce.event.TransferOrigin;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    @Shadow
    @Final
    private NetworkSide side;

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
    @Inject(method = "channelRead0(Lio/netty/channel/ChannelHandlerContext;Lnet/minecraft/network/packet/Packet;)V",
            at = @At("HEAD"), cancellable = true, require = 1)
    private void hookReceivingPacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet,
                                     CallbackInfo callbackInfo) {
        // Only handle clientbound packets
        if (side == NetworkSide.CLIENTBOUND) {
            final PacketEvent event = new PacketEvent(TransferOrigin.RECEIVE, packet, true);
            EventManager.INSTANCE.callEvent(event);

            if (event.isCancelled()) {
                callbackInfo.cancel();
            }
        }
    }

    /**
     * Hook proxy
     */
    @Inject(method = "addHandlers", at = @At("HEAD"))
    private static void hookProxy(ChannelPipeline pipeline, NetworkSide side, CallbackInfo callbackInfo) {
        if (side == NetworkSide.CLIENTBOUND) {
            final PipelineEvent event = new PipelineEvent(pipeline);
            EventManager.INSTANCE.callEvent(event);
        }
    }

}
