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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleTridentBoost;
import net.minecraft.world.item.TridentItem;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@NullMarked
@Mixin(TridentItem.class)
public abstract class MixinTridentItem {

    @ModifyExpressionValue(
        method = {"use", "releaseUsing"},
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;isInWaterOrRain()Z")
    )
    private boolean hookRiptideWaterRequirement(boolean original) {
        return original || ModuleTridentBoost.ignoresWaterRequirement();
    }

    @ModifyArgs(
        method = "releaseUsing",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;push(DDD)V")
    )
    private void hookRiptideImpulse(Args args) {
        args.set(0, ModuleTridentBoost.scaleHorizontal(args.<Double>get(0)));
        args.set(1, ModuleTridentBoost.scaleVertical(args.<Double>get(1)));
        args.set(2, ModuleTridentBoost.scaleHorizontal(args.<Double>get(2)));
    }

}
