/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2025 CCBlueX
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
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import net.ccbluex.liquidbounce.utils.network.PacketRegistryKt;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.packet.PacketType;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.TreeSet;

@Mixin(PacketType.class)
public class MixinPacketType {

    /**
     * Registers a packet type for the given [networkSide] with the specified [id].
     *
     * @param networkSide The side of the network.
     * @param identifier The identifier of the packet type to register defined in {@link PacketType}
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void hookPacketRegistry(NetworkSide networkSide, Identifier identifier, CallbackInfo ci) {
        PacketRegistryKt.getPacketRegistry().computeIfAbsent(networkSide, k -> new TreeSet<>())
                .add(identifier);
    }
}
