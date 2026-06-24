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
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimations;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSilentHotbar;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.ccbluex.liquidbounce.utils.item.ItemCategorizationsKt;
import net.ccbluex.liquidbounce.utils.render.FirstPersonShieldTint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ShieldItem;
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

@Mixin(ItemInHandRenderer.class)
public abstract class MixinItemInHandRenderer {

    @Final
    @Shadow
    private Minecraft minecraft;

    @Shadow
    private ItemStack offHandItem;

    @Shadow
    @Final
    private static float ITEM_POS_Y;

    @WrapOperation(method = "renderItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/item/ItemStackRenderState;submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;III)V"))
    private void hookFirstPersonShieldTint(
        ItemStackRenderState instance, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords,
        int overlayCoords, int outlineColor, Operation<Void> original, LivingEntity mob, ItemStack itemStack,
        ItemDisplayContext type
    ) {
        if (itemStack.getItem() instanceof ShieldItem && type.firstPerson()) {
            FirstPersonShieldTint.render(
                () -> original.call(instance, poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor));
            return;
        }

        original.call(instance, poseStack, submitNodeCollector, lightCoords, overlayCoords, outlineColor);
    }

    @Inject(method = "submitArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
    private void hookRenderFirstPersonItem(
        AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (ModuleAnimations.INSTANCE.getRunning()) {
            var isInBothHands = InteractionHand.MAIN_HAND == hand && itemStack.has(DataComponents.MAP_ID) && offHandItem.isEmpty();
            ModuleAnimations.MainHand mainHand = ModuleAnimations.MainHand.INSTANCE;
            ModuleAnimations.OffHand offHand = ModuleAnimations.OffHand.INSTANCE;
            if (isInBothHands && mainHand.getRunning() && offHand.getRunning()) {
                liquid_bounce$applyTransformations(poseStack,
                        (mainHand.getMainHandX() + offHand.getOffHandX()) / 2f,
                        (mainHand.getMainHandY() + offHand.getOffHandY()) / 2f,
                        (mainHand.getMainHandItemScale() + offHand.getOffHandItemScale()) / 2f,
                        (mainHand.getMainHandPositiveX() + offHand.getOffHandPositiveX()) / 2f,
                        (mainHand.getMainHandPositiveY() + offHand.getOffHandPositiveY()) / 2f,
                        (mainHand.getMainHandPositiveZ() + offHand.getOffHandPositiveZ()) / 2f
                );
            } else if (isInBothHands && mainHand.getRunning()) {
                poseStack.translate(0f, 0f, mainHand.getMainHandItemScale());
            } else if (InteractionHand.MAIN_HAND == hand && mainHand.getRunning()) {
                liquid_bounce$applyTransformations(poseStack, mainHand.getMainHandX(), mainHand.getMainHandY(), mainHand.getMainHandItemScale(), mainHand.getMainHandPositiveX(), mainHand.getMainHandPositiveY(), mainHand.getMainHandPositiveZ());
            } else if (offHand.getRunning()) {
                liquid_bounce$applyTransformations(poseStack, offHand.getOffHandX(), offHand.getOffHandY(), offHand.getOffHandItemScale(), offHand.getOffHandPositiveX(), offHand.getOffHandPositiveY(), offHand.getOffHandPositiveZ());
            }
        }
    }

    @Unique
    private static void liquid_bounce$applyTransformations(PoseStack matrices, float translateX, float translateY, float translateZ, float rotateX, float rotateY, float rotateZ) {
        matrices.translate(translateX, translateY, translateZ);
        matrices.mulPose(Axis.XP.rotationDegrees(rotateX));
        matrices.mulPose(Axis.YP.rotationDegrees(rotateY));
        matrices.mulPose(Axis.ZP.rotationDegrees(rotateZ));
    }

    @Inject(method = "submitArmWithItem",
        slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;")),
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V", ordinal = 0, shift = At.Shift.AFTER))
    private void transformBlockAnimation(
        AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (ItemCategorizationsKt.isSword(itemStack)) {
            var arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();

            if (ModuleAnimations.INSTANCE.getRunning()) {
                var activeChoice = ModuleAnimations.INSTANCE.getBlockAnimationChoice().getActiveMode();
                activeChoice.transform(poseStack, arm, inverseArmHeight, attack);
            } else {
                // Default animation
                ModuleAnimations.OneSevenAnimation.INSTANCE.transform(poseStack, arm, inverseArmHeight, attack);
            }
        }
    }

    @Inject(method = "submitArmWithItem", at = @At("HEAD"), cancellable = true)
    private void hideShield(AbstractClientPlayer player, float frameInterp, float xRot, InteractionHand hand, float attack, ItemStack itemStack, float inverseArmHeight, PoseStack poseStack, SubmitNodeCollector submitNodeCollector, int lightCoords, CallbackInfo ci) {
        if (hand == InteractionHand.OFF_HAND && player == Minecraft.getInstance().player &&
            ModuleSwordBlock.INSTANCE.shouldHideOffhand(itemStack)) {
            ci.cancel();
        }
    }

    @ModifyArg(method = "submitArmWithItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V",
            ordinal = 3
    ), index = 2)
    private float injectIgnoreBlocking(float equipProgress) {
        if (ModuleAnimations.EquipOffset.INSTANCE.getRunning() && ModuleAnimations.EquipOffset.INSTANCE.getIgnoreBlocking()) {
            return 0.0F;
        }

        return equipProgress;
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getMainHandItem()Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack injectSilentHotbar(ItemStack original) {
        if (ModuleSilentHotbar.INSTANCE.getRunning()) {
            // noinspection DataFlowIssue
            return minecraft.player.getInventory().getNonEquipmentItems().get(SilentHotbar.INSTANCE.getClientsideSlot());
        }

        return original;
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
    private float injectSilentHotbarNoCooldown(float original) {
        if (ModuleSilentHotbar.INSTANCE.getRunning() && ModuleSilentHotbar.INSTANCE.getNoCooldownProgress() && SilentHotbar.INSTANCE.isSlotModified()) {
            return 1f;
        }

        return original;
    }

    @Inject(method = "itemUsed", at = @At("HEAD"), cancellable = true)
    private void injectIgnorePlace(InteractionHand hand, CallbackInfo ci) {
        if (ModuleAnimations.INSTANCE.getRunning() && ModuleAnimations.EquipOffset.INSTANCE.getIgnorePlace()) {
            ci.cancel();
        }
    }

    @Inject(method = "shouldInstantlyReplaceVisibleItem", at = @At("RETURN"), cancellable = true)
    private void injectIgnoreAmount(ItemStack currentlyVisibleItem, ItemStack expectedItem, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleAnimations.INSTANCE.getRunning() && !cir.getReturnValueZ()) {
            cir.setReturnValue(!ModuleAnimations.EquipOffset.INSTANCE.getRunning()
                    || (currentlyVisibleItem.getCount() == expectedItem.getCount() || ModuleAnimations.EquipOffset.INSTANCE.getIgnoreAmount())
                    && ItemStack.isSameItemSameComponents(currentlyVisibleItem, expectedItem)
            );
        }
    }

    @ModifyArg(method = "applyItemArmTransform", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V"), index = 1)
    private float injectDisableEquipOffset(float y) {
        if (ModuleAnimations.INSTANCE.getRunning() && !ModuleAnimations.EquipOffset.INSTANCE.getRunning()) {
            return ITEM_POS_Y;
        }
        return y;
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/world/item/ItemStack;getUseAnimation()Lnet/minecraft/world/item/ItemUseAnimation;",
        ordinal = 0
    ))
    private ItemUseAnimation hookUseAction(ItemUseAnimation original, @Local(argsOnly = true, name = "itemStack") ItemStack itemStack, @Local(argsOnly = true, name = "player") AbstractClientPlayer player) {
        if (ModuleSwordBlock.shouldAnimateSwordBlock(player, itemStack)) {
            return ItemUseAnimation.BLOCK;
        }
        return original;
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;isUsingItem()Z",
        ordinal = 1
    ))
    private boolean hookIsUseItem(boolean original, @Local(argsOnly = true, name = "player") AbstractClientPlayer player) {
        if (ModuleSwordBlock.shouldAnimateSwordBlock(player)) {
            return true;
        }

        return original;
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;",
        ordinal = 1
    ))
    private InteractionHand hookActiveHand(InteractionHand original, @Local(argsOnly = true, name = "player") AbstractClientPlayer player) {
        if (ModuleSwordBlock.shouldAnimateSwordBlock(player)) {
            return InteractionHand.MAIN_HAND;
        }

        return original;
    }

    @ModifyExpressionValue(method = "submitArmWithItem", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/client/player/AbstractClientPlayer;getUseItemRemainingTicks()I",
        ordinal = 2
    ))
    private int hookItemUseItem(int original, @Local(argsOnly = true, name = "player") AbstractClientPlayer player) {
        if (ModuleSwordBlock.shouldAnimateSwordBlock(player)) {
            return 7200;
        }

        return original;
    }


}
