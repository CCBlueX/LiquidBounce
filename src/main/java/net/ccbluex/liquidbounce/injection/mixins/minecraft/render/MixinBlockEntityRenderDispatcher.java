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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.common.OutlineFlag;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleStorageESP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(BlockEntityRenderDispatcher.class)
public abstract class MixinBlockEntityRenderDispatcher {

    /**
     * Inject StorageESP glow effect
     *
     * @author 1zuna
     */
    @ModifyArg(
            method = "submit",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/blockentity/BlockEntityRenderer;submit(Lnet/minecraft/client/renderer/blockentity/state/BlockEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V")
    )
    private static <S extends BlockEntityRenderState> S render(
        S state, @Local(argsOnly = true) S blockEntityRenderState
    ) {
        var client = Minecraft.getInstance();
        if (ModuleStorageESP.GlowMode.INSTANCE.getRunning() && client.level != null) {
            var type = ModuleStorageESP.categorize(client.level.getBlockEntity(blockEntityRenderState.blockPos));

            if (type != null && type.shouldRender(blockEntityRenderState.blockPos)) {
                var color = type.getColor();

                if (!color.isTransparent()) {
                    var outlineVertexConsumerProvider = client.renderBuffers()
                        .outlineBufferSource();
                    outlineVertexConsumerProvider.setColor(color.argb());
                    OutlineFlag.drawOutline = true;
                    return state;
                }
            }
        }

        return state;
    }

}
