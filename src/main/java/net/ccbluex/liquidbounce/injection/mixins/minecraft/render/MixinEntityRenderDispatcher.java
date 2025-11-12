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

import com.google.common.collect.ImmutableList;
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleHitbox;
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition;
import net.ccbluex.liquidbounce.utils.combat.CombatExtensionsKt;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.client.render.entity.state.EntityHitbox;
import net.minecraft.client.render.entity.state.EntityHitboxAndView;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderManager.class)
public abstract class MixinEntityRenderDispatcher {
    @Unique
    private static Entity $entity = null;

    @Inject(method = "render", at = @At(value = "HEAD"))
    private static  <S extends EntityRenderState> void getEntity(S state, CameraRenderState cameraRenderState, double d, double e, double f, MatrixStack matrixStack, OrderedRenderCommandQueue orderedRenderCommandQueue, CallbackInfo ci) {
        MixinEntityRenderDispatcher.$entity = ((EntityRenderStateAddition) state).liquid_bounce$getEntity();
    }

    @ModifyArg(
            method = "render",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitDebugHitbox(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/entity/state/EntityRenderState;Lnet/minecraft/client/render/entity/state/EntityHitboxAndView;)V"),
            index = 2
    )
    private static EntityHitboxAndView updateBoundingBox(EntityHitboxAndView hbv) {
        var moduleHitBox = ModuleHitbox.INSTANCE;
        if ($entity != null && moduleHitBox.getRunning() && CombatExtensionsKt.shouldBeAttacked($entity)) {
            var expansion = moduleHitBox.getSize();
            // TODO(1.21.10-port): this probably isn't correct either.
            return new EntityHitboxAndView(
                hbv.viewX(),
                hbv.viewY(),
                hbv.viewZ(),
                hbv.hitboxes().stream().map(hb -> new EntityHitbox(
                    hb.x0() - expansion,
                    hb.y0() - expansion,
                    hb.z0() - expansion,
                    hb.x1() + expansion,
                    hb.y1() + expansion,
                    hb.z1() + expansion,
                    hb.offsetX(),
                    hb.offsetY(),
                    hb.offsetZ(),
                    hb.red(),
                    hb.green(),
                    hb.blue()
                )).collect(ImmutableList.toImmutableList())
            );
        }
        return hbv;
    }
}
