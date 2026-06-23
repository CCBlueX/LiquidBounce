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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.fog;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FogRenderer.class)
public abstract class MixinFogRenderer {

    @Inject(method = "computeFogColor", at = @At("HEAD"), cancellable = true)
    private void editFogColor(Camera camera, float partialTicks, ClientLevel level, int renderDistance, float darkenWorldAmount, Vector4f dest, CallbackInfo ci) {
        var fogColorOverride = ModuleCustomAmbience.FogValueGroup.FogColorOverride.INSTANCE;
        if (fogColorOverride.getRunning()) {
            fogColorOverride.getColor().toVector4f(dest);
            ci.cancel();
        }
    }

    @Inject(
        method = "updateBuffer(Lnet/minecraft/client/renderer/fog/FogData;)V",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/GpuBuffer;map(ZZ)Lcom/mojang/blaze3d/buffers/GpuBufferSlice$MappedView;")
    )
    private void editFogData(FogData fog, CallbackInfo ci) {
        ModuleCustomAmbience.FogValueGroup.INSTANCE.modifyFogData(fog);
    }

}
