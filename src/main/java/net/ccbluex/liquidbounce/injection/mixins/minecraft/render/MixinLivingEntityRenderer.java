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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleRotations;
import net.ccbluex.liquidbounce.utils.aiming.Rotation;
import net.ccbluex.liquidbounce.utils.aiming.RotationManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public class MixinLivingEntityRenderer<T extends LivingEntity> {

    private final ThreadLocal<Rotation> currentRotation = ThreadLocal.withInitial(() -> null);

    @Inject(method = "render", at = @At("HEAD"))
    private void injectRender(T livingEntity, float f, float g, MatrixStack matrixStack, VertexConsumerProvider vertexConsumerProvider, int i, CallbackInfo ci) {
        Rotation currentRotation = RotationManager.INSTANCE.getCurrentRotation();

        if (!ModuleRotations.INSTANCE.getEnabled() || livingEntity != MinecraftClient.getInstance().player || currentRotation == null) {
            this.currentRotation.set(null);
            return;
        }

        this.currentRotation.set(currentRotation);
    }

    /**
     * Yaw injection hook
     *
     * float h = MathHelper.lerpAngleDegrees(g, livingEntity.prevBodyYaw, livingEntity.bodyYaw);
     * float j = MathHelper.lerpAngleDegrees(g, livingEntity.prevHeadYaw, livingEntity.headYaw);
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 0))
    private float injectRotationsA(float g, float f, float s) {
        Rotation rot = currentRotation.get();
        if (rot != null) {
            return MathHelper.lerpAngleDegrees(g, rot.getYaw(), rot.getYaw());
        } else {
            return MathHelper.lerpAngleDegrees(g, f, s);
        }
    }

    /**
     * Yaw injection hook
     *
     * float h = MathHelper.lerpAngleDegrees(g, livingEntity.prevBodyYaw, livingEntity.bodyYaw);
     * float j = MathHelper.lerpAngleDegrees(g, livingEntity.prevHeadYaw, livingEntity.headYaw);
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F", ordinal = 1))
    private float injectRotationsB(float g, float f, float s) {
        Rotation rot = currentRotation.get();
        if (rot != null) {
            return MathHelper.lerpAngleDegrees(g, rot.getYaw(), rot.getYaw());
        } else {
            return MathHelper.lerpAngleDegrees(g, f, s);
        }
    }

    /**
     * Pitch injection hook
     *
     * float m = MathHelper.lerp(g, livingEntity.prevPitch, livingEntity.getPitch());
     */
    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerp(FFF)F", ordinal = 0))
    private float injectRotationsC(float g, float f, float s) {
        Rotation rot = currentRotation.get();
        if (rot != null) {
            return MathHelper.lerp(g, rot.getPitch(), rot.getPitch());
        } else {
            return MathHelper.lerp(g, f, s);
        }
    }

}
