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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.spoofer.SpooferBungeeCord;
import net.minecraft.network.protocol.handshake.ClientIntentionPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ClientIntentionPacket.class)
public abstract class MixinClientIntentionPacket {

    @ModifyExpressionValue(method = "write", at = @At(value = "FIELD", target = "Lnet/minecraft/network/protocol/handshake/ClientIntentionPacket;hostName:Ljava/lang/String;"))
    private String modifyAddress(String original) {
        if (SpooferBungeeCord.INSTANCE.getRunning()) {
            return SpooferBungeeCord.INSTANCE.modifyHandshakeAddress(original);
        }

        return original;
    }

}
