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

import com.mojang.realmsclient.client.RealmsClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * {@link RealmsClient#getOrCreate(net.minecraft.client.Minecraft)} caches the instance it built from the
 * session that happened to be active at the time, in final fields, and hands it out from then on. Nulling
 * the instance is the only way to make it pick up the account we switched to.
 * <p>
 * Realms is not covered by Fabric intermediary, so its names are the same in development and production.
 */
@Mixin(RealmsClient.class)
public interface MixinRealmsClientAccessor {

    @Accessor(value = "realmsClientInstance", remap = false)
    static void setRealmsClientInstance(RealmsClient instance) {
        throw new AssertionError();
    }

}
