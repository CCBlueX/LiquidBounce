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
package net.ccbluex.liquidbounce.injection.mixins.neoforge;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Loader-specific companion of {@code minecraft.render.MixinWeatherEffectRenderer}.
 * <p>
 * NeoForge patches the body of {@code render} into an added overload with a
 * trailing {@code LevelRenderState} parameter, so each loader hooks its own
 * shape of the method.
 */
@Mixin(WeatherEffectRenderer.class)
public abstract class MixinWeatherEffectRenderer {

    @ModifyExpressionValue(
        method = "render(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/client/renderer/state/level/WeatherRenderState;Lnet/minecraft/client/renderer/state/level/LevelRenderState;)V",
        at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/level/WeatherRenderState;intensity:F", opcode = Opcodes.GETFIELD)
    )
    private float modifyPrecipitationGradient(float original) {
        var precipitation = ModuleCustomAmbience.Precipitation.INSTANCE;
        if (precipitation.getRunning() && original != 0f) {
            return precipitation.getGradient();
        }

        return original;
    }

}
