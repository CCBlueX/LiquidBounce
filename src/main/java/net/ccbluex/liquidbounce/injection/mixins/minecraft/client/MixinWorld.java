/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleOverrideTime;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleOverrideWeather;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(World.class)
public class MixinWorld {

    @Inject(method = "setBlockState(Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/block/BlockState;II)Z", at = @At("RETURN"))
    private void injectBlockStateChange(BlockPos pos, BlockState state, int flags, int maxUpdateDepth, CallbackInfoReturnable<Boolean> cir) {
        if (MinecraftClient.getInstance().world != (Object) this) {
            return;
        }

        EventManager.INSTANCE.callEvent(new BlockChangeEvent(pos, state));
    }

    @Inject(method = "getTimeOfDay", cancellable = true, at = @At("HEAD"))
    private void injectOverrideTime(CallbackInfoReturnable<Long> cir) {
        ModuleOverrideTime module = ModuleOverrideTime.INSTANCE;
        if (module.getEnabled()) {
            cir.setReturnValue(switch (module.getTime().get()) {
                case NOON -> 6000L;
                case NIGHT -> 13000L;
                case MID_NIGHT -> 18000L;
                default -> 1000L;
            });
            cir.cancel();
        }
    }

    @Inject(method = "getRainGradient", cancellable = true, at = @At("HEAD"))
    private void injectOverrideWeather(float delta, CallbackInfoReturnable<Float> cir) {
        ModuleOverrideWeather module = ModuleOverrideWeather.INSTANCE;
        if (module.getEnabled()) {
            cir.setReturnValue(module.getWeather().get() == ModuleOverrideWeather.WeatherType.SUNNY ? 0.0f : 1.0f);
            cir.cancel();
        }
    }

}
