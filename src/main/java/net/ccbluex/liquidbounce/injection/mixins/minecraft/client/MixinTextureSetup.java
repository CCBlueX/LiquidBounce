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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.textures.GpuTextureView;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.texture.TextureSetup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

@Mixin(TextureSetup.class)
public abstract class MixinTextureSetup {

    @Unique
    private static final Map<GpuTextureView, TextureSetup> $textureSetupCache$withoutGlTexture = new Reference2ObjectOpenHashMap<>();

    @Unique
    private static final boolean CACHING_ENABLED = false;

    /**
     * Cache the TextureSetup for the given GpuTextureView.
     */
    @WrapOperation(
        method = "withoutGlTexture(Lcom/mojang/blaze3d/textures/GpuTextureView;)Lnet/minecraft/client/texture/TextureSetup;",
        at = @At(value = "NEW", target = "(Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuTextureView;Lcom/mojang/blaze3d/textures/GpuTextureView;)Lnet/minecraft/client/texture/TextureSetup;")
    )
    private static TextureSetup cacheInit0(
        GpuTextureView gpuTextureView, GpuTextureView gpuTextureView2,
        GpuTextureView gpuTextureView3, Operation<TextureSetup> original) {
        // original call
        // return new TextureSetup(texture, null, null);
        assert gpuTextureView2 == null;
        assert gpuTextureView3 == null;
        if (gpuTextureView == null) return TextureSetup.empty();
        if (!CACHING_ENABLED) return original.call(gpuTextureView, null, null);
        return $textureSetupCache$withoutGlTexture.computeIfAbsent(gpuTextureView, k -> original.call(k, null, null));
    }

}
