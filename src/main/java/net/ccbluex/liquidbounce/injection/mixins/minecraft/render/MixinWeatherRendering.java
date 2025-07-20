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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCustomAmbience;
import net.minecraft.client.render.WeatherRendering;
import net.minecraft.world.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(WeatherRendering.class)
public abstract class MixinWeatherRendering {

    @ModifyExpressionValue(method = "addParticlesAndSound", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/world/ClientWorld;getRainGradient(F)F"))
    private float ambientPrecipitation2(float original) {
        var moduleCustomAmbience = ModuleCustomAmbience.INSTANCE;
        if (moduleCustomAmbience.getRunning() && moduleCustomAmbience.getWeather().get() == ModuleCustomAmbience.WeatherType.SNOWY) {
            return 0f;
        }

        return original;
    }

    @ModifyVariable(method = "renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V", at = @At(value = "STORE"), ordinal = 1)
    private int modifyPrecipitationLayers(int original) {
        var precipitation = ModuleCustomAmbience.Precipitation.INSTANCE;
        if (precipitation.getRunning()) {
            return precipitation.getLayers();
        }

        return original;
    }

    @ModifyExpressionValue(method = "renderPrecipitation(Lnet/minecraft/world/World;Lnet/minecraft/client/render/VertexConsumerProvider;IFLnet/minecraft/util/math/Vec3d;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;getRainGradient(F)F"))
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
