package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity.feature;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.render.entity.feature.Deadmau5FeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Deadmau5FeatureRenderer.class)
public class MixinDeadmau5FeatureRenderer {

    @ModifyExpressionValue(
            method = "render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/network/AbstractClientPlayerEntity;FFFFFF)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/String;equals(Ljava/lang/Object;)Z")
    )
    private boolean onRender(boolean original) {
        // TODO: Check if player has a deadmau5 ears cosmetic enabled via LiquidBounce's cosmetics system
        return true;
    }

}
