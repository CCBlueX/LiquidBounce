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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Loader-specific companion of {@code minecraft.entity.MixinLocalPlayer}.
 * <p>
 * NeoForge patches the {@code Abilities#mayfly} read in {@code aiStep} into a
 * {@code mayFly()} call, so each loader hooks its own shape of the expression.
 */
@Mixin(LocalPlayer.class)
public abstract class MixinLocalPlayer {

    @ModifyExpressionValue(
        method = "aiStep",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;mayFly()Z"
        )
    )
    private boolean hookFreeCamPreventCreativeFly(boolean original) {
        return !ModuleFreeCam.INSTANCE.getRunning() && original;
    }

}
