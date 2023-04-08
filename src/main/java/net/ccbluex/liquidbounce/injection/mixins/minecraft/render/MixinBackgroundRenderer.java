/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

import com.mojang.blaze3d.systems.RenderSystem;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.interfaces.IMixinGameRenderer;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.BackgroundRenderer.FogType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.CameraSubmersionType;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.stream.Stream;

import static java.lang.Float.MAX_VALUE;
import static net.minecraft.client.render.CameraSubmersionType.LAVA;
import static net.minecraft.client.render.CameraSubmersionType.WATER;
import static org.spongepowered.asm.mixin.injection.At.Shift.AFTER;

@Mixin(BackgroundRenderer.class)
public abstract class MixinBackgroundRenderer implements IMixinGameRenderer {

    @Redirect(method = "getFogModifier", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"))
    private static Stream<BackgroundRenderer.StatusEffectFogModifier> injectAntiBlind(List<BackgroundRenderer.StatusEffectFogModifier> list) {
        return list.stream().filter(modifier -> {
            final StatusEffect effect = modifier.getStatusEffect();

            return !(effect == StatusEffects.BLINDNESS && ModuleAntiBlind.INSTANCE.getEnabled() && ModuleAntiBlind.INSTANCE.getAntiBlind());
        });
    }

    @Inject(method = "applyFog", at = @At(value = "INVOKE", shift = AFTER, ordinal = 0, target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogStart(F)V", remap = false))
    private static void injectLiquidsFog(Camera camera, FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo callback) {
        if (isLiquidsFogEnabled(camera)) {
            RenderSystem.setShaderFogStart(MAX_VALUE);
            RenderSystem.setShaderFogEnd(MAX_VALUE);
        }
    }

    @Inject(method = "applyFog", at = @At(value = "INVOKE", shift = AFTER, ordinal = 0, target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderFogEnd(F)V", remap = false))
    private static void injectLiquidsFogUnderwater(Camera camera, FogType fogType, float viewDistance, boolean thickFog, float tickDelta, CallbackInfo info) {
        if (isLiquidsFogEnabled(camera)) {
            RenderSystem.setShaderFogEnd(MAX_VALUE);
        }
    }

    @Unique
    private static boolean isLiquidsFogEnabled(Camera camera) {
        ModuleAntiBlind module = ModuleAntiBlind.INSTANCE;
        if (module.getEnabled() && module.getLiquidsFog()) {
            CameraSubmersionType type = camera.getSubmersionType();
            return LAVA == type || WATER == type;
        }
        return false;
    }
}
