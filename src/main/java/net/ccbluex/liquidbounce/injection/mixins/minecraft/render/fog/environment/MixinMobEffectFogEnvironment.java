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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.fog.environment;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.client.renderer.fog.environment.MobEffectFogEnvironment;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MobEffectFogEnvironment.class)
public abstract class MixinMobEffectFogEnvironment {

    @Shadow
    public abstract Holder<MobEffect> getMobEffect();

    @ModifyReturnValue(method = "isApplicable", at = @At("RETURN"))
    private boolean onIsApplicable(boolean original) {
        if (!original) {
            return false;
        }

        var effect = getMobEffect();

        if (effect == MobEffects.BLINDNESS) {
            return ModuleAntiBlind.canRender(DoRender.BLINDING);
        }
        if (effect == MobEffects.DARKNESS) {
            return ModuleAntiBlind.canRender(DoRender.DARKNESS);
        }

        return true;
    }
}
