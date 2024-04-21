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

import net.minecraft.client.network.Address;
import net.minecraft.client.network.AllowedAddressResolver;
import net.minecraft.client.network.BlockListChecker;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Patches out Mojang's server blacklist
 *
 * @see BlockListChecker
 */
@Mixin(AllowedAddressResolver.class)
public class MixinAllowedAddressResolver {

    @Redirect(method = "resolve", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/BlockListChecker;isAllowed(Lnet/minecraft/client/network/ServerAddress;)Z"))
    private boolean isAllowed(BlockListChecker instance, ServerAddress serverAddress) {
        return true;
    }

    @Redirect(method = "resolve", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/BlockListChecker;isAllowed(Lnet/minecraft/client/network/Address;)Z"))
    private boolean isAllowed(BlockListChecker instance, Address address) {
        return true;
    }

}
