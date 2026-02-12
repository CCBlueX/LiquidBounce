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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.google.common.collect.ImmutableList;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.resolver.AddressCheck;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.stream.Collector;
import java.util.stream.Stream;

@NullMarked
@Mixin(AddressCheck.class)
public interface MixinAddressCheck {

    /**
     * java.lang.NoClassDefFoundError: Could not initialize class net.minecraft.client.multiplayer.resolver.ServerNameResolver
     * 	at ~
     * Caused by: java.lang.ExceptionInInitializerError: Exception java.util.ServiceConfigurationError: com.mojang.blocklist.BlockListSupplier: com.mojang.patchy.MojangBlockListSupplier not a subtype
     */
    @WrapOperation(
        method = "createFromService",
        at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;collect(Ljava/util/stream/Collector;)Ljava/lang/Object;", remap = false)
    )
    private static Object preventCannotConnectToAnyServer(
        Stream<?> instance,
        Collector<?, ?, ?> collector,
        Operation<?> operation
    ) {
        try {
            return operation.call(instance, collector);
        } catch (NoClassDefFoundError | ExceptionInInitializerError error) {
            return ImmutableList.of();
        }
    }

}
