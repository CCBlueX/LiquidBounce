/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCrystalView;
import net.minecraft.client.render.entity.model.EndCrystalEntityModel;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(EndCrystalEntityModel.class)
public abstract class MixinEndCrystalEntityModel {

    @ModifyExpressionValue(method = "setAngles(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EndCrystalEntityRenderer;getYOffset(F)F"))
    public float injectYOffsetMultiplier(float original) {
        var crystalView = ModuleCrystalView.INSTANCE;
        if (crystalView.getRunning()) {
            return original * crystalView.getBounce();
        }

        return original;
    }

    @ModifyVariable(method = "setAngles(Lnet/minecraft/client/render/entity/state/EndCrystalEntityRenderState;)V", at = @At(value = "STORE", opcode = Opcodes.FSTORE, ordinal = 0))
    public float injectSpinSpeedMultiplier(float original) {
        var crystalView = ModuleCrystalView.INSTANCE;
        if (crystalView.getRunning()) {
            return original * crystalView.getSpinSpeed();
        }

        return original;
    }

}
