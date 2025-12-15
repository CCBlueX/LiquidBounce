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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimations;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSilentHotbar;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.ccbluex.liquidbounce.utils.item.ItemCategorizationsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    private ItemStack offHand;

    @Shadow
    @Final
    private static float EQUIP_OFFSET_TRANSLATE_Y;

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER))
    private void hookRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (ModuleAnimations.INSTANCE.getRunning()) {
            var isInBothHands = Hand.MAIN_HAND == hand && item.contains(DataComponentTypes.MAP_ID) && offHand.isEmpty();
            ModuleAnimations.MainHand mainHand = ModuleAnimations.MainHand.INSTANCE;
            ModuleAnimations.OffHand offHand = ModuleAnimations.OffHand.INSTANCE;
            if (isInBothHands && mainHand.getRunning() && offHand.getRunning()) {
                liquid_bounce$applyTransformations(matrices,
                        (mainHand.getMainHandX() + offHand.getOffHandX()) / 2f,
                        (mainHand.getMainHandY() + offHand.getOffHandY()) / 2f,
                        (mainHand.getMainHandItemScale() + offHand.getOffHandItemScale()) / 2f,
                        (mainHand.getMainHandPositiveX() + offHand.getOffHandPositiveX()) / 2f,
                        (mainHand.getMainHandPositiveY() + offHand.getOffHandPositiveY()) / 2f,
                        (mainHand.getMainHandPositiveZ() + offHand.getOffHandPositiveZ()) / 2f
                );
            } else if (isInBothHands && mainHand.getRunning()) {
                matrices.translate(0f, 0f, mainHand.getMainHandItemScale());
            } else if (Hand.MAIN_HAND == hand && mainHand.getRunning()) {
                liquid_bounce$applyTransformations(matrices, mainHand.getMainHandX(), mainHand.getMainHandY(), mainHand.getMainHandItemScale(), mainHand.getMainHandPositiveX(), mainHand.getMainHandPositiveY(), mainHand.getMainHandPositiveZ());
            } else if (offHand.getRunning()) {
                liquid_bounce$applyTransformations(matrices, offHand.getOffHandX(), offHand.getOffHandY(), offHand.getOffHandItemScale(), offHand.getOffHandPositiveX(), offHand.getOffHandPositiveY(), offHand.getOffHandPositiveZ());
            }
        }
    }

    @Unique
    private static void liquid_bounce$applyTransformations(MatrixStack matrices, float translateX, float translateY, float translateZ, float rotateX, float rotateY, float rotateZ) {
        matrices.translate(translateX, translateY, translateZ);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotateX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotateY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotateZ));
    }

    @Inject(method = "renderFirstPersonItem",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/item/consume/UseAction;")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V", ordinal = 0, shift = At.Shift.AFTER))
    private void transformBlockAnimation(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (ItemCategorizationsKt.isSword(item)) {
            var arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();

            if (ModuleAnimations.INSTANCE.getRunning()) {
                var activeChoice = ModuleAnimations.INSTANCE.getBlockAnimationChoice().getActiveChoice();
                activeChoice.transform(matrices, arm, equipProgress, swingProgress);
            } else {
                // Default animation
                ModuleAnimations.OneSevenAnimation.INSTANCE.transform(matrices, arm, equipProgress, swingProgress);
            }
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void hideShield(AbstractClientPlayerEntity player, float tickProgress, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, OrderedRenderCommandQueue orderedRenderCommandQueue, int light, CallbackInfo ci) {
        if (hand == Hand.OFF_HAND && player == MinecraftClient.getInstance().player &&
            ModuleSwordBlock.INSTANCE.shouldHideOffhand(item)) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V",
            ordinal = 3
    ), index = 2)
    private float injectIgnoreBlocking(float equipProgress) {
        if (ModuleAnimations.EquipOffset.INSTANCE.getRunning() && ModuleAnimations.EquipOffset.INSTANCE.getIgnoreBlocking()) {
            return 0.0F;
        }

        return equipProgress;
    }

    @ModifyExpressionValue(method = "updateHeldItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack injectSilentHotbar(ItemStack original) {
        if (ModuleSilentHotbar.INSTANCE.getRunning()) {
            // noinspection DataFlowIssue
            return client.player.getInventory().getMainStacks().get(SilentHotbar.INSTANCE.getClientsideSlot());
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateHeldItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getHandEquippingProgress(F)F"))
    private float injectSilentHotbarNoCooldown(float original) {
        if (ModuleSilentHotbar.INSTANCE.getRunning() && ModuleSilentHotbar.INSTANCE.getNoCooldownProgress() && SilentHotbar.INSTANCE.isSlotModified()) {
            return 1f;
        }

        return original;
    }

    @Inject(method = "resetEquipProgress", at = @At("HEAD"), cancellable = true)
    private void injectIgnorePlace(Hand hand, CallbackInfo ci) {
        if (ModuleAnimations.INSTANCE.getRunning() && ModuleAnimations.EquipOffset.INSTANCE.getIgnorePlace()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldSkipHandAnimationOnSwap", at = @At("RETURN"), cancellable = true)
    private void injectIgnoreAmount(ItemStack from, ItemStack to, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleAnimations.INSTANCE.getRunning() && !cir.getReturnValueZ()) {
            cir.setReturnValue(!ModuleAnimations.EquipOffset.INSTANCE.getRunning()
                    || (from.getCount() == to.getCount() || ModuleAnimations.EquipOffset.INSTANCE.getIgnoreAmount())
                    && ItemStack.areItemsAndComponentsEqual(from, to)
            );
        }
    }

    @ModifyArg(method = "applyEquipOffset", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V"), index = 1)
    private float injectDisableEquipOffset(float y) {
        if (ModuleAnimations.INSTANCE.getRunning() && !ModuleAnimations.EquipOffset.INSTANCE.getRunning()) {
            return EQUIP_OFFSET_TRANSLATE_Y;
        }
        return y;
    }

    /**
     * Determines if the sword block animation should be applied no matter if we
     * are actually blocking.
     */
    @Unique
    private static boolean liquid_bounce$shouldAnimate(PlayerEntity player) {
        return ModuleSwordBlock.INSTANCE.getRunning() && ModuleSwordBlock.isBlockingWithOffhandShield(player)
            || KillAuraAutoBlock.INSTANCE.getBlockVisual();
    }

    @ModifyExpressionValue(method = "renderFirstPersonItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/item/consume/UseAction;",
        ordinal = 0
    ))
    private UseAction hookUseAction(UseAction original, @Local(argsOnly = true) ItemStack item) {
        if (item.isIn(ItemTags.SWORDS) && liquid_bounce$shouldAnimate(client.player)) {
            return UseAction.BLOCK;
        }
        return original;
    }

    @ModifyExpressionValue(method = "renderFirstPersonItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;isUsingItem()Z",
        ordinal = 1
    ))
    private boolean hookIsUseItem(boolean original, @Local(argsOnly = true) AbstractClientPlayerEntity entity) {
        var itemStack = entity.getMainHandStack();
        if (itemStack.isIn(ItemTags.SWORDS) && liquid_bounce$shouldAnimate(entity)) {
            return true;
        }

        return entity.isUsingItem();
    }

    @ModifyExpressionValue(method = "renderFirstPersonItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getActiveHand()Lnet/minecraft/util/Hand;",
        ordinal = 1
    ))
    private Hand hookActiveHand(Hand original, @Local(argsOnly = true) AbstractClientPlayerEntity entity) {
        var itemStack = entity.getMainHandStack();
        if (itemStack.isIn(ItemTags.SWORDS) && liquid_bounce$shouldAnimate(entity)) {
            return Hand.MAIN_HAND;
        }

        return entity.getActiveHand();
    }

    @ModifyExpressionValue(method = "renderFirstPersonItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getItemUseTimeLeft()I",
        ordinal = 2
    ))
    private int hookItemUseItem(int original, @Local(argsOnly = true) AbstractClientPlayerEntity entity) {
        var itemStack = entity.getMainHandStack();
        if (itemStack.isIn(ItemTags.SWORDS) && liquid_bounce$shouldAnimate(entity)) {
            return 7200;
        }

        return entity.getItemUseTimeLeft();
    }


}
