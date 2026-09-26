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
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.ShieldSpecialRenderer;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShieldSpecialRenderer.class)
public abstract class MixinShieldSpecialRenderer {

    @WrapOperation(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"))
    private <S> void hookFirstPersonShieldBaseTint(
        OrderedSubmitNodeCollector instance, Model<? super S> model, S state, PoseStack poseStack, RenderType renderType, int lightCoords, int overlayCoords,
        int tintedColor, @Nullable UvMapping uvMapping, int outlineColor,
        Operation<Void> original
    ) {
        if (!FirstPersonShieldTint.isRendering()) {
            original.call(
                instance, model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, uvMapping, outlineColor
            );
            return;
        }

        int shieldTint = ModuleItemChams.Shield.INSTANCE.applyTint(tintedColor);
        if (ModuleItemChams.Shield.INSTANCE.usesTranslucentTint(tintedColor)) {
            instance.submitModel(
                model, state, poseStack, RenderTypes.entityTranslucentCull(RenderTypeSpriteLocation),
                lightCoords, overlayCoords, shieldTint, uvMapping, outlineColor
            );
            return;
        }

        original.call(
            instance, model, state, poseStack, renderType, lightCoords, overlayCoords, shieldTint, uvMapping, outlineColor
        );
    }

    @Unique
    private static final Identifier RenderTypeSpriteLocation = Identifier.withDefaultNamespace("textures/entity/shield_base.png");

    @ModifyArg(method = "submit(Lnet/minecraft/core/component/DataComponentMap;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IIZI)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/OrderedSubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/UvMapping;I)V"), index = 6)
    private int hookFirstPersonShieldGlintTint(int tintedColor) {
        return FirstPersonShieldTint.isRendering()
            ? ModuleItemChams.Shield.INSTANCE.applyTint(tintedColor)
            : tintedColor;
    }

}
