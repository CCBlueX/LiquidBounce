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

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleChams;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.OutputTarget;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemFeatureRenderer.class)
public abstract class MixinItemFeatureRenderer extends MixinRenderTypeFeatureRenderer {

    @ModifyExpressionValue(
        method = "prepareMainSubmit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/resources/model/geometry/BakedQuad$MaterialInfo;itemRenderType()Lnet/minecraft/client/renderer/rendertype/RenderType;"
        )
    )
    private RenderType remapHeldItemMainRenderType(
        RenderType renderType,
        @Local(argsOnly = true, name = "submit") ItemFeatureRenderer.Submit submit
    ) {
        return ModuleChams.INSTANCE.remapHeldItemRenderTypeIfNeeded(submit, renderType);
    }

    @WrapOperation(
        method = "prepareFoilSubmit",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/rendertype/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer remapHeldItemFoilRenderType(
        ItemFeatureRenderer instance,
        RenderType renderType,
        @Nullable PoseStack.Pose foilDecalPose,
        Operation<VertexConsumer> original,
        @Local(argsOnly = true, name = "submit") ItemFeatureRenderer.Submit submit
    ) {
        if (!ModuleChams.INSTANCE.isHeldItemSubmit(submit)) {
            return original.call(instance, renderType, foilDecalPose);
        }

        RenderType foilRenderType = useTransparentGlint(renderType)
            ? RenderTypes.glintTranslucent()
            : RenderTypes.glint();

        VertexConsumer vertexConsumer = getVertexBuilder(
            ModuleChams.INSTANCE.remapHeldItemRenderTypeIfNeeded(submit, foilRenderType)
        );

        if (foilDecalPose != null) {
            vertexConsumer = new SheetedDecalTextureGenerator(vertexConsumer, foilDecalPose, 0.0078125F);
        }

        return vertexConsumer;
    }

    @Unique
    private static boolean useTransparentGlint(RenderType renderType) {
        return Minecraft.getInstance().gameRenderer.gameRenderState().useShaderTransparency()
            && renderType.outputTarget() == OutputTarget.ITEM_ENTITY_TARGET;
    }

}
