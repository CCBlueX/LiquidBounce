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

import com.llamalad7.mixinextras.injector.ModifyReceiver;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.ServerPingedEvent;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerStatusPinger;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.client.multiplayer.ServerStatusPinger$1")
public abstract class MixinMultiplayerServerListPinger {

    @ModifyReceiver(method = "handlePongResponse",
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/multiplayer/ServerData;ping:J", opcode = Opcodes.PUTFIELD))
    private ServerData onPingResult$onPingResult(ServerData instance, long value) {
        instance.ping = value;
        EventManager.INSTANCE.callEvent(new ServerPingedEvent(instance));
        return instance;
    }

    /**
     * The legacy ping connects to {@link InetSocketAddress#getAddress()}, which is null for an address left to the
     * proxy to resolve.
     */
    @WrapWithCondition(method = "onDisconnect", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/ServerStatusPinger;pingLegacyServer(Ljava/net/InetSocketAddress;Lnet/minecraft/client/multiplayer/resolver/ServerAddress;Lnet/minecraft/client/multiplayer/ServerData;Lnet/minecraft/server/network/EventLoopGroupHolder;)V"))
    private boolean skipUnresolvedLegacyPing(ServerStatusPinger instance, InetSocketAddress address,
                                             ServerAddress rawAddress, ServerData data,
                                             EventLoopGroupHolder eventLoopGroupHolder) {
        return !address.isUnresolved();
    }

}
