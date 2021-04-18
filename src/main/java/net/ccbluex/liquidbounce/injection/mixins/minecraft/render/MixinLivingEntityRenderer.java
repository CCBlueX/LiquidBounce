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

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevBodyYaw:F"))
    private float injectRotationsA(T entity) {
        Rotation rot = currentRotation.get();

        if (rot != null)
            return rot.getYaw();

        return entity.prevBodyYaw;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;bodyYaw:F"))
    private float injectRotationsB(T entity) {
        Rotation rot = currentRotation.get();

        if (rot != null)
            return rot.getYaw();

        return entity.bodyYaw;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevHeadYaw:F"))
    private float injectRotationsC(T entity) {
        Rotation rot = currentRotation.get();

        if (rot != null)
            return rot.getYaw();

        return entity.prevHeadYaw;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;headYaw:F"))
    private float injectRotationsD(T entity) {
        Rotation rot = currentRotation.get();

        if (rot != null)
            return rot.getYaw();

        return entity.headYaw;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;pitch:F"))
    private float injectRotationsE(T entity) {
        Rotation rot = currentRotation.get();

        if (rot != null)
            return rot.getPitch();

        return entity.pitch;
    }

    @Redirect(method = "render", at = @At(value = "FIELD", target = "Lnet/minecraft/entity/LivingEntity;prevPitch:F"))
    private float injectRotationsF(T entity) {
        Rotation rot = currentRotation.get();

        if (rot != null)
            return rot.getPitch();

        return entity.prevPitch;
    }
}
