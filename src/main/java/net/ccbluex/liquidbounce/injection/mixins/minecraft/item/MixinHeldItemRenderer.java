package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.AutoBlock;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimation;
import net.ccbluex.liquidbounce.utils.client.ProtocolUtilKt;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class MixinHeldItemRenderer {

    @Inject(method = "renderFirstPersonItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;push()V", shift = At.Shift.AFTER))
    private void hookRenderFirstPersonItem(AbstractClientPlayerEntity player, float tickDelta, float pitch, Hand hand, float swingProgress, ItemStack item, float equipProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (ModuleAnimation.INSTANCE.getEnabled()) {
            if (Hand.MAIN_HAND == hand && ModuleAnimation.MainHand.INSTANCE.getEnabled()) {
                matrices.translate(ModuleAnimation.MainHand.INSTANCE.getMainHandX(), ModuleAnimation.MainHand.INSTANCE.getMainHandY(), ModuleAnimation.MainHand.INSTANCE.getMainHandItemScale());
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ModuleAnimation.MainHand.INSTANCE.getMainHandPositiveX()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ModuleAnimation.MainHand.INSTANCE.getMainHandPositiveY()));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ModuleAnimation.MainHand.INSTANCE.getMainHandPositiveZ()));
            } else if (ModuleAnimation.OffHand.INSTANCE.getEnabled()) {
                matrices.translate(ModuleAnimation.OffHand.INSTANCE.getOffHandX(), ModuleAnimation.OffHand.INSTANCE.getOffHandY(), ModuleAnimation.OffHand.INSTANCE.getOffHandItemScale());
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(ModuleAnimation.OffHand.INSTANCE.getOffHandPositiveX()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(ModuleAnimation.OffHand.INSTANCE.getOffHandPositiveY()));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(ModuleAnimation.OffHand.INSTANCE.getOffHandPositiveZ()));
            }
        }
    }

    @Shadow
    protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void hideShield(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                                Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                CallbackInfo ci) {
        if (ModuleSwordBlock.INSTANCE.getEnabled() && hand == Hand.OFF_HAND && item.getItem() instanceof ShieldItem &&
                !player.getStackInHand(Hand.MAIN_HAND).isEmpty()
                && player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof SwordItem) {
            ci.cancel();
        }
    }

    @Redirect(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;",
            ordinal = 0
    ))
    private UseAction hookUseAction(ItemStack instance) {
        var item = instance.getItem();
        if (item instanceof SwordItem && ModuleKillAura.INSTANCE.getEnabled() &&
                AutoBlock.INSTANCE.getEnabled() &&
                AutoBlock.INSTANCE.getVisualBlocking()) {
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

        if (item instanceof SwordItem && ModuleKillAura.INSTANCE.getEnabled() && AutoBlock.INSTANCE.getEnabled() &&
                AutoBlock.INSTANCE.getVisualBlocking()) {
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

        if (item instanceof SwordItem && ModuleKillAura.INSTANCE.getEnabled() && AutoBlock.INSTANCE.getEnabled() &&
                AutoBlock.INSTANCE.getVisualBlocking()) {
            return Hand.MAIN_HAND;
        }

        return instance.getActiveHand();
    }

    @Redirect(method = "renderFirstPersonItem", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/network/AbstractClientPlayerEntity;getItemUseTimeLeft()I",
            ordinal = 1
    ))
    private int hookItemUseItem(AbstractClientPlayerEntity instance) {
        var item = instance.getMainHandStack().getItem();

        if (item instanceof SwordItem && ModuleKillAura.INSTANCE.getEnabled() && AutoBlock.INSTANCE.getEnabled() &&
                AutoBlock.INSTANCE.getVisualBlocking()) {
            return 7200;
        }

        return instance.getItemUseTimeLeft();
    }



    /**
     * Taken from ViaFabricPlus
     *
     * https://github.com/ViaVersion/ViaFabricPlus/blob/b9cecbfbf3a20e350d075159ebe70bc45c8f962e/src/main/java/de/florianmichael/viafabricplus/injection/mixin/fixes/minecraft/item/MixinHeldItemRenderer.java#L53-L66
     */
    @Inject(method = "renderFirstPersonItem",
            slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;getUseAction()Lnet/minecraft/util/UseAction;")),
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/item/HeldItemRenderer;applyEquipOffset(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/util/Arm;F)V", ordinal = 2, shift = At.Shift.AFTER))
    private void transformLegacyBlockAnimations(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                                Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                                MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                                CallbackInfo ci) {
        if (ModuleSwordBlock.INSTANCE.getEnabled() && item.getItem() instanceof SwordItem) {
            // If is old combat we do not want to translate the item because ViaFabricPlus already does that
            final boolean isOldCombat = ProtocolUtilKt.isOldCombat();

            if (!isOldCombat) {
                matrices.translate(-0.1F, 0.05F, 0.0F);
            }

            // But this is still needed - because ViaFabricPlus does not do that
            final Arm arm = (hand == Hand.MAIN_HAND) ? player.getMainArm() : player.getMainArm().getOpposite();
            applySwingOffset(matrices, arm, swingProgress);

            if (!isOldCombat) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-102.25f));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(13.365f));
                matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(78.05f));
            }
        }
    }

}
