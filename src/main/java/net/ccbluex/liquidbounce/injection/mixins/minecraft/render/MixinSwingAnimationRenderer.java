package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.render.animations.ModuleAnimations;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public abstract class MixinSwingAnimationRenderer {

    @Shadow
    public abstract void renderItem(net.minecraft.world.entity.LivingEntity entity, ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight);

    @Inject(
        method = "renderArmWithItem",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"),
        cancellable = true
    )
    private void onRenderArmWithItem(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swingProgress, ItemStack item, float equippedProgress, PoseStack poseStack, SubmitNodeCollector nodeCollector, int packedLight, CallbackInfo ci) {
        if (ModuleAnimations.INSTANCE.getEnabled() && ModuleAnimations.INSTANCE.getSwingAnimation().getEnabled() && hand == InteractionHand.MAIN_HAND) {
            if (ModuleSwordBlock.INSTANCE.getEnabled() && !ModuleSwordBlock.shouldAnimateSwordBlock(player)) {

                ModuleAnimations.INSTANCE.getSwingAnimation().onRenderItem(player, hand, swingProgress, equippedProgress, poseStack);

                boolean isMainHand = hand == InteractionHand.MAIN_HAND;
                HumanoidArm arm = isMainHand ? player.getMainArm() : player.getMainArm().getOpposite();
                ItemDisplayContext context = (arm == HumanoidArm.RIGHT) ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;

                this.renderItem(player, item, context, poseStack, nodeCollector, packedLight);

                poseStack.popPose();
                ci.cancel();

            }
        }
    }
}
