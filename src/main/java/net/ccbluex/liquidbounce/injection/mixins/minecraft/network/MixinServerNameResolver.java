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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyDns;
import net.minecraft.client.multiplayer.resolver.AddressCheck;
import net.minecraft.client.multiplayer.resolver.ResolvedServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.multiplayer.resolver.ServerAddressResolver;
import net.minecraft.client.multiplayer.resolver.ServerNameResolver;
import net.minecraft.client.multiplayer.resolver.ServerRedirectHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Predicate;

/**
 * Patches out Mojang's server blacklist and lets {@link ProxyDns} take over lookups
 *
 * @see AddressCheck
 */
@Mixin(ServerNameResolver.class)
public abstract class MixinServerNameResolver {

    @WrapOperation(
        method = "resolveAddress",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/resolver/AddressCheck;isAllowed(Lnet/minecraft/client/multiplayer/resolver/ServerAddress;)Z")
    )
    private boolean isAllowedA(AddressCheck instance, ServerAddress serverAddress, Operation<Boolean> original) {
        return true;
    }

    @WrapOperation(
        method = "resolveAddress",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/resolver/AddressCheck;isAllowed(Lnet/minecraft/client/multiplayer/resolver/ResolvedServerAddress;)Z")
    )
    private boolean isAllowedB(AddressCheck instance, ResolvedServerAddress resolvedServerAddress, Operation<Boolean> original) {
        return true;
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    @WrapOperation(method = "resolveAddress", at = @At(value = "INVOKE",
        target = "Ljava/util/Optional;filter(Ljava/util/function/Predicate;)Ljava/util/Optional;", remap = false))
    private Optional<?> isAllowedC(Optional<?> instance, Predicate<?> predicate, Operation<Optional<?>> original) {
        return instance;
    }

    @WrapOperation(method = "<clinit>", at = @At(value = "NEW", target = "(Lnet/minecraft/client/multiplayer/resolver/ServerAddressResolver;Lnet/minecraft/client/multiplayer/resolver/ServerRedirectHandler;Lnet/minecraft/client/multiplayer/resolver/AddressCheck;)Lnet/minecraft/client/multiplayer/resolver/ServerNameResolver;"))
    private static ServerNameResolver hookProxyDns(ServerAddressResolver resolver, ServerRedirectHandler redirectHandler,
                                                   AddressCheck addressCheck, Operation<ServerNameResolver> original) {
        return original.call(ProxyDns.resolver(resolver), ProxyDns.redirectHandler(redirectHandler), addressCheck);
    }

}
