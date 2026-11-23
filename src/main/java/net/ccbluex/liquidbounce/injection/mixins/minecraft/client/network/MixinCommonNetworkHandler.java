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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.network;

import net.ccbluex.liquidbounce.features.module.modules.player.ModuleAntiExploit;
import net.ccbluex.liquidbounce.features.spoofer.SpooferResourcePack;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MixinCommonNetworkHandler {

    @Shadow
    @Final
    protected Connection connection;

    @Inject(method = "onPacketError", at = @At(value = "HEAD"), cancellable = true)
    private void packetExceptionCancel(Packet<?> packet, Exception cause, CallbackInfo ci) {
        var antiExploit = ModuleAntiExploit.INSTANCE;
        if (antiExploit.getRunning() && antiExploit.getIgnoreProtocol()) {
            ci.cancel();
        }
    }

    /**
     * Hooked here instead of the PacketEvent, because servers usually push their resource pack
     * during the configuration phase, where mc.getConnection() is still null.
     */
    @Inject(method = "handleResourcePackPush", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V",
        shift = At.Shift.AFTER), cancellable = true)
    private void hookResourcePackPush(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (SpooferResourcePack.INSTANCE.getRunning()) {
            SpooferResourcePack.INSTANCE.spoof(connection, packet);
            ci.cancel();
        }
    }

}
