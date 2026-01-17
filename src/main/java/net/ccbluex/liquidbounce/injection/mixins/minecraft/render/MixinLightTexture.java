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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightTexture.class)
public abstract class MixinLightTexture {

    @Shadow
    @Final
    private GpuTexture texture;

    /**
     * @see net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams
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
    @Inject(method = "updateLightTexture(F)V", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LightTexture;updateLightTexture:Z", ordinal = 1), cancellable = true)
    private void injectCustomClearColor(float tickProgress, CallbackInfo ci) {
        if (ModuleCustomAmbience.CustomLightmap.INSTANCE.getRunning()) {
            RenderSystem.getDevice().createCommandEncoder()
                .clearColorTexture(this.texture, ModuleCustomAmbience.CustomLightmap.INSTANCE.getColor().toARGB());

            ci.cancel();
        }
    }

    /**
     * Target:
     * <pre>
     *     this.client.options.getGamma().getValue().floatValue()
     * </pre>
     */
    @ModifyExpressionValue(method = "updateLightTexture(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 2))
    private Object injectXRayFullBright(Object original) {
        // If fullBright is enabled, we need to return our own gamma value
        if (ModuleFullBright.FullBrightGamma.INSTANCE.getRunning()) {
            return ModuleFullBright.FullBrightGamma.INSTANCE.getGamma();
        }

        // Xray fullbright
        final ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getRunning() || !module.getFullBright()) {
            return original;
        }

        // They use .floatValue() afterward on the return value,
        // so we need to return a value which is not bigger than Float.MAX_VALUE
        return (double) Float.MAX_VALUE;
    }

    // Turns off blinking when the darkness effect is active.
    @Redirect(method = "updateLightTexture", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getEffectBlendFactor(Lnet/minecraft/core/Holder;F)F"))
    private float injectAntiDarkness(LocalPlayer instance, Holder<MobEffect> registryEntry, float v) {
        if (!ModuleAntiBlind.canRender(DoRender.DARKNESS) && registryEntry == MobEffects.DARKNESS) {
            return 0f;
        }

        return instance.getEffectBlendFactor(registryEntry, v);
    }

}
