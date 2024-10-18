/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.AutoBlock;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimations;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.UseAction;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER))
    private void hookRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (ModuleAnimations.INSTANCE.getEnabled()) {
            if (Hand.MAIN_HAND == hand && ModuleAnimations.MainHand.INSTANCE.getEnabled()) {
                matrices.translate(ModuleAnimations.MainHand.INSTANCE.getMainHandX(), ModuleAnimations.MainHand.INSTANCE.getMainHandY(), ModuleAnimations.MainHand.INSTANCE.getMainHandItemScale());
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ModuleAnimations.MainHand.INSTANCE.getMainHandPositiveX()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ModuleAnimations.MainHand.INSTANCE.getMainHandPositiveY()));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ModuleAnimations.MainHand.INSTANCE.getMainHandPositiveZ()));
            } else if (ModuleAnimations.OffHand.INSTANCE.getEnabled()) {
                matrices.translate(ModuleAnimations.OffHand.INSTANCE.getOffHandX(), ModuleAnimations.OffHand.INSTANCE.getOffHandY(), ModuleAnimations.OffHand.INSTANCE.getOffHandItemScale());
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ModuleAnimations.OffHand.INSTANCE.getOffHandPositiveX()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ModuleAnimations.OffHand.INSTANCE.getOffHandPositiveY()));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ModuleAnimations.OffHand.INSTANCE.getOffHandPositiveZ()));
            }
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void hideShield(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                                Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                CallbackInfo ci) {
        if ((ModuleSwordBlock.INSTANCE.handleEvents() || AutoBlock.INSTANCE.getBlockVisual())
                && hand == Hand.OFF_HAND
                && item.getItem() instanceof ShieldItem
                && (player.getMainHandStack().getItem() instanceof SwordItem
                || ModuleSwordBlock.INSTANCE.handleEvents() && ModuleSwordBlock.INSTANCE.getAlwaysHideShield())
        ) ci.cancel();
    }

    @Redirect(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;",
            ordinal = 0
    ))
    private UseAction hookUseAction(ItemStack instance) {
        var item = instance.getItem();
        if (item instanceof SwordItem && AutoBlock.INSTANCE.getBlockVisual()) {
            return UseAction.BLOCK;
        }

        return instance.getUseAction();
    }

    @Redirect(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isUsingItem()Z",
            ordinal = 1
    ))
    private boolean hookIsUseItem(AbstractClientPlayerEntity instance) {
        var item = instance.getMainHandStack().getItem();

        if (item instanceof SwordItem && AutoBlock.INSTANCE.getBlockVisual()) {
            return true;
        }

        return instance.isUsingItem();
    }

    @Redirect(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getActiveHand()Lnet/minecraft/util/Hand;",
            ordinal = 1
    ))
    private Hand hookActiveHand(AbstractClientPlayerEntity instance) {
        var item = instance.getMainHandStack().getItem();

        if (item instanceof SwordItem && AutoBlock.INSTANCE.getBlockVisual()) {
            return Hand.MAIN_HAND;
        }

        return instance.getActiveHand();
    }

    @Redirect(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getItemUseTimeLeft()I",
            ordinal = 2
    ))
    private int hookItemUseItem(AbstractClientPlayerEntity instance) {
        var item = instance.getMainHandStack().getItem();

        if (item instanceof SwordItem && AutoBlock.INSTANCE.getBlockVisual()) {
            return 7200;
        }

        return instance.getItemUseTimeLeft();
    }

    @ModifyArg(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
            ordinal = 4
    ), index = 2)
    private float applyEquipOffset(float equipProgress) {
        if (ModuleAnimations.INSTANCE.getEnabled() && !ModuleAnimations.INSTANCE.getEquipOffset()) {
            return 0.0F;
        }

        return equipProgress;
    }

    @Inject(method = "renderFirstPersonItem",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V", ordinal = 2, shift = At.Shift.AFTER))
    private void transformLegacyBlockAnimations(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                                Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                CallbackInfo ci) {
        var shouldAnimate = ModuleSwordBlock.INSTANCE.getEnabled() || AutoBlock.INSTANCE.getBlockVisual();

        if (shouldAnimate && item.getItem() instanceof SwordItem) {
            final Arm arm = (hand == Hand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();

            if (ModuleAnimations.INSTANCE.getEnabled()) {
                var activeChoice = ModuleAnimations.INSTANCE.getBlockAnimationChoice().getActiveChoice();

                activeChoice.transform(matrices, arm, equipProgress, swingProgress);
                return;
            }

            // Default animation
            ModuleAnimations.OneSevenAnimation.INSTANCE.transform(matrices, arm, equipProgress, swingProgress);
        }
    }

}
