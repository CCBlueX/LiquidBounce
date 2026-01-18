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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.WorldEntityRemoveEvent;
import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleTrueSight;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ExplosionParticleInfo;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public abstract class MixinClientLevel {

    @ModifyReturnValue(method = "getMarkerParticleTarget", at = @At("RETURN"))
    private Block injectBlockParticle(Block original) {
        if (ModuleTrueSight.INSTANCE.getRunning() && ModuleTrueSight.INSTANCE.getBarriers()) {
            return Blocks.BARRIER;
        }
        return original;
    }

    @Inject(method = "addParticle(Lnet/minecraft/core/particles/ParticleOptions;DDDDDD)V", at = @At("HEAD"), cancellable = true)
    private void injectNoExplosionParticles(ParticleOptions parameters, double x, double y, double z, double velocityX, double velocityY, double velocityZ, CallbackInfo ci) {
        var type = parameters.getType();
        if (!ModuleAntiBlind.canRender(DoRender.EXPLOSION_PARTICLES) && (type == ParticleTypes.EXPLOSION || type == ParticleTypes.EXPLOSION_EMITTER)) {
            ci.cancel();
        }
    }

    @Inject(method = "removeEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;onClientRemoval()V"))
    private void injectRemoveEntity(int entityId, Entity.RemovalReason removalReason, CallbackInfo ci, @Local Entity entity) {
        EventManager.INSTANCE.callEvent(new WorldEntityRemoveEvent(entity));
    }

    @Inject(method = "trackExplosionEffects", at = @At("HEAD"), cancellable = true)
    private void hookAddBlockParticleEffects(
        Vec3 center, float radius, int blockCount, WeightedList<ExplosionParticleInfo> particles, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.BLOCK_BREAK_PARTICLES)) {
            ci.cancel();
        }
    }

    @Inject(method = "addDestroyBlockEffect", at = @At("HEAD"), cancellable = true)
    private void hookAddBlockBreakParticles(BlockPos pos, BlockState state, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.BLOCK_BREAK_PARTICLES)) {
            ci.cancel();
        }
    }
}
