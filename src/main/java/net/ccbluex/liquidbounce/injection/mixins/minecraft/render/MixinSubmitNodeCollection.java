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
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.common.StorageEspOutlineContext;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleChams;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollection;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.UvMapping;
import net.minecraft.client.resources.model.geometry.ItemQuads;
import net.minecraft.world.item.ItemDisplayContext;
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

    /**
     * Captures the entity submission into the chams storage, so the entity only appears on the chams
     * render target. Its outline submission stays in this storage to keep glow working.
     */
    @Inject(method = "submitModel", at = @At("HEAD"))
    private <S> void captureChamsModel(
        Model<? super S> model,
        S state,
        PoseStack poseStack,
        RenderType renderType,
        int lightCoords,
        int overlayCoords,
        int tintedColor,
        UvMapping uvMapping,
        int outlineColor,
        CallbackInfo ci,
        @Share("chamsCaptured") LocalBooleanRef chamsCaptured
    ) {
        chamsCaptured.set(ModuleChams.INSTANCE.captureModel(
            model, state, poseStack, renderType, lightCoords, overlayCoords, tintedColor, uvMapping
        ));
    }

    /**
     * Captured models count as outline render types, for which vanilla only submits the outline.
     */
    @ModifyExpressionValue(method = "submitModel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/rendertype/RenderType;isOutline()Z"))
    private boolean skipChamsModel(boolean isOutline, @Share("chamsCaptured") LocalBooleanRef chamsCaptured) {
        return isOutline || chamsCaptured.get();
    }

    /**
     * Captures the item submission into the chams storage, so the item only appears on the chams
     * render target. Its outline submission stays in this storage to keep glow working.
     */
    @Inject(method = "submitItem", at = @At("HEAD"))
    private void captureChamsItem(
        PoseStack poseStack,
        ItemDisplayContext displayContext,
        int lightCoords,
        int overlayCoords,
        int outlineColor,
        int[] tintLayers,
        ItemQuads quads,
        ItemStackRenderState.FoilType foilType,
        CallbackInfo ci,
        @Share("chamsCaptured") LocalBooleanRef chamsCaptured
    ) {
        chamsCaptured.set(ModuleChams.INSTANCE.captureItem(
            poseStack, displayContext, lightCoords, overlayCoords, tintLayers, quads, foilType
        ));
    }

    /**
     * Captured items count as having no quads, so vanilla only submits their outline.
     */
    @ModifyExpressionValue(method = "submitItem", at = @At(value = "INVOKE", target = "Ljava/util/List;isEmpty()Z"))
    private boolean skipChamsItem(boolean isEmpty, @Share("chamsCaptured") LocalBooleanRef chamsCaptured) {
        return isEmpty || chamsCaptured.get();
    }

}
