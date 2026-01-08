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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(WeatherEffectRenderer.class)
public abstract class MixinWeatherEffectRenderer {

    @ModifyExpressionValue(method = "tickRainParticles", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getRainLevel(F)F"))
    private float ambientPrecipitation2(float original) {
        var moduleCustomAmbience = ModuleCustomAmbience.INSTANCE;
        if (moduleCustomAmbience.getRunning() && moduleCustomAmbience.getWeather().get() == ModuleCustomAmbience.WeatherType.SNOWY) {
            return 0f;
        }

        return original;
    }

    @ModifyExpressionValue(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/state/WeatherRenderState;intensity:F"))
    private float modifyPrecipitationGradient(float original) {
        var precipitation = ModuleCustomAmbience.Precipitation.INSTANCE;
        if (precipitation.getRunning() && original != 0f) {
            return precipitation.getGradient();
        }

        return original;
    }

    @ModifyReturnValue(method = "getPrecipitationAt", at = @At(value = "RETURN", ordinal = 1))
    private Biome.Precipitation modifyBiomePrecipitation(Biome.Precipitation original) {
        var moduleOverrideWeather = ModuleCustomAmbience.INSTANCE;
        if (moduleOverrideWeather.getRunning() && moduleOverrideWeather.getWeather().get() == ModuleCustomAmbience.WeatherType.SNOWY) {
            return Biome.Precipitation.SNOW;
        }

        return original;
    }

}
