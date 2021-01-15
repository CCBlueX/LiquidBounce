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

import io.netty.channel.ChannelHandlerContext;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientConnection.class)
public class MixinClientConnection {

    /**
     * Handle sending packets
     *
     * @param packet packet to send
     * @param callbackInfo callback
     */
    @Inject(method = "send(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void handleSendingPacket(Packet<?> packet, final CallbackInfo callbackInfo) {
        PacketEvent event = new PacketEvent(packet);

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
    private void handleReceivingPacket(ChannelHandlerContext channelHandlerContext, Packet<?> packet, CallbackInfo callbackInfo) {
        PacketEvent event = new PacketEvent(packet);

        EventManager.INSTANCE.callEvent(event);

        if (event.isCancelled())
            callbackInfo.cancel();
    }

}
