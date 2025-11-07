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

import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleHitbox;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityHitbox;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public abstract class MixinEntityRenderDispatcher {
    @Unique
    private static Entity $entity = null;

    @Inject(method = "render(Lnet/minecraft/entity/Entity;DDDFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;ILnet/minecraft/client/render/entity/EntityRenderer;)V", at = @At(value = "HEAD"))
    private static void getEntity(Entity entity, double x, double y, double z, float tickProgress, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, EntityRenderer<?, ?> renderer, CallbackInfo ci) {
        MixinEntityRenderDispatcher.$entity = entity;
    }

    @ModifyArg(
            method = "renderHitboxes(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/state/EntityHitboxAndView;Lnet/minecraft/client/render/VertexConsumer;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/EntityRenderDispatcher;renderHitbox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;Lnet/minecraft/client/render/entity/state/EntityHitbox;)V"),
            index = 2
    )
    private static EntityHitbox updateBoundingBox(EntityHitbox hitbox) {
        var moduleHitBox = ModuleHitbox.INSTANCE;
        if ($entity != null && moduleHitBox.getRunning() && CombatExtensionsKt.shouldBeAttacked($entity)) {
            var expansion = moduleHitBox.getSize();
            return new EntityHitbox(
                    hitbox.x0() - expansion,
                    hitbox.y0() - expansion,
                    hitbox.z0() - expansion,
                    hitbox.x1() + expansion,
                    hitbox.y1() + expansion,
                    hitbox.z1() + expansion,
                    hitbox.offsetX(),
                    hitbox.offsetY(),
                    hitbox.offsetZ(),
                    hitbox.red(),
                    hitbox.green(),
                    hitbox.blue()
            );
        }
        return hitbox;
    }
}
