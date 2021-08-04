/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

import net.ccbluex.liquidbounce.common.RenderingFlags;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleESP;
import net.ccbluex.liquidbounce.render.engine.Color4b;
import net.ccbluex.liquidbounce.render.shaders.OutlineShader;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Matrix4f;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {
    @Shadow
    @Nullable
    public Framebuffer entityOutlinesFramebuffer;

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Inject(method = "loadEntityOutlineShader", at = @At("RETURN"))
    private void onLoadEntityOutlineShader(CallbackInfo info) {
        try {
            OutlineShader.INSTANCE.load();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void injectOutlineESP(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        // Prevent stack overflow
        if (!ModuleESP.INSTANCE.getEnabled() || !ModuleESP.OutlineMode.INSTANCE.isActive() || RenderingFlags.isCurrentlyRenderingEntityOutline().get())
            return;

        if (CombatExtensionsKt.shouldBeShown(entity)) {
            Color4b color = ModuleESP.INSTANCE.getColor(entity);
            OutlineShader outlineShader = OutlineShader.INSTANCE;
            Framebuffer originalBuffer = this.entityOutlinesFramebuffer;

            this.entityOutlinesFramebuffer = outlineShader.getFramebuffer();

            outlineShader.begin(ModuleESP.OutlineMode.INSTANCE.getWidth(), color != null ? color : ModuleESP.INSTANCE.getBaseColor());
            outlineShader.setDirty();

            RenderingFlags.isCurrentlyRenderingEntityOutline().set(true);

            try {
                renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, outlineShader.getVertexConsumerProvider());
            } finally {
                RenderingFlags.isCurrentlyRenderingEntityOutline().set(false);
            }

            this.entityOutlinesFramebuffer = originalBuffer;
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onDrawOutlines(MatrixStack matrices, float tickDelta, long limitTime, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, LightmapTextureManager lightmapTextureManager, Matrix4f matrix4f, CallbackInfo info) {
        if (!ModuleESP.INSTANCE.getEnabled() || !ModuleESP.OutlineMode.INSTANCE.isActive())
            return;

        OutlineShader.INSTANCE.end(tickDelta);
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;draw(IIZ)V"))
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo info) {
        if (!ModuleESP.INSTANCE.getEnabled() || !ModuleESP.OutlineMode.INSTANCE.isActive())
            return;

        if (OutlineShader.INSTANCE.isDirty())
            OutlineShader.INSTANCE.drawFramebuffer();
    }

    @Inject(method = "onResized", at = @At("HEAD"))
    private void onResized(int w, int h, CallbackInfo info) {
        OutlineShader.INSTANCE.onResized(w, h);
    }
}
