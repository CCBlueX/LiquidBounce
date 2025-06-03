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

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.BackgroundRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Fog;
import net.minecraft.entity.effect.StatusEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.List;
import java.util.stream.Stream;

@Mixin(BackgroundRenderer.class)
public abstract class MixinBackgroundRenderer {

    @Shadow
    private static boolean fogEnabled;

    @Redirect(method = "getFogModifier", at = @At(value = "INVOKE", target = "Ljava/util/List;stream()Ljava/util/stream/Stream;"))
    private static Stream<BackgroundRenderer.StatusEffectFogModifier> injectAntiBlind(List<BackgroundRenderer.StatusEffectFogModifier> list) {
        return list.stream().filter(modifier -> {
            final var effect = modifier.getStatusEffect();

            if (!ModuleAntiBlind.INSTANCE.getRunning()) {
                return true;
            }

            return !(StatusEffects.BLINDNESS == effect && !ModuleAntiBlind.canRender(DoRender.BLINDING)) ||
                    (StatusEffects.DARKNESS == effect && !ModuleAntiBlind.canRender(DoRender.DARKNESS));
        });
    }

    @ModifyReturnValue(method = "applyFog", at = @At("RETURN"))
    private static Fog injectFog(Fog original, @Local(argsOnly = true) Camera camera, @Local(argsOnly = true, ordinal = 0) float viewDistance) {
        var customAmbienceFog = ModuleCustomAmbience.FogConfigurable.INSTANCE;
        if (!ModuleAntiBlind.INSTANCE.getRunning() || customAmbienceFog.getRunning() || !fogEnabled) {
            return ModuleCustomAmbience.FogConfigurable.INSTANCE.modifyFog(camera, viewDistance, original);
        }

        CameraSubmersionType type = camera.getSubmersionType();

        if (!ModuleAntiBlind.canRender(DoRender.POWDER_SNOW_FOG) && type == CameraSubmersionType.POWDER_SNOW) {
            return new Fog(-8f, viewDistance * 0.5f, original.shape(), original.red(), original.green(), original.blue(), original.alpha());
        }

        if (!ModuleAntiBlind.canRender(DoRender.LIQUIDS_FOG)) {
            // Renders fog same as spectator.
            switch (type) {
                case LAVA -> {
                    return new Fog(-8f, viewDistance * 0.5f, original.shape(), original.red(), original.green(), original.blue(), original.alpha());
                }

                case WATER -> {
                    return new Fog(-8f, viewDistance, original.shape(), original.red(), original.green(), original.blue(), original.alpha());
                }
            }
        }

        return original;
    }

}
