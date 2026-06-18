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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.client.renderer.ScreenEffectRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ScreenEffectRenderer.class)
public abstract class MixinScreenEffectRenderer {

    @Unique
    private static final ScopedValue<Float> FIRE_ALPHA = ScopedValue.newInstance();

    @WrapMethod(method = "lambda$submitFire$0")
    private static void wrapFireRenderer(TextureAtlasSprite sprite, PoseStack.Pose basePose, VertexConsumer builder, Operation<Void> original) {
        ScopedValue.where(FIRE_ALPHA, ModuleAntiBlind.INSTANCE.getFireOpacityPercentage())
            .run(() -> original.call(sprite, basePose, builder));
    }

    @ModifyArg(
        method = "buildQuad",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/VertexConsumer;setColor(I)Lcom/mojang/blaze3d/vertex/VertexConsumer;")
    )
    private static int injectFireOpacity(int color) {
        return FIRE_ALPHA.isBound()
            ? ARGB.multiplyAlpha(color, FIRE_ALPHA.get())
            : color;
    }

    @Inject(method = "submitBlockSprite", at = @At("HEAD"), cancellable = true)
    private static void hookWallOverlay(TextureAtlasSprite sprite, PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector, int color, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.WALL_OVERLAY)) {
            ci.cancel();
        }
    }

}
