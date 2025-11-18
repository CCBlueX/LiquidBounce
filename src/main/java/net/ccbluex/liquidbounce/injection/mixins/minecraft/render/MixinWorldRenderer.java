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
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.common.OutlineFlag;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.DrawOutlinesEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP;
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode;
import net.ccbluex.liquidbounce.render.engine.OutlineFramebufferHolder;
import net.ccbluex.liquidbounce.utils.collection.Pools;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.Handle;
import net.minecraft.client.util.ObjectAllocator;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.util.profiler.Profiler;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static org.lwjgl.opengl.GL11.*;

@Mixin(WorldRenderer.class)
public abstract class MixinWorldRenderer {

    @Shadow
    protected abstract boolean canDrawEntityOutlines();

    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract @Nullable Framebuffer getEntityOutlinesFramebuffer();

    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        var matrixStack = Pools.MatStack.borrow();
        // Apply camera transformation to fix outline positioning
        matrixStack.peek().getPositionMatrix().mul(positionMatrix);

        var event = new DrawOutlinesEvent(OutlineFramebufferHolder.prepare(), matrixStack, camera, tickCounter.getTickProgress(false), DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE);
        EventManager.INSTANCE.callEvent(event);

        if (event.getDirtyFlag()) {
            OutlineFramebufferHolder.setDirty(true);
        }
        Pools.MatStack.recycle(matrixStack);
    }

    @ModifyExpressionValue(method = "method_62218", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/ColorHelper;fromFloats(FFFF)I"))
    private int customFogClearColor(int original) {
        return ModuleCustomAmbience.FogConfigurable.INSTANCE.modifyClearColor(original);
    }

    // this method is a lambda
    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onDrawOutlines(Fog fog, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Matrix4f matrix4f2, Handle handle, Handle handle2, boolean bl, Frustum frustum, Handle handle3, Handle handle4, CallbackInfo ci) {
        OutlineFramebufferHolder.drawIfDirty(this.client.getFramebuffer());
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;drawBlit(Lcom/mojang/blaze3d/textures/GpuTexture;)V"))
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo info) {
    }

    @Unique
    private boolean isRenderingChams = false;

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void injectChamsForEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (ModuleChams.INSTANCE.getRunning() && CombatExtensionsKt.shouldBeAttacked(entity)) {
            glEnable(GL_POLYGON_OFFSET_FILL);
            glPolygonOffset(1f, -1000000F);

            this.isRenderingChams = true;
        }
    }

    @Inject(method = "renderEntity", at = @At("RETURN"))
    private void injectChamsForEntityPost(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo ci) {
        if (ModuleChams.INSTANCE.getRunning() && CombatExtensionsKt.shouldBeAttacked(entity) && this.isRenderingChams) {
            glPolygonOffset(1f, 1000000F);
            glDisable(GL_POLYGON_OFFSET_FILL);

            this.isRenderingChams = false;
        }
    }

    @Redirect(method = "getEntitiesToRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isSleeping()Z"))
    private boolean hookFreeCamRenderPlayerFromAllPerspectives(LivingEntity instance) {
        return ModuleFreeCam.INSTANCE.renderPlayerFromAllPerspectives(instance);
    }

    @ModifyExpressionValue(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    private boolean injectHasOutline(boolean original, @Local Entity entity) {
        return original || shouldRenderOutline(entity);
    }

    @ModifyExpressionValue(method = "getEntitiesToRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    private boolean injectHasOutline2(boolean original, @Local Entity entity) {
        return original || shouldRenderOutline(entity);
    }

    @Unique
    private boolean shouldRenderOutline(Entity entity) {
        if (ModuleItemESP.GlowMode.INSTANCE.getRunning() && ModuleItemESP.INSTANCE.shouldRender(entity)) {
            return true;
        } else if (EspGlowMode.INSTANCE.getRunning() && CombatExtensionsKt.shouldBeShown(entity) && EspGlowMode.INSTANCE.shouldRender(entity)) {
            return true;
        } else if (ModuleTNTTimer.INSTANCE.getRunning() && ModuleTNTTimer.INSTANCE.getEsp() && entity instanceof TntEntity) {
            return true;
        } else if (ModuleStorageESP.Glow.INSTANCE.getRunning()) {
            var category = ModuleStorageESP.categorize(entity);
            return category != null && category.shouldRender(entity);
        } else {
            return false;
        }
    }

    /**
     * Inject ESP color as glow color
     *
     * @author 1zuna
     */
    @ModifyExpressionValue(method = "renderEntities", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    private int injectTeamColor(int original, @Local Entity entity) {
        if (entity instanceof LivingEntity livingEntity && EspGlowMode.INSTANCE.getRunning() && EspGlowMode.INSTANCE.shouldRender(livingEntity)) {
            return ModuleESP.INSTANCE.getColor(livingEntity).toARGB();
        } else if (ModuleItemESP.GlowMode.INSTANCE.getRunning() && ModuleItemESP.INSTANCE.shouldRender(entity)) {
            return ModuleItemESP.INSTANCE.getColor().toARGB();
        } else if (entity instanceof TntEntity tntEntity && ModuleTNTTimer.INSTANCE.getRunning() && ModuleTNTTimer.INSTANCE.getEsp()) {
            return ModuleTNTTimer.INSTANCE.getTntColor(tntEntity.getFuse()).toARGB();
        } else if (ModuleStorageESP.Glow.INSTANCE.getRunning()) {
            var category = ModuleStorageESP.categorize(entity);
            if (category != null && category.shouldRender(entity)) {
                return category.getColor().toARGB();
            }
        }

        return original;
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V", shift = At.Shift.BEFORE))
    private void onRenderGlow(Fog fog, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Matrix4f matrix4f2, Handle handle, Handle handle2, boolean bl, Frustum frustum, Handle handle3, Handle handle4, CallbackInfo ci) {
        var entityOutlineFb = getEntityOutlinesFramebuffer();
        if (!this.canDrawEntityOutlines() || entityOutlineFb == null) {
            return;
        }

        var matrixStack = Pools.MatStack.borrow();
        entityOutlineFb.blitToScreen();
        var event = new DrawOutlinesEvent(entityOutlineFb, matrixStack, camera, renderTickCounter.getTickProgress(false), DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW);
        EventManager.INSTANCE.callEvent(event);
        OutlineFlag.drawOutline |= event.getDirtyFlag();
        Pools.MatStack.recycle(matrixStack);
    }

    @ModifyVariable(method = "render", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/gl/ShaderLoader;loadPostEffect(Lnet/minecraft/util/Identifier;Ljava/util/Set;)Lnet/minecraft/client/gl/PostEffectProcessor;"
    ), name = "bl3", ordinal = 3)
    private boolean modifyDrawOutline(boolean original) {
        var flag = OutlineFlag.drawOutline;
        if (flag) {
            OutlineFlag.drawOutline = false;
        }
        return original || flag;
    }

    @ModifyArg(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;setupTerrain(Lnet/minecraft/client/render/Camera;Lnet/minecraft/client/render/Frustum;ZZ)V"), index = 3)
    private boolean renderSetupTerrainModifyArg(boolean spectator) {
        return ModuleFreeCam.INSTANCE.getRunning() || spectator;
    }

    @Inject(method = "renderTargetBlockOutline", at = @At("HEAD"), cancellable = true)
    private void cancelBlockOutline(Camera camera, VertexConsumerProvider.Immediate vertexConsumers, MatrixStack matrices, boolean translucent, CallbackInfo ci) {
        if (ModuleBlockOutline.INSTANCE.getRunning()) {
            ci.cancel();
        }
    }

}
