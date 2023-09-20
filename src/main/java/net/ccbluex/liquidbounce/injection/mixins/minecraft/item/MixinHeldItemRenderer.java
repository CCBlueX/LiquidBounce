package net.ccbluex.liquidbounce.injection.mixins.minecraft.item;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAnimation;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
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
}
