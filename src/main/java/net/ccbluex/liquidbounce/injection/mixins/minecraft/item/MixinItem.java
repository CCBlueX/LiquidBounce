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
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Item.class)
public abstract class MixinItem {

    @ModifyExpressionValue(method = "getPlayerPOVHitResult", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;calculateViewVector(FF)Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 hookFixRotation(Vec3 original, Level world, Player player, ClipContext.Fluid fluidHandling) {
        if (player == Minecraft.getInstance().player) {
            var rotation = RotationManager.INSTANCE.getCurrentRotation();
            if (rotation != null) {
                return rotation.directionVector();
            }
        }

        return original;
    }

}
