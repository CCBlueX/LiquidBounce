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
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ConsumableComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.SwordItem;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class MixinItem {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void hookSwordUse(World world, PlayerEntity user, Hand hand,
                              CallbackInfoReturnable<ActionResult> cir) {
        // Hooks sword use - only if main hand (otherwise this makes no sense on 1.8)
        if (((Object) this) instanceof SwordItem && ModuleSwordBlock.INSTANCE.getRunning() && !ModuleSwordBlock.INSTANCE.getOnlyVisual() && hand == Hand.MAIN_HAND) {
            var itemStack = user.getStackInHand(hand);
            user.setCurrentHand(hand);
            ConsumableComponent consumableComponent = itemStack.get(DataComponentTypes.CONSUMABLE);
            if (consumableComponent == null) {
                return;
            }

            cir.setReturnValue(consumableComponent.consume(user, itemStack, hand));
        }
    }

    @ModifyReturnValue(method = "getUseAction", at = @At("RETURN"))
    private UseAction hookSwordUseAction(UseAction original) {
        // Hooks sword use action
        if (((Object) this) instanceof SwordItem && ModuleSwordBlock.INSTANCE.getRunning() && !ModuleSwordBlock.INSTANCE.getOnlyVisual()) {
            return UseAction.BLOCK;
        }

        return original;
    }

    @ModifyReturnValue(method = "getMaxUseTime", at = @At("RETURN"))
    private int hookMaxUseTime(int original) {
        // Hooks sword max use time
        if (((Object) this) instanceof SwordItem && ModuleSwordBlock.INSTANCE.getRunning() && !ModuleSwordBlock.INSTANCE.getOnlyVisual()) {
            return 72000;
        }

        return original;
    }

    @ModifyExpressionValue(method = "raycast", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;"))
    private static Vec3d hookFixRotation(Vec3d original, World world, PlayerEntity player, RaycastContext.FluidHandling fluidHandling) {
        var rotation = RotationManager.INSTANCE.getCurrentRotation();

        if (player == MinecraftClient.getInstance().player && rotation != null) {
            return rotation.getDirectionVector();
        }

        return original;
    }

}
