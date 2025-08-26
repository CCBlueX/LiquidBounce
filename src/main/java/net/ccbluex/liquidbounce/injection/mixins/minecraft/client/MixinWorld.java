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
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.common.ChunkUpdateFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.BlockChangeEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.ccbluex.liquidbounce.utils.block.BlockExtensionsKt;
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
        if (MinecraftClient.getInstance().world != (Object) this || ChunkUpdateFlag.chunkUpdate) {
            return;
        }

        // IMPORTANT: BlockPos might be a BlockPos.Mutable, so we need to create a new BlockPos instance to issues
        EventManager.INSTANCE.callEvent(new BlockChangeEvent(BlockExtensionsKt.getImmutable(pos), state));
    }

    @ModifyReturnValue(method = "getTimeOfDay", at = @At("RETURN"))
    private long injectOverrideTime(long original) {
        return ModuleCustomAmbience.getTime(original);
    }

    @Inject(method = "getRainGradient", cancellable = true, at = @At("HEAD"))
    private void injectOverrideWeather(float delta, CallbackInfoReturnable<Float> cir) {
        var module = ModuleCustomAmbience.INSTANCE;
        var desiredWeather = module.getWeather().get();
        if (module.getRunning()) {
            switch (desiredWeather) {
                case SUNNY -> cir.setReturnValue(0.0f);
                case RAINY, THUNDER -> cir.setReturnValue(1.0f);
                case SNOWY -> cir.setReturnValue(0.9f);
            }
        }
    }

    @Inject(method = "getThunderGradient", cancellable = true, at = @At("HEAD"))
    private void injectOverrideThunder(float delta, CallbackInfoReturnable<Float> cir) {
        var module = ModuleCustomAmbience.INSTANCE;
        var desiredWeather = module.getWeather().get();
        if (module.getRunning()) {
            switch (desiredWeather) {
                case SUNNY, RAINY, SNOWY -> cir.setReturnValue(0.0f);
                case THUNDER -> cir.setReturnValue(1.0f);
            }
        }
    }

}
