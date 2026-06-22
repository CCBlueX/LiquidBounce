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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.mojang.blaze3d.textures.GpuTexture;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.client.renderer.Lightmap;
import net.minecraft.client.renderer.state.LightmapRenderState;
import org.jspecify.annotations.NullMarked;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@NullMarked
@Mixin(Lightmap.class)
public abstract class MixinLightmap {

    @Shadow
    @Final
    private GpuTexture texture;

    /**
     * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams.Lightmap
     */
    @ModifyArg(
        method = "<init>",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/GpuDevice;createTexture(Ljava/lang/String;ILcom/mojang/blaze3d/textures/TextureFormat;IIII)Lcom/mojang/blaze3d/textures/GpuTexture;"),
        index = 1
    )
    private int makeTextureCopiable(int usage) {
        return usage | GpuTexture.USAGE_COPY_SRC | GpuTexture.USAGE_COPY_DST;
    }

    /**
     * <pre>
     * this.dirty = false;
     * </pre>
     */
    @Inject(
        method = "render(Lnet/minecraft/client/renderer/state/LightmapRenderState;)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/LightmapRenderState;needsUpdate:Z", ordinal = 0, opcode = Opcodes.GETFIELD),
        cancellable = true
    )
    private void injectCustomClearColor(LightmapRenderState renderState, CallbackInfo ci) {
        ModuleCustomAmbience.CustomLightmap customLightmap = ModuleCustomAmbience.CustomLightmap.INSTANCE;
        if (customLightmap.getRunning() && customLightmap.getMode().getActiveMode().edit(this.texture, renderState)) {
            ci.cancel();
        }
    }

}
