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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render.special;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleItemChams;
import net.ccbluex.liquidbounce.utils.render.FirstPersonShieldTint;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ShieldSpecialRenderer.class)
public abstract class MixinShieldSpecialRenderer {

    @WrapOperation(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;IIILnet/minecraft/client/resources/model/sprite/SpriteId;Lnet/minecraft/client/resources/model/sprite/SpriteGetter;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private <S> void hookFirstPersonShieldBaseTint(
        SubmitNodeCollector instance, Model<S> model, S state, PoseStack poseStack, int lightCoords, int overlayCoords,
        int tintedColor, SpriteId sprite, SpriteGetter sprites, int outlineColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlay, Operation<Void> original
    ) {
        if (!FirstPersonShieldTint.isRendering()) {
            original.call(
                instance, model, state, poseStack, lightCoords, overlayCoords, tintedColor, sprite, sprites, outlineColor,
                crumblingOverlay
            );
            return;
        }

        int shieldTint = ModuleItemChams.Shield.INSTANCE.applyTint(tintedColor);
        if (ModuleItemChams.Shield.INSTANCE.usesTranslucentTint(tintedColor)) {
            instance.submitModel(
                model, state, poseStack, RenderTypes.entityTranslucentCullItemTarget(sprite.atlasLocation()),
                lightCoords, overlayCoords, shieldTint,
                sprites.get(sprite), outlineColor, crumblingOverlay
            );
            return;
        }

        original.call(
            instance, model, state, poseStack, lightCoords, overlayCoords, shieldTint, sprite, sprites, outlineColor,
            crumblingOverlay
        );
    }

    @WrapOperation(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private <S> void hookFirstPersonShieldGlintTint(
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
