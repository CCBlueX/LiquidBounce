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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity;

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Avatar;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@NullMarked
@Mixin(AvatarRenderer.class)
public abstract class MixinAvatarRenderer {

    @Inject(method = "getArmPose(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/client/model/HumanoidModel$ArmPose;", at = @At("HEAD"), cancellable = true)
    private static void injectArmPose(
        Avatar player, ItemStack stack, InteractionHand hand, CallbackInfoReturnable<HumanoidModel.ArmPose> cir) {
        LocalPlayer localPlayer = Minecraft.getInstance().player;
        if (player == localPlayer
            && ModuleSwordBlock.INSTANCE.getApplyToThirdPersonView()
        ) {
            switch (hand) {
                case MAIN_HAND -> {
                    if (ModuleSwordBlock.shouldAnimateSwordBlock(localPlayer, stack)) {
                        cir.setReturnValue(HumanoidModel.ArmPose.BLOCK);
                    }
                }
                case OFF_HAND -> {
                    if (ModuleSwordBlock.INSTANCE.shouldHideOffhand()) {
                        cir.setReturnValue(HumanoidModel.ArmPose.EMPTY);
                    }
                }
            }
        }
    }

}
