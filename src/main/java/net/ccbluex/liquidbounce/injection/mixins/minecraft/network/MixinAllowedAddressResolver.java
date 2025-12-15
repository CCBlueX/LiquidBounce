/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import com.google.common.base.Predicates;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.network.Address;
import net.minecraft.client.network.AllowedAddressResolver;
import net.minecraft.client.network.BlockListChecker;
import net.minecraft.client.network.ServerAddress;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.function.Predicate;

/**
 * Patches out Mojang's server blacklist
 *
 * @see BlockListChecker
 */
@Mixin(AllowedAddressResolver.class)
public class MixinAllowedAddressResolver {

    @WrapOperation(method = "resolve", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/BlockListChecker;isAllowed(Lnet/minecraft/client/network/ServerAddress;)Z"))
    private boolean isAllowedA(BlockListChecker instance, ServerAddress serverAddress, Operation<Boolean> original) {
        return true;
    }

    @WrapOperation(method = "resolve", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/network/BlockListChecker;isAllowed(Lnet/minecraft/client/network/Address;)Z"))
    private boolean isAllowedB(BlockListChecker instance, Address address, Operation<Boolean> original) {
        return true;
    }

    @ModifyArg(method = "resolve", at = @At(value = "INVOKE",
        target = "Ljava/util/Optional;filter(Ljava/util/function/Predicate;)Ljava/util/Optional;"))
    private Predicate<?> isAllowedC(Predicate<?> predicate) {
        return Predicates.alwaysTrue();
    }

}
