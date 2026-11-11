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
import net.ccbluex.liquidbounce.features.session.ClientSession;
import net.ccbluex.liquidbounce.interfaces.ConnectionAddition;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketListener;
import net.minecraft.network.PacketProcessor;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundResourcePackPopPacket;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;
import net.minecraft.network.protocol.common.ServerboundResourcePackPacket;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class MixinCommonNetworkHandler implements ConnectionAddition {

    @Shadow
    @Final
    protected Connection connection;

    @Override
    public @Nullable ClientSession liquid_bounce$getSession() {
        return ((ConnectionAddition) this.connection).liquid_bounce$getSession();
    }

    @Inject(method = "onPacketError", at = @At(value = "HEAD"), cancellable = true)
    private void packetExceptionCancel(Packet<?> packet, Exception cause, CallbackInfo ci) {
        var antiExploit = ModuleAntiExploit.INSTANCE;
        if (antiExploit.getRunning() && antiExploit.getIgnoreProtocol()) {
            ci.cancel();
        }
    }

    /**
     * The game loads one set of server resource packs, and that is the one of the main session.
     * Every other session tells its server what it wants to hear.
     */
    @Inject(method = "handleResourcePackPush", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void acceptPackWithoutLoading(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (liquid_bounce$loadsServerPacks()) {
            return;
        }

        this.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.ACCEPTED));
        this.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.DOWNLOADED));
        this.connection.send(new ServerboundResourcePackPacket(packet.id(), ServerboundResourcePackPacket.Action.SUCCESSFULLY_LOADED));
        ci.cancel();
    }

    @Inject(method = "handleResourcePackPop", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void ignorePackRemoval(ClientboundResourcePackPopPacket packet, CallbackInfo ci) {
        if (!liquid_bounce$loadsServerPacks()) {
            ci.cancel();
        }
    }

    private boolean liquid_bounce$loadsServerPacks() {
        var session = liquid_bounce$getSession();
        return session == null || session.isMain();
    }

}
