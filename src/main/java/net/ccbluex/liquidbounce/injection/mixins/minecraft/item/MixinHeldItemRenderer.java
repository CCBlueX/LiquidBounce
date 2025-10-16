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
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.KillAuraAutoBlock;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimations;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleSilentHotbar;
import net.ccbluex.liquidbounce.utils.client.SilentHotbar;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.ModelTransformationMode;
import net.minecraft.item.consume.UseAction;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Final
    @Shadow
    private MinecraftClient client;

    @Shadow
    @Final
    private static float EQUIP_OFFSET_TRANSLATE_Y;

    @ModifyArgs(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderItem(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/item/ItemStack;Lnet/minecraft/item/ModelTransformationMode;ZLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V"))
    private void hookRenderFirstPersonItem(Args args) {
        if (!ModuleAnimations.INSTANCE.getRunning()) return;

        ItemStack item = args.get(1);
        ModelTransformationMode mode = args.get(2);
        MatrixStack matrices = args.get(4);
        ModuleAnimations.HandConfigurable mainHand = ModuleAnimations.INSTANCE.getMainHand();
        ModuleAnimations.HandConfigurable offHand = ModuleAnimations.INSTANCE.getOffHand();

        // Current exceptions: Trident, crossbow
        if (item.isOf(Items.TRIDENT) || item.isOf(Items.CROSSBOW)) return;

        // Apply normal transformations
        if (mode == ModelTransformationMode.FIRST_PERSON_RIGHT_HAND && mainHand.getRunning()) {
            liquid_bounce$applyTransformations(matrices, mainHand.getX(), mainHand.getY(), mainHand.getZ(),
                    mainHand.getPositiveX(), mainHand.getPositiveY(), mainHand.getPositiveZ(),
                    mainHand.getItemScale());
        } else if (mode == ModelTransformationMode.FIRST_PERSON_LEFT_HAND && offHand.getRunning()) {
            liquid_bounce$applyTransformations(matrices, offHand.getX(), offHand.getY(), offHand.getZ(),
                    offHand.getPositiveX(), offHand.getPositiveY(), offHand.getPositiveZ(),
                    offHand.getItemScale());
        }
    }

    @ModifyArgs(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;renderArmHoldingItem(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;IFFLnet/minecraft/util/Arm;)V"))
    private void hookRenderFirstPersonArm(Args args) {
        if (!ModuleAnimations.INSTANCE.getRunning()) return;
        MatrixStack matrices = args.get(0);
        ModuleAnimations.HandConfigurable arm = ModuleAnimations.INSTANCE.getArm();
        /*
         * Arm scale does not properly work
         * so we are force-setting it as 1.
         * You can still use arm.getZ()
         * to get a similar scaling result
         */
        if (arm.getRunning()) {
            liquid_bounce$applyTransformations(matrices, arm.getX(), arm.getY(), arm.getZ(),
                    arm.getPositiveX(), arm.getPositiveY(), arm.getPositiveZ(),
                    1);
        }
    }

    @Unique
    private static void liquid_bounce$applyTransformations(MatrixStack matrices, float translateX, float translateY, float translateZ, float rotateX, float rotateY, float rotateZ, float scaleXYZ) {
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotateX));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotateY));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotateZ));
        matrices.scale(scaleXYZ, scaleXYZ, scaleXYZ);
        matrices.translate(translateX, translateY, translateZ);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void hideShield(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                                Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                CallbackInfo ci) {
        if (hand == Hand.OFF_HAND && ModuleSwordBlock.INSTANCE.shouldHideOffhand(player, item)) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/item/consume/UseAction;",
            ordinal = 0
    ))
    private UseAction hookUseAction(ItemStack instance) {
        if (instance.isIn(ItemTags.SWORDS) && KillAuraAutoBlock.INSTANCE.getBlockVisual()) {
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
        var itemStack = instance.getMainHandStack();

        if (itemStack.isIn(ItemTags.SWORDS) && KillAuraAutoBlock.INSTANCE.getBlockVisual()) {
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
        var itemStack = instance.getMainHandStack();

        if (itemStack.isIn(ItemTags.SWORDS) && KillAuraAutoBlock.INSTANCE.getBlockVisual()) {
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
      var itemStack = instance.getMainHandStack();

      if (itemStack.isIn(ItemTags.SWORDS) && KillAuraAutoBlock.INSTANCE.getBlockVisual()) {
            return 7200;
        }

        return instance.getItemUseTimeLeft();
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

    /**
     * This transformation was previously a VFP option but got now added to minecraft directly.
     * View the code that was used to disable the VFP option here:
     * <a href="https://github.com/CCBlueX/LiquidBounce/blob/e5a0dbf5458b063d3028e69e04762b8b25b998b5/src/main/java/net/ccbluex/liquidbounce/utils/client/vfp/VfpCompatibility.java#L44">...</a>
     */
    @ModifyExpressionValue(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;"))
    private Item preventConflictingCode(Item item) {
        // only applies to sword items,
        // so that future items won't be affected if minecraft decides to actually make use out of this
        if (item.getDefaultStack().isIn(ItemTags.SWORDS)) {
            return Items.SHIELD; // makes the instanceof return true and therefore not do the transformation
        }

        return item;
    }

    @Inject(method = "renderFirstPersonItem",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/item/consume/UseAction;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V", ordinal = 2, shift = At.Shift.AFTER))
    private void transformLegacyBlockAnimations(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                                Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                CallbackInfo ci) {
        var shouldAnimate = ModuleSwordBlock.INSTANCE.getRunning() || KillAuraAutoBlock.INSTANCE.getBlockVisual();

        if (shouldAnimate && item.isIn(ItemTags.SWORDS)) {
            final Arm arm = (hand == Hand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();

            if (ModuleAnimations.INSTANCE.getRunning()) {
                var activeChoice = ModuleAnimations.INSTANCE.getBlockAnimationChoice().getActiveChoice();

                activeChoice.transform(matrices, arm, equipProgress, swingProgress);
                return;
            }

            // Default animation
            ModuleAnimations.OneSevenAnimation.INSTANCE.transform(matrices, arm, equipProgress, swingProgress);
        }
    }

    @ModifyExpressionValue(method = "updateHeldItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getMainHandStack()Lnet/minecraft/item/ItemStack;"))
    private ItemStack injectSilentHotbar(ItemStack original) {
        if (ModuleSilentHotbar.INSTANCE.getRunning()) {
            // noinspection DataFlowIssue
            return client.player.getInventory().main.get(SilentHotbar.INSTANCE.getClientsideSlot());
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateHeldItems", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getAttackCooldownProgress(F)F"))
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
}
