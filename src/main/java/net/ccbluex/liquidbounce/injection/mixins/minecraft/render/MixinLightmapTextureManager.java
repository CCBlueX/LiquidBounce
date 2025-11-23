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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTextureView;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.interfaces.LightmapTextureManagerAddition;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LightmapTextureManager.class)
public abstract class MixinLightmapTextureManager implements LightmapTextureManagerAddition {

    @Shadow
    @Final
    private GpuTextureView glTextureView;

    @Unique
    private boolean liquid_bounce$customLightMap = false;

    @ModifyExpressionValue(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 1))
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

    @Inject(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiler/Profiler;pop()V"))
    private void hookBlendTextureColors(float delta, CallbackInfo ci) {
        var lightColor = ModuleCustomAmbience.CustomLightColor.INSTANCE;
        if (lightColor.getRunning()) {
            lightColor.update();
        }
    }

    @Inject(method = "update(F)V", at = @At(value = "HEAD"))
    private void hookResetIndex(float delta, CallbackInfo ci) {
        var customLightColor = ModuleCustomAmbience.CustomLightColor.INSTANCE;
        if (customLightColor.getRunning()) {
            liquid_bounce$customLightMap = true;
            if (RenderSystem.getShaderTexture(2) == this.glTextureView) {
                RenderSystem.setShaderTexture(2, customLightColor.getTextureView());
            }
        }
    }

    @ModifyArg(
        method = "enable",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILcom/mojang/blaze3d/textures/GpuTextureView;)V"
        ),
        index = 1
    )
    private @Nullable GpuTextureView hookSpoof(@Nullable GpuTextureView texture) {
        return liquid_bounce$customLightMap
            ? ModuleCustomAmbience.CustomLightColor.INSTANCE.getTextureView()
            : texture;
    }

    @Override
    public void liquid_bounce$restoreLightMap() {
        if (RenderSystem.getShaderTexture(2) == ModuleCustomAmbience.CustomLightColor.INSTANCE.getTextureView()) {
            RenderSystem.setShaderTexture(2, this.glTextureView);
        }
        liquid_bounce$customLightMap = false;
    }

    // Turns off blinking when the darkness effect is active.
    @Redirect(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getEffectFadeFactor(Lnet/minecraft/registry/entry/RegistryEntry;F)F"))
    private float injectAntiDarkness(ClientPlayerEntity instance, RegistryEntry<StatusEffect> registryEntry, float v) {
        if (!ModuleAntiBlind.canRender(DoRender.DARKNESS) && registryEntry == StatusEffects.DARKNESS) {
            return 0f;
        }

        return instance.getEffectFadeFactor(registryEntry, v);
    }

}
