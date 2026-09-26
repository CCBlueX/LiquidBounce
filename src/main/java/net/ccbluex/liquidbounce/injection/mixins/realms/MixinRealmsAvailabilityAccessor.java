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

package net.ccbluex.liquidbounce.injection.mixins.realms;

import com.mojang.realmsclient.RealmsAvailability;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.concurrent.CompletableFuture;

/**
 * {@link RealmsAvailability#get()} only re-runs its check when the cached result carries an exception, and
 * an {@code AUTHENTICATION_ERROR} carries none - so a failure from an earlier session is never retried.
 */
@Mixin(RealmsAvailability.class)
public interface MixinRealmsAvailabilityAccessor {

    @Accessor(value = "future", remap = false)
    static void setFuture(CompletableFuture<RealmsAvailability.Result> future) {
        throw new AssertionError();
    }

}
