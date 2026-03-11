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

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP;
import net.ccbluex.liquidbounce.features.module.modules.render.esp.modes.EspGlowMode;
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.phys.Vec3;
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
    protected EntityRenderDispatcher entityRenderDispatcher;

    @Shadow
    public abstract Font getFont();

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleCombineMobs.INSTANCE.getRunning() && ModuleCombineMobs.INSTANCE.trackEntity(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "submit", at = @At("HEAD"))
    private void renderMobOwners(S state, PoseStack matrices, SubmitNodeCollector queue, CameraRenderState cameraState, CallbackInfo ci) {
        var entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();
        var ownerName = ModuleMobOwners.INSTANCE.getOwnerInfoText(entity);

        if (ownerName != null) {
            renderLabel(entity, ownerName, matrices, queue, state.lightCoords);
        }
    }

    @SuppressWarnings("unused")
    @Unique
    private void renderLabel(
        Entity entity, FormattedCharSequence text,
        PoseStack matrices, SubmitNodeCollector queue, int light
    ) {
        var d = this.entityRenderDispatcher.distanceToSqr(entity);

        if (d > 4096.0) {
            return;
        }

        var f = entity.getBbHeight() / 2.0F;

        matrices.pushPose();
        matrices.translate(0.0D, f, 0.0D);
        matrices.mulPose(this.entityRenderDispatcher.camera.rotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);

        var matrix4f = matrices.last().pose();

        var g = Minecraft.getInstance().options.getBackgroundOpacity(0.25F);
        var j = (int) (g * 255.0F) << 24;
        var textRenderer = this.getFont();
        var h = (float) (-textRenderer.width(text) / 2);
        queue.submitText(
            matrices, h,
            0, text,
            true, Font.DisplayMode.NORMAL,
            light, -1,
            Color4b.BLACK.argb(), -1
        );
        matrices.popPose();
    }

    @WrapWithCondition(method = "submitNameTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitNameTag(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;ILnet/minecraft/network/chat/Component;ZIDLnet/minecraft/client/renderer/state/CameraRenderState;)V"))
    private boolean disableVanillaNametag(SubmitNodeCollector instance, PoseStack poseStack, Vec3 vec3, int i, Component component, boolean b, int j, double v, CameraRenderState cameraRenderState, @Local(argsOnly = true) S state) {
        return ModuleNametags.INSTANCE.shouldRenderVanillaNametag(state);
    }

    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void hookInjectEntityIntoState(T entity, S state, float tickDelta, CallbackInfo ci) {
        ((EntityRenderStateAddition) state).liquid_bounce$setEntity(entity);
    }

    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;shouldEntityAppearGlowing(Lnet/minecraft/world/entity/Entity;)Z"))
    private boolean modifyShouldRenderOutline(Minecraft instance, Entity entity, Operation<Boolean> operation) {
        return operation.call(instance, entity) || liquid_bounce$shouldRenderOutline(entity);
    }

    @Unique
    private static boolean liquid_bounce$shouldRenderOutline(Entity entity) {
        if (ModuleItemESP.GlowMode.INSTANCE.getRunning() && ModuleItemESP.INSTANCE.shouldRender(entity)) {
            return true;
        } else if (EspGlowMode.INSTANCE.getRunning() && CombatExtensionsKt.shouldBeShown(entity) && EspGlowMode.INSTANCE.shouldRender(entity)) {
            return true;
        } else if (ModuleTNTTimer.INSTANCE.getRunning() && ModuleTNTTimer.INSTANCE.getEsp() && entity instanceof PrimedTnt) {
            return true;
        } else if (ModuleStorageESP.GlowMode.INSTANCE.getRunning()) {
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
    @WrapOperation(method = "extractRenderState", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;getTeamColor()I"))
    private int injectTeamColor(Entity entity, Operation<Integer> operation) {
        if (entity instanceof LivingEntity livingEntity && EspGlowMode.INSTANCE.getRunning() && EspGlowMode.INSTANCE.shouldRender(livingEntity)) {
            return ModuleESP.INSTANCE.getColor(livingEntity).argb();
        } else if (ModuleItemESP.GlowMode.INSTANCE.getRunning() && ModuleItemESP.INSTANCE.shouldRender(entity)) {
            return ModuleItemESP.INSTANCE.getColor().argb();
        } else if (entity instanceof PrimedTnt tntEntity && ModuleTNTTimer.INSTANCE.getRunning() && ModuleTNTTimer.INSTANCE.getEsp()) {
            return ModuleTNTTimer.INSTANCE.getTntColor(tntEntity.getFuse()).argb();
        } else if (ModuleStorageESP.GlowMode.INSTANCE.getRunning()) {
            var category = ModuleStorageESP.categorize(entity);
            if (category != null && category.shouldRender(entity)) {
                return category.getColor().argb();
            }
        }

        return operation.call(entity);
    }

}
