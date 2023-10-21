/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

import com.mojang.datafixers.util.Pair;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.GameRenderEvent;
import net.ccbluex.liquidbounce.event.ScreenRenderEvent;
import net.ccbluex.liquidbounce.event.WorldRenderEvent;
import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleDankBobbing;
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleReach;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoBob;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoHurtCam;
import net.ccbluex.liquidbounce.interfaces.IMixinGameRenderer;
import net.ccbluex.liquidbounce.web.GameWebView;
import net.fabricmc.fabric.impl.client.rendering.FabricShaderProgram;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.screen.Overlay;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.resource.ResourceFactory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IMixinGameRenderer {

    @Shadow
    @Final
    private Camera camera;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private int ticks;

    @Shadow
    protected abstract void bobView(MatrixStack matrixStack, float f);

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(double d);

    @Shadow
    protected abstract double getFov(Camera camera, float tickDelta, boolean changingFov);

    @Shadow
    protected abstract void tiltViewWhenHurt(MatrixStack matrices, float tickDelta);

    @Inject(method = "render", at = @At("HEAD"))
    private void hookRenderHead(float tickDelta, long startTime, boolean tick, CallbackInfo callbackInfo) {
        GameWebView.INSTANCE.renderTick();
    }

    /**
     * Hook game render event
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;F)V"))
    public void hookInGameRender(InGameHud instance, DrawContext context, float tickDelta) {
        GameWebView.INSTANCE.renderInGame(context);
        EventManager.INSTANCE.callEvent(new GameRenderEvent());
        instance.render(context, tickDelta);
    }

    /**
     * Hook overlay render event
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Overlay;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    public void hookOverlayRender(Overlay instance, DrawContext context, int mouseX, int mouseY, float delta) {
        instance.render(context, mouseX, mouseY, delta);
        GameWebView.INSTANCE.renderOverlay(context);
    }

    /**
     * Hook world render event
     */
    @Inject(method = "renderWorld", at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/render/GameRenderer;renderHand:Z",
            opcode = Opcodes.GETFIELD,
            ordinal = 0))
    public void hookWorldRender(float partialTicks, long finishTimeNano, MatrixStack matrixStack,
                                final CallbackInfo callbackInfo) {
        EventManager.INSTANCE.callEvent(new WorldRenderEvent(matrixStack, partialTicks));
    }

    /**
     * Hook screen render event
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/Screen;renderWithTooltip(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    public void hookScreenRender(Screen screen, DrawContext context, int mouseX, int mouseY, float delta) {
        screen.render(context, mouseX, mouseY, delta);
        EventManager.INSTANCE.callEvent(new ScreenRenderEvent(screen, context, mouseX, mouseY, delta));
    }

    @Override
    public Matrix4f getCameraMVPMatrix(float tickDelta, boolean bobbing) {
        final MatrixStack matrices = new MatrixStack();

        final double fov = this.getFov(camera, tickDelta, true);
        matrices.multiplyPositionMatrix(this.getBasicProjectionMatrix(fov));

        if (bobbing) {
            this.tiltViewWhenHurt(matrices, tickDelta);

            if (this.client.options.getBobView().getValue()) {
                this.bobView(matrices, tickDelta);
            }

            float f = MathHelper.lerp(tickDelta, this.client.player.prevNauseaIntensity, this.client.player.nauseaIntensity) * this.client.options.getDistortionEffectScale().getValue().floatValue() * this.client.options.getDistortionEffectScale().getValue().floatValue();
            if (f > 0.0F) {
                int i = this.client.player.hasStatusEffect(StatusEffects.NAUSEA) ? 7 : 20;
                float g = 5.0F / (f * f + 5.0F) - f * 0.04F;
                g *= g;

                RotationAxis vec3f = RotationAxis.of(new Vector3f(0.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F, MathHelper.SQUARE_ROOT_OF_TWO / 2.0F));
                matrices.multiply(vec3f.rotationDegrees(((float) this.ticks + tickDelta) * (float) i));
                matrices.scale(1.0F / g, 1.0F, 1.0F);
                float h = -((float) this.ticks + tickDelta) * (float) i;
                matrices.multiply(vec3f.rotationDegrees(h));
            }
        }

        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0f));

        final Vec3d cameraPosition = this.camera.getPos();

        final Matrix4f matrix4f = matrices.peek().getPositionMatrix();
        matrix4f.mul(new Matrix4f().translate((float) -cameraPosition.x, (float) -cameraPosition.y, (float) -cameraPosition.z));
        return matrix4f;
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void injectHurtCam(MatrixStack matrixStack, float f, CallbackInfo callbackInfo) {
        if (ModuleNoHurtCam.INSTANCE.getEnabled()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void injectBobView(MatrixStack matrixStack, float f, CallbackInfo callbackInfo) {
        if (ModuleNoBob.INSTANCE.getEnabled()) {
            callbackInfo.cancel();
            return;
        }

        if (!ModuleDankBobbing.INSTANCE.getEnabled()) {
            return;
        }

        if (!(this.client.getCameraEntity() instanceof PlayerEntity playerEntity)) {
            return;
        }

        float additionalBobbing = ModuleDankBobbing.INSTANCE.getMotion();

        float g = playerEntity.horizontalSpeed - playerEntity.prevHorizontalSpeed;
        float h = -(playerEntity.horizontalSpeed + g * f);
        float i = MathHelper.lerp(f, playerEntity.prevStrideDistance, playerEntity.strideDistance);
        matrixStack.translate((MathHelper.sin(h * MathHelper.PI) * i * 0.5F), -Math.abs(MathHelper.cos(h * MathHelper.PI) * i), 0.0D);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(h * MathHelper.PI) * i * (3.0F + additionalBobbing)));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(Math.abs(MathHelper.cos(h * MathHelper.PI - (0.2F + additionalBobbing)) * i) * 5.0F));

        callbackInfo.cancel();
    }

    @Inject(method = "renderHand", at = @At("HEAD"), cancellable = true)
    private void hookFreeCamDisableHandRender(MatrixStack matrices, Camera camera, float tickDelta, CallbackInfo ci) {
        if (ModuleFreeCam.INSTANCE.shouldDisableHandRender()) {
            ci.cancel();
        }
    }

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 9.0))
    private double hookReachModifyCombatReach(double constant) {
        return ModuleReach.INSTANCE.getEnabled() ? (double) (ModuleReach.INSTANCE.getCombatReach() * ModuleReach.INSTANCE.getCombatReach()) : constant;
    }

    @Inject(method = "updateTargetedEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getRotationVec(F)Lnet/minecraft/util/math/Vec3d;"))
    private void hookReachModifyBlockReach(float tickDelta, CallbackInfo ci) {
        if (ModuleReach.INSTANCE.getEnabled()) {
            this.client.crosshairTarget = this.client.player.raycast(ModuleReach.INSTANCE.getBlockReach(), tickDelta, false);
        }
    }

    public ShaderProgram bgraPositionTextureShader;

    /**
     * Register BRGA shader
     * Code taken from FabricMC fabric-rendering-v1 for MC 1.20.1
     */
    @Inject(
            method = "loadPrograms",
            at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false, shift = At.Shift.AFTER),
            slice = @Slice(from = @At(value = "NEW", target = "net/minecraft/client/gl/ShaderProgram", ordinal = 0)),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void registerBgraShader(ResourceFactory factory, CallbackInfo info, List<?> shaderStages, List<Pair<ShaderProgram, Consumer<ShaderProgram>>> programs) throws IOException {
        programs.add(new Pair<>(new FabricShaderProgram(factory, new Identifier("liquidbounce", "bgra_position_tex"), VertexFormats.POSITION_TEXTURE), program -> {
            bgraPositionTextureShader = program;
        }));
    }

    @Override
    public ShaderProgram getBgraPositionTextureShader() {
        return bgraPositionTextureShader;
    }
}
