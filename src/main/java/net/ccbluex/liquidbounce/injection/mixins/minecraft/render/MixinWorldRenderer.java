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
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspOutlineMode;
import net.ccbluex.liquidbounce.render.engine.RenderingFlags;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.ccbluex.liquidbounce.render.shader.shaders.OutlineShader;
import net.ccbluex.liquidbounce.utils.client.ClientUtilsKt;
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler;
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
    @Final
    public DefaultFramebufferSet framebufferSet;

    @Shadow
    protected abstract void renderEntity(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers);

    @Shadow
    @Nullable
    public Framebuffer entityOutlineFramebuffer;

    @Shadow
    public abstract @Nullable Framebuffer getEntityOutlinesFramebuffer();

    @Inject(method = "loadEntityOutlinePostProcessor", at = @At("RETURN"))
    private void onLoadEntityOutlineShader(CallbackInfo info) {
        try {
            // load the shader class to compile the shaders
            //noinspection unused
            var instance = OutlineShader.INSTANCE;
        } catch (Exception e) {
            ErrorHandler.fatal(e, null, true, "Failed to load outline shader");

            // This will make Minecraft unable to continue loading
            throw e;
        }
    }

   @Inject(method = "render", at = @At("HEAD"))
    private void onRender(ObjectAllocator allocator, RenderTickCounter tickCounter, boolean renderBlockOutline, Camera camera, GameRenderer gameRenderer, Matrix4f positionMatrix, Matrix4f projectionMatrix, CallbackInfo ci) {
        try {
            OutlineShader outlineShader = OutlineShader.INSTANCE;
            outlineShader.update();
            outlineShader.getHandle().get().beginWrite(false);

            var event = new DrawOutlinesEvent(new MatrixStack(), camera, tickCounter.getTickDelta(false), DrawOutlinesEvent.OutlineType.INBUILT_OUTLINE);
            EventManager.INSTANCE.callEvent(event);

            if (event.getDirtyFlag()) {
                outlineShader.setDirty(true);
            }

            client.getFramebuffer().beginWrite(false);
        } catch (Throwable e) {
            ClientUtilsKt.getLogger().error("Failed to begin outline shader", e);
        }
    }

    @Inject(method = "renderEntity", at = @At("HEAD"))
    private void injectOutlineESP(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        // Prevent stack overflow
        if (RenderingFlags.isCurrentlyRenderingEntityOutline().get()) {
            return;
        }

        Color4b color;

        if (EspOutlineMode.INSTANCE.getRunning() && entity instanceof LivingEntity && CombatExtensionsKt.shouldBeShown(entity)) {
            color = ModuleESP.INSTANCE.getColor((LivingEntity) entity);
        } else if (ModuleItemESP.OutlineMode.INSTANCE.getRunning() && ModuleItemESP.INSTANCE.shouldRender(entity)) {
            color = ModuleItemESP.INSTANCE.getColor();
        } else {
            return;
        }

        var outlineShader = OutlineShader.INSTANCE;
        var originalBuffer = framebufferSet.entityOutlineFramebuffer;
        var originalBuffer2 = entityOutlineFramebuffer;

        framebufferSet.entityOutlineFramebuffer = outlineShader.getHandle();
        entityOutlineFramebuffer = outlineShader.getHandle().get();

        outlineShader.setColor(color);
        outlineShader.setDirty(true);

        RenderingFlags.isCurrentlyRenderingEntityOutline().set(true);

        try {
            renderEntity(entity, cameraX, cameraY, cameraZ, tickDelta, matrices, outlineShader.getVertexConsumerProvider());
        } finally {
            RenderingFlags.isCurrentlyRenderingEntityOutline().set(false);
        }

        entityOutlineFramebuffer = originalBuffer2;
        framebufferSet.entityOutlineFramebuffer = originalBuffer;
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V"))
    private void onDrawOutlines(Fog fog, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Matrix4f matrix4f2, Handle handle, Handle handle2, Handle handle3, Handle handle4, boolean bl, Frustum frustum, Handle handle5, CallbackInfo ci) {
        if (OutlineShader.INSTANCE.getDirty()) {
            OutlineShader.INSTANCE.draw();
        }
    }

    @Inject(method = "drawEntityOutlinesFramebuffer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;drawInternal(II)V"))
    private void onDrawEntityOutlinesFramebuffer(CallbackInfo info) {
        if (OutlineShader.INSTANCE.getDirty()) {
            OutlineShader.INSTANCE.apply(false);
        }
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
        } else if (EspGlowMode.INSTANCE.getRunning() && CombatExtensionsKt.shouldBeShown(entity)) {
            return true;
        } else if (ModuleTNTTimer.INSTANCE.getRunning() && ModuleTNTTimer.INSTANCE.getEsp() && entity instanceof TntEntity) {
            return true;
        } else if (ModuleStorageESP.Glow.INSTANCE.getRunning() && ModuleStorageESP.categorize(entity) != null) {
            return true;
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
        if (entity instanceof LivingEntity livingEntity && EspGlowMode.INSTANCE.getRunning()) {
            return ModuleESP.INSTANCE.getColor(livingEntity).toARGB();
        } else if (ModuleItemESP.GlowMode.INSTANCE.getRunning() && ModuleItemESP.INSTANCE.shouldRender(entity)) {
            return ModuleItemESP.INSTANCE.getColor().toARGB();
        } else if (entity instanceof TntEntity tntEntity && ModuleTNTTimer.INSTANCE.getRunning() && ModuleTNTTimer.INSTANCE.getEsp()) {
            return ModuleTNTTimer.INSTANCE.getTntColor(tntEntity.getFuse()).toARGB();
        } else if (ModuleStorageESP.Glow.INSTANCE.getRunning()) {
            var color = ModuleStorageESP.categorize(entity);
            if (color != null) {
                return color.getColor().toARGB();
            }
        }

        return original;
    }

    @Inject(method = "method_62214", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/OutlineVertexConsumerProvider;draw()V", shift = At.Shift.BEFORE))
    private void onRenderOutline(Fog fog, RenderTickCounter renderTickCounter, Camera camera, Profiler profiler, Matrix4f matrix4f, Matrix4f matrix4f2, Handle handle, Handle handle2, Handle handle3, Handle handle4, boolean bl, Frustum frustum, Handle handle5, CallbackInfo ci) {
        if (!this.canDrawEntityOutlines()) {
            return;
        }

        //noinspection DataFlowIssue
        this.getEntityOutlinesFramebuffer().beginWrite(false);
        var event = new DrawOutlinesEvent(new MatrixStack(), camera, renderTickCounter.getTickDelta(false), DrawOutlinesEvent.OutlineType.MINECRAFT_GLOW);
        EventManager.INSTANCE.callEvent(event);
        OutlineFlag.drawOutline |= event.getDirtyFlag();
        client.getFramebuffer().beginWrite(false);
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
