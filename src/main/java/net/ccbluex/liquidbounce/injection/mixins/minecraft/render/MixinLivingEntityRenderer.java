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
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.ccbluex.liquidbounce.api.models.cosmetics.CosmeticCategory;
import net.ccbluex.liquidbounce.features.cosmetic.CosmeticService;
import net.ccbluex.liquidbounce.features.module.modules.render.*;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.ccbluex.liquidbounce.render.engine.type.Color4b;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Tuple;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jspecify.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@NullMarked
@Mixin(LivingEntityRenderer.class)
public abstract class MixinLivingEntityRenderer<T extends LivingEntity, S extends LivingEntityRenderState, M extends EntityModel<? super S>> {

    @Unique
    private static final int ESP_TRUE_SIGHT_REQUIREMENT_COLOR = new Color4b(255, 255, 255, 100).argb();

    @Shadow
    public abstract Identifier getTextureLocation(S state);

    @Unique
    private @Nullable Tuple<Rotation, Rotation> getOverwriteRotation(ModuleRotations.BodyPart bodyPart) {
        if (ModuleRotations.INSTANCE.getRunning() && ModuleRotations.INSTANCE.isPartAllowed(bodyPart)) {
            var rotation = ModuleRotations.INSTANCE.getModelRotation();
            var prevRotation = ModuleRotations.INSTANCE.getPrevModelRotation();

            if (rotation != null && prevRotation != null) {
                return new Tuple<>(prevRotation, rotation);
            }
        }

        if (ModuleFreeCam.INSTANCE.getRunning()) {
            var serverRotation = RotationManager.INSTANCE.getServerRotation();
            return new Tuple<>(serverRotation, serverRotation);
        }

        return null;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;solveBodyRot(Lnet/minecraft/world/entity/LivingEntity;FF)F"))
    private float hookBodyYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != Minecraft.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation(ModuleRotations.BodyPart.BODY);
        if (overwriteRotation != null) {
            return Mth.rotLerp(tickDelta, overwriteRotation.getA().yRot(), overwriteRotation.getB().yRot());
        }

        return original;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;rotLerp(FFF)F"))
    private float hookHeadYaw(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != Minecraft.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation(ModuleRotations.BodyPart.HEAD);
        if (overwriteRotation != null) {
            return Mth.rotLerp(tickDelta, overwriteRotation.getA().yRot(), overwriteRotation.getB().yRot());
        }

        return original;
    }

    @ModifyExpressionValue(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getXRot(F)F"))
    private float hookPitch(float original, LivingEntity entity, S state, float tickDelta) {
        if (entity != Minecraft.getInstance().player) {
            return original;
        }

        var overwriteRotation = getOverwriteRotation(ModuleRotations.BodyPart.HEAD);
        if (overwriteRotation != null) {
            return Mth.rotLerp(tickDelta, overwriteRotation.getA().xRot(), overwriteRotation.getB().xRot());
        }

        return original;
    }

    @WrapOperation(method = "submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SubmitNodeCollector;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/rendertype/RenderType;IIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;ILnet/minecraft/client/renderer/feature/ModelFeatureRenderer$CrumblingOverlay;)V"))
    private void injectTrueSight(
        SubmitNodeCollector instance, Model<M> model,
        Object o, PoseStack matrixStack,
        RenderType renderLayer, int light,
        int overlay, int tintedColor,
        TextureAtlasSprite sprite, int outlineColor,
        ModelFeatureRenderer.CrumblingOverlay crumblingOverlayCommand, Operation<Void> original,
        @Local(argsOnly = true) S livingEntityRenderState
    ) {
        if (ModuleLogoffSpot.INSTANCE.isLogoffEntity(livingEntityRenderState)) {
            tintedColor = ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
        }

        var trueSightModule = ModuleTrueSight.INSTANCE;
        var trueSight = trueSightModule.getRunning() && trueSightModule.getEntities();
        if (ModuleTrueSight.canRenderEntities(livingEntityRenderState)) {
            tintedColor = trueSight ? trueSightModule.getEntityColor().argb() : ESP_TRUE_SIGHT_REQUIREMENT_COLOR;
        }
        original.call(
            instance, model,
            o, matrixStack,
            renderLayer, light,
            overlay, tintedColor,
            sprite, outlineColor,
            crumblingOverlayCommand
        );
    }

    @ModifyReturnValue(method = "getRenderType", at = @At("RETURN"))
    private RenderType injectTrueSight(RenderType original, S state, boolean showBody, boolean translucent, boolean showOutline) {
        if (ModuleLogoffSpot.INSTANCE.isLogoffEntity(state)) {
            return RenderTypes.itemEntityTranslucentCull(this.getTextureLocation(state));
        }

        if (ModuleTrueSight.canRenderEntities(state) && !showBody && !translucent && !showOutline) {
            state.isInvisible = false;
            return RenderTypes.itemEntityTranslucentCull(this.getTextureLocation(state));
        }
        return original;
    }

    @ModifyReturnValue(method = "isEntityUpsideDown(Lnet/minecraft/world/entity/LivingEntity;)Z", at = @At("RETURN"))
    private static boolean injectShouldFlipUpsideDown(boolean original, LivingEntity entity) {
        if (!(entity instanceof AbstractClientPlayer)) {
            return original;
        }

        return original || CosmeticService.INSTANCE.hasCosmetic(entity.getUUID(), CosmeticCategory.DINNERBONE);
    }

    // Chams

    @ModifyExpressionValue(
        method = "getRenderType",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/model/EntityModel;renderType(Lnet/minecraft/resources/Identifier;)Lnet/minecraft/client/renderer/rendertype/RenderType;")
    )
    private @Nullable RenderType render_Chams(
        @Nullable RenderType original,
        @Local(argsOnly = true) S state,
        @Local Identifier identifier
    ) {
        if (original == null) return null;

        var entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();

        if (ModuleChams.INSTANCE.getRunning() && CombatExtensionsKt.shouldBeAttacked(entity)) {
            RenderSetup renderSetup = ((MixinRenderTypeAccessor) original).getState();
            boolean affectsOutline = ((MixinRenderSetupAccessor) (Object) renderSetup).getOutlineProperty() == RenderSetup.OutlineProperty.AFFECTS_OUTLINE;

            switch (((MixinRenderTypeAccessor) original).getName()) {
                case "entity_translucent" -> {
                    return ModuleChams.ENTITY_TRANSLUCENT.apply(identifier, affectsOutline);
                }
                case "entity_cutout" -> {
                    return ModuleChams.ENTITY_CUTOUT.apply(identifier);
                }
                case "entity_cutout_no_cull" -> {
                    return ModuleChams.ENTITY_CUTOUT_NO_CULL.apply(identifier, affectsOutline);
                }
                default -> {
                    return original;
                }
            }
        }

        return original;
    }

    // FreeCam
    @ModifyExpressionValue(method = "shouldShowName(Lnet/minecraft/world/entity/LivingEntity;D)Z", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;getCameraEntity()Lnet/minecraft/world/entity/Entity;"))
    private @Nullable Entity hasLabelGetCameraEntityProxy(@Nullable Entity cameraEntity) {
        return ModuleFreeCam.INSTANCE.getRunning() ? null : cameraEntity;
    }
}
