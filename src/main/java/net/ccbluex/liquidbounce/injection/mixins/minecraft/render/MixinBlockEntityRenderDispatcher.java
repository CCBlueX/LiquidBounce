package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.common.OutlineFlag;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderDispatcher;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockEntityRenderDispatcher.class)
public class MixinBlockEntityRenderDispatcher {

    /**
     * Inject StorageESP glow effect
     *
     * @author 1zuna
     */
    @Redirect(
            method = "render(Lnet/minecraft/client/render/block/entity/BlockEntityRenderer;Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/block/entity/BlockEntityRenderer;render(Lnet/minecraft/block/entity/BlockEntity;FLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;II)V")
    )
    private static void render(BlockEntityRenderer blockEntityRenderer, BlockEntity blockEntity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (ModuleStorageESP.INSTANCE.getEnabled() && ModuleStorageESP.Glow.INSTANCE.isActive()) {
            var outlineVertexConsumerProvider = MinecraftClient.getInstance().getBufferBuilders()
                    .getOutlineVertexConsumers();
            var type = ModuleStorageESP.INSTANCE.categorizeBlockEntity(blockEntity);

            if (type != null) {
                var color = type.getColor();
                outlineVertexConsumerProvider.setColor(color.getR(), color.getG(), color.getB(), 255);

                blockEntityRenderer.render(blockEntity, tickDelta, matrices, outlineVertexConsumerProvider, light, overlay);
                OutlineFlag.drawOutline = true;
            }
            return;
        }

        blockEntityRenderer.render(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay);
    }

}
