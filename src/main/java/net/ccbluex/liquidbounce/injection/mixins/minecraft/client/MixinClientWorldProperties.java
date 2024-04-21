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
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientWorld.Properties.class)
public class MixinClientWorldProperties {

    @Inject(method = "getTimeOfDay", cancellable = true, at = @At("HEAD"))
    private void injectOverrideTime(CallbackInfoReturnable<Long> cir) {
        var module = ModuleCustomAmbience.INSTANCE;
        if (module.getEnabled()) {
            switch (module.getTime().get()) {
                case DAY -> cir.setReturnValue(1000L);
                case NOON -> cir.setReturnValue(6000L);
                case NIGHT -> cir.setReturnValue(13000L);
                case MID_NIGHT -> cir.setReturnValue(18000L);
            }
        }
    }

}
