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
import net.ccbluex.liquidbounce.common.StorageEspOutlineContext;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleChams;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.feature.ItemFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SubmitNodeCollection.class)
public abstract class MixinSubmitNodeCollection {

    @ModifyVariable(method = "submitModel", at = @At("HEAD"), argsOnly = true, name = "outlineColor")
    private int injectStorageEspGlowOutlineColor(int outlineColor) {
        int storageEspOutlineColor = StorageEspOutlineContext.getOutlineColor();
        return outlineColor == 0 && storageEspOutlineColor != 0 ? storageEspOutlineColor : outlineColor;
    }

    @ModifyVariable(
        method = "submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V",
        at = @At("HEAD"),
        argsOnly = true,
        name = "renderType"
    )
    private RenderType remapHeldItemModelRenderType(RenderType renderType) {
        return ModuleChams.INSTANCE.remapCurrentHeldItemRenderTypeIfNeeded(renderType);
    }

    @Inject(
        method = "submitItem",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/ItemFeatureRenderer$Submit;hasTranslucency()Z"
        )
    )
    private void markHeldItemSubmit(
        CallbackInfo callbackInfo,
        @Local(name = "submit") ItemFeatureRenderer.Submit submit
    ) {
        ModuleChams.INSTANCE.markHeldItemSubmitIfActive(submit);
    }

}
