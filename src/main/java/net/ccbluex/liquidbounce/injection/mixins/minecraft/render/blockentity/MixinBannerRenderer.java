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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.blockentity;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams;
import net.ccbluex.liquidbounce.utils.render.FirstPersonShieldTint;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BannerRenderer.class)
public abstract class MixinBannerRenderer {

    @WrapOperation(method = "submitPatternLayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private static <S> void hookFirstPersonShieldPatternTint(
        SubmitNodeCollector instance, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType,
        int lightCoords, int overlayCoords, int tintedColor, TextureAtlasSprite sprite, int outlineColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original
    ) {
        original.call(
            instance, model, state, poseStack, renderType, lightCoords, overlayCoords,
            FirstPersonShieldTint.isRendering()
                ? ModuleItemChams.Shield.INSTANCE.applyTint(tintedColor)
                : tintedColor,
            sprite, outlineColor, crumblingOverlay
        );
    }

}
