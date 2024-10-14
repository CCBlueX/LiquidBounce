package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock;
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features.AutoBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.item.ShieldItem;
import net.minecraft.item.SwordItem;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntityRenderer.class)
public class MixinPlayerEntityRenderer  {
    @Inject(method = "getArmPose", at = @At("HEAD"), cancellable = true)
    private static void injectArmPose(AbstractClientPlayerEntity player, Hand hand, CallbackInfoReturnable<BipedEntityModel.ArmPose> cir) {
        if (hand == Hand.OFF_HAND && player == MinecraftClient.getInstance().player) {
            if (ModuleSwordBlock.INSTANCE.handleEvents() || AutoBlock.INSTANCE.getBlockVisual()) {
                if (player.getOffHandStack().getItem() instanceof ShieldItem
                        && (player.getMainHandStack().getItem() instanceof SwordItem
                        || ModuleSwordBlock.INSTANCE.handleEvents()
                        && ModuleSwordBlock.INSTANCE.getAlwaysHideShield())) {
                    cir.setReturnValue(BipedEntityModel.ArmPose.EMPTY);
                }
            }
        }
    }
}
