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
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.network;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.exploit.ModuleBungeeSpoofer;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(HandshakeC2SPacket.class)
public class MixinHandshakeC2SPacket {

    @ModifyExpressionValue(method = "write", at = @At(value = "FIELD", target = "Lnet/minecraft/network/packet/c2s/handshake/HandshakeC2SPacket;address:Ljava/lang/String;"))
    private String modifyAddress(String original) {
        if (ModuleBungeeSpoofer.INSTANCE.getEnabled()) {
            return ModuleBungeeSpoofer.INSTANCE.modifyHandshakeAddress(original);
        }

        return original;
    }

}
