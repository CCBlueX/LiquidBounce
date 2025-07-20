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
package net.ccbluex.liquidbounce.injection.mixins.blaze3d;

import com.mojang.blaze3d.systems.RenderSystem;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderSystem.class, remap = false)
public abstract class MixinRenderSystem {

    @Inject(method = "clearColor", at = @At(value = "HEAD"), cancellable = true)
    private static void injectFog(float red, float green, float blue, float alpha, CallbackInfo ci) {
        if (ModuleCustomAmbience.FogConfigurable.INSTANCE.modifyClearColor()) {
            ci.cancel();
        }
    }

}
