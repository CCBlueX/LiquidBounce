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

            if (type != null && type.getShouldRender().invoke(blockEntity.getPos())) {
                var color = type.getColor().invoke();

                if (color.getA() > 0) {
                    outlineVertexConsumerProvider.setColor(color.getR(), color.getG(), color.getB(), color.getA());

                    blockEntityRenderer.render(blockEntity, tickDelta, matrices, outlineVertexConsumerProvider, light, overlay);
                    OutlineFlag.drawOutline = true;
                    return;
                }
            }
        }

        blockEntityRenderer.render(blockEntity, tickDelta, matrices, vertexConsumers, light, overlay);
    }

}
