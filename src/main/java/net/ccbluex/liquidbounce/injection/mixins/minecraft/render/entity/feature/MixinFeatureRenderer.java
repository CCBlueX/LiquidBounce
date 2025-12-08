/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleLogoffSpot;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.minecraft.client.model.Model;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.command.ModelCommandRenderer;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FeatureRenderer.class)
public abstract class MixinFeatureRenderer {

    @Unique
    private static final int ESP_TRUE_SIGHT_REQUIREMENT_COLOR = new Color4b(255, 255, 255, 120).toARGB();

    @WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"))
    private static <S> void injectTrueSight(
        RenderCommandQueue instance,
        Model<? super S> model,
        S state,
        MatrixStack matrices,
        RenderLayer renderLayer,
        int light,
        int overlay,
        int tintedColor,
        Sprite sprite,
        int outlineColor,
        ModelCommandRenderer.CrumblingOverlayCommand crumblingOverlay,
        Operation<Void> original
    ) {
        if (state instanceof LivingEntityRenderState rs) {
            var trueSightModule = ModuleTrueSight.INSTANCE;
            var trueSight = trueSightModule.getRunning() && trueSightModule.getEntities();
            if (ModuleTrueSight.canRenderEntities(rs)) {
                tintedColor = trueSight ? trueSightModule.getEntityFeatureLayerColor().toARGB() : ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
            }
            if (ModuleLogoffSpot.INSTANCE.isLogoffEntity(rs)) {
                tintedColor = ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
            }
        }
        original.call(
            instance, model,
            state, matrices,
            renderLayer, light,
            overlay, tintedColor,
            sprite, outlineColor,
            crumblingOverlay
        );
    }

    @WrapOperation(method = "renderModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/RenderLayers;entityCutoutNoCull(Lnet/minecraft/util/Identifier;)Lnet/minecraft/client/render/RenderLayer;"))
    private static RenderLayer injectTrueSight(Identifier texture, Operation<RenderLayer> original, @Local(argsOnly = true) LivingEntityRenderState state) {
        if (ModuleTrueSight.canRenderEntities(state) || ModuleLogoffSpot.INSTANCE.isLogoffEntity(state)) {
            return RenderLayers.itemEntityTranslucentCull(texture);
        }
        return original.call(texture);
    }

}
