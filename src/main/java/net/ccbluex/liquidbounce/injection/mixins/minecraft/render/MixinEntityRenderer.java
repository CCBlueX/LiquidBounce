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

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP;
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode;
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity, S extends EntityRenderState> {

    @Shadow
    @Final
    protected EntityRenderManager dispatcher;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleCombineMobs.INSTANCE.getRunning() && ModuleCombineMobs.INSTANCE.trackEntity(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderMobOwners(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraState, CallbackInfo ci) {
        var entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();
        var ownerName = ModuleMobOwners.INSTANCE.getOwnerInfoText(entity);

        if (ownerName != null) {
            renderLabel(entity, ownerName, matrices, queue, state.light);
        }
    }

    @SuppressWarnings("unused")
    @Unique
    private void renderLabel(
        Entity entity, OrderedText text,
        MatrixStack matrices, OrderedRenderCommandQueue queue, int light
    ) {
        var d = this.dispatcher.getSquaredDistanceToCamera(entity);

        if (d > 4096.0) {
            return;
        }

        var f = entity.getHeight() / 2.0F;

        matrices.push();
        matrices.translate(0.0D, f, 0.0D);
        matrices.multiply(this.dispatcher.camera.getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);

        var matrix4f = matrices.peek().getPositionMatrix();

        var g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
        var j = (int) (g * 255.0F) << 24;
        var textRenderer = this.getTextRenderer();
        var h = (float) (-textRenderer.getWidth(text) / 2);
        queue.submitText(
            matrices, h,
            0, text,
            true, TextRenderer.TextLayerType.NORMAL,
            light, -1,
            Color4b.BLACK.toARGB(), -1
        );
        matrices.pop();
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void disableDuplicateNametagsAndInjectMobOwners(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        // Don't render nametags
        var entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();
        if (ModuleNametags.INSTANCE.getRunning() && CombatExtensionsKt.shouldBeShown(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "updateRenderState", at = @At("HEAD"))
    private void hookInjectEntityIntoState(T entity, S state, float tickDelta, CallbackInfo ci) {
        ((EntityRenderStateAddition) state).liquid_bounce$setEntity(entity);
    }

    @WrapOperation(method = "updateRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;hasOutline(Lnet/minecraft/entity/Entity;)Z"))
    private boolean modifyShouldRenderOutline(MinecraftClient instance, Entity entity, Operation<Boolean> operation) {
        return operation.call(instance, entity) || liquid_bounce$shouldRenderOutline(entity);
    }

    @Unique
    private static boolean liquid_bounce$shouldRenderOutline(Entity entity) {
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
    @WrapOperation(method = "updateRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;getTeamColorValue()I"))
    private int injectTeamColor(Entity entity, Operation<Integer> operation) {
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

        return operation.call(entity);
    }

}
