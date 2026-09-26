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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.entity.feature;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleLogoffSpot;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(RenderLayer.class)
public abstract class MixinRenderLayer {

    @Unique
    private static final int ESP_TRUE_SIGHT_REQUIREMENT_COLOR = new Color4b(255, 255, 255, 120).argb();

    @WrapOperation(method = "renderColoredCutoutModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"))
    private static <S> void injectTrueSight(
        OrderedSubmitNodeCollector instance,
        Model<? super S> model,
        S state, PoseStack poseStack, RenderType renderType,
        int light, int overlay, int tintedColor, @Nullable UvMapping uvMapping,
        int outlineColor, Operation<Void> original
    ) {
        if (state instanceof LivingEntityRenderState rs) {
            var trueSightModule = ModuleTrueSight.INSTANCE;
            var trueSight = trueSightModule.getRunning() && trueSightModule.getEntities();
            if (ModuleTrueSight.canRenderEntities(rs)) {
                tintedColor = trueSight ? trueSightModule.getEntityFeatureLayerColor().argb() : ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
            }
            if (ModuleLogoffSpot.INSTANCE.isLogoffEntity(rs)) {
                tintedColor = ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
            }
        }
        original.call(
            instance, model,
            state, poseStack,
            renderType, light,
            overlay, tintedColor,
            uvMapping, outlineColor
        );
    }

    @WrapOperation(method = "renderColoredCutoutModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderTypes;entityCutout(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;"))
    private static RenderType injectTrueSight(
        Identifier texture, Operation<RenderType> original, @Local(argsOnly = true, name = "state") LivingEntityRenderState state) {
        if (ModuleTrueSight.canRenderEntities(state) || ModuleLogoffSpot.INSTANCE.isLogoffEntity(state)) {
            return RenderTypes.entityTranslucentCull(texture);
        }
        return original.call(texture);
    }

}
