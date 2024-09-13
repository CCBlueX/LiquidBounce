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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFullBright;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay;
import net.ccbluex.liquidbounce.interfaces.LightmapTextureManagerAddition;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.SimpleOption;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;
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
    private NativeImage image;

    @Shadow
    @Final
    private NativeImageBackedTexture texture;

    @Shadow
    private boolean dirty;
    @Unique
    private final int[] liquid_bounce$originalLightColor = new int[256];

    @Unique
    private short liquid_bounce$currentIndex = 0;

    @Unique
    private boolean liquid_bounce$dirty = false;

    @Redirect(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/SimpleOption;getValue()Ljava/lang/Object;", ordinal = 1))
    private Object injectXRayFullBright(SimpleOption option) {
        // If fullBright is enabled, we need to return our own gamma value
        if (ModuleFullBright.INSTANCE.getEnabled() && ModuleFullBright.FullBrightGamma.INSTANCE.isActive()) {
            return ModuleFullBright.FullBrightGamma.INSTANCE.getGamma();
        }

        // Xray fullbright
        final ModuleXRay module = ModuleXRay.INSTANCE;
        if (!module.getEnabled() || !module.getFullBright()) {
            return option.getValue();
        }

        // They use .floatValue() afterward on the return value, so we need to return a value which is not bigger than Float.MAX_VALUE
        return (double) Float.MAX_VALUE;
    }

    @Inject(method = "update(F)V", at = @At(value = "HEAD"))
    private void hookBlendTextureColors(float delta, CallbackInfo ci) {
        if (!dirty && ModuleCustomAmbience.INSTANCE.getEnabled() && ModuleCustomAmbience.CustomLightColor.INSTANCE.getEnabled()) {
            liquid_bounce$dirty = true;
            liquid_bounce$currentIndex = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    image.setColor(x, y, ModuleCustomAmbience.CustomLightColor.INSTANCE.blendWithLightColor(liquid_bounce$originalLightColor[liquid_bounce$currentIndex]));
                    liquid_bounce$currentIndex++;
                }
            }
            texture.upload();
        }
    }

    @Inject(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/option/GameOptions;getDarknessEffectScale()Lnet/minecraft/client/option/SimpleOption;"))
    private void hookResetIndex(float delta, CallbackInfo ci) {
        if (ModuleCustomAmbience.INSTANCE.getEnabled() && ModuleCustomAmbience.CustomLightColor.INSTANCE.getEnabled()) {
            liquid_bounce$dirty = true;
            liquid_bounce$currentIndex = 0;
        }
    }

    @ModifyArg(method = "update(F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/NativeImage;setColor(III)V"), index = 2)
    private int cacheAndModifyTextureColor(int color) {
        if (liquid_bounce$dirty) {
            liquid_bounce$originalLightColor[liquid_bounce$currentIndex] = color;
            liquid_bounce$currentIndex++;
            return ModuleCustomAmbience.CustomLightColor.INSTANCE.blendWithLightColor(color);
        }

        return color;
    }

    @Unique
    @Override
    public void liquid_bounce$restoreLightMap() {
        if (liquid_bounce$dirty) {
            liquid_bounce$dirty = false;
            liquid_bounce$currentIndex = 0;
            for (int y = 0; y < 16; y++) {
                for (int x = 0; x < 16; x++) {
                    image.setColor(x, y, liquid_bounce$originalLightColor[liquid_bounce$currentIndex]);
                    liquid_bounce$currentIndex++;
                }
            }
            texture.upload();
        }
    }

    // Turns off blinking when the darkness effect is active.
    @Redirect(method = "getDarknessFactor", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getStatusEffect(Lnet/minecraft/registry/entry/RegistryEntry;)Lnet/minecraft/entity/effect/StatusEffectInstance;"))
    private StatusEffectInstance injectAntiDarkness(ClientPlayerEntity instance, RegistryEntry<StatusEffect> registryEntry) {
        final var module = ModuleAntiBlind.INSTANCE;

        if (module.getEnabled() && module.getAntiDarkness()) {
            return null;
        }

        return instance.getStatusEffect(registryEntry);
    }

}
