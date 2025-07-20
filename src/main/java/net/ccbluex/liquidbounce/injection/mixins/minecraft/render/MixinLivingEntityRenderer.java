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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory;
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleFreeCam;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleLogoffSpot;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.Pair;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Unique
    private static final int ESP_TRUE_SIGHT_REQUIREMENT_COLOR = new Color4b(255, 255, 255, 255).alpha(100).toARGB();

    @Shadow
    public abstract Identifier getTexture(S state);

    @Unique
    private Pair<Rotation, Rotation> getOverwriteRotation(ModuleRotations.BodyPart bodyPart) {
        if (ModuleRotations.INSTANCE.getRunning() && ModuleRotations.INSTANCE.isPartAllowed(bodyPart)) {
            var rotation = ModuleRotations.INSTANCE.getModelRotation();
            var prevRotation = ModuleRotations.INSTANCE.getPrevModelRotation();

            if (rotation != null && prevRotation != null) {
                return new Pair<>(prevRotation, rotation);
            }
        }

        if (ModuleFreeCam.INSTANCE.getRunning()) {
            var serverRotation = RotationManager.INSTANCE.getServerRotation();
            return new Pair<>(serverRotation, serverRotation);
        }

        return null;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;clampBodyYaw(Lnet/minecraft/entity/LivingEntity;FF)F"))
    private float hookBodyYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != MinecraftClient.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation(ModuleRotations.BodyPart.BODY);
        if (overwriteRotation != null) {
            return MathHelper.lerpAngleDegrees(tickDelta, overwriteRotation.getLeft().getYaw(), overwriteRotation.getRight().getYaw());
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    private float hookHeadYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != MinecraftClient.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation(ModuleRotations.BodyPart.HEAD);
        if (overwriteRotation != null) {
            return MathHelper.lerpAngleDegrees(tickDelta, overwriteRotation.getLeft().getYaw(), overwriteRotation.getRight().getYaw());
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float hookPitch(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != MinecraftClient.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation(ModuleRotations.BodyPart.HEAD);
        if (overwriteRotation != null) {
            return MathHelper.lerpAngleDegrees(tickDelta, overwriteRotation.getLeft().getPitch(), overwriteRotation.getRight().getPitch());
        }

        return original;
    }

    @WrapOperation(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void injectTrueSight(EntityModel instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int light, int overlay, int color, Operation<Void> original, @Local(argsOnly = true) S livingEntityRenderState) {
        if (ModuleLogoffSpot.INSTANCE.isLogoffEntity(livingEntityRenderState)) {
            color = ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
        }

        var trueSightModule = ModuleTrueSight.INSTANCE;
        var trueSight = trueSightModule.getRunning() && trueSightModule.getEntities();
        if (ModuleTrueSight.canRenderEntities(livingEntityRenderState)) {
            color = trueSight ? trueSightModule.getEntityColor().toARGB() : ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
        }
        original.call(instance, matrixStack, vertexConsumer, light, overlay, color);
    }

    @ModifyReturnValue(method = "getRenderLayer", at = @At("RETURN"))
    private RenderLayer injectTrueSight(RenderLayer original, S state, boolean showBody, boolean translucent, boolean showOutline) {
        if (ModuleLogoffSpot.INSTANCE.isLogoffEntity(state)) {
            return RenderLayer.getItemEntityTranslucentCull(this.getTexture(state));
        }

        if (ModuleTrueSight.canRenderEntities(state) && !showBody && !translucent && !showOutline) {
            state.invisible = false;
            return RenderLayer.getItemEntityTranslucentCull(this.getTexture(state));
        }
        return original;
    }

    @ModifyReturnValue(method = "shouldFlipUpsideDown", at = @At("RETURN"))
    private static boolean injectShouldFlipUpsideDown(boolean original, LivingEntity entity) {
        if (!(entity instanceof AbstractClientPlayerEntity)) {
            return original;
        }

        return CosmeticService.INSTANCE.hasCosmetic(entity.getUuid(), CosmeticCategory.DINNERBONE);
    }

}
