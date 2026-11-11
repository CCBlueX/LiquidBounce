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

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ccbluex.liquidbounce.features.client.view.ClientViews;
import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.extract.LevelExtractor;
import net.minecraft.client.renderer.extract.TransientBlock;
import net.minecraft.client.renderer.state.level.ParticlesRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every level reports its changes to the extractor it was created with, the window's. A background client's
 * changes go to its view instead, or nowhere if it has none.
 */
@Mixin(LevelExtractor.class)
public abstract class MixinLevelExtractor {

    @WrapMethod(method = "blockChanged")
    private void forwardBlockChanged(BlockPos pos, int updateFlags, Operation<Void> original) {
        var receiver = ClientViews.receiverOf((LevelExtractor) (Object) this);
        if (receiver == (Object) this) {
            original.call(pos, updateFlags);
        } else if (receiver != null) {
            receiver.blockChanged(pos, updateFlags);
        }
    }

    @WrapMethod(method = "setBlocksDirty")
    private void forwardBlocksDirty(int x0, int y0, int z0, int x1, int y1, int z1, Operation<Void> original) {
        var receiver = ClientViews.receiverOf((LevelExtractor) (Object) this);
        if (receiver == (Object) this) {
            original.call(x0, y0, z0, x1, y1, z1);
        } else if (receiver != null) {
            receiver.setBlocksDirty(x0, y0, z0, x1, y1, z1);
        }
    }

    @WrapMethod(method = "setBlockDirty(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;)V")
    private void forwardBlockDirty(BlockPos pos, BlockState oldState, BlockState newState, Operation<Void> original) {
        var receiver = ClientViews.receiverOf((LevelExtractor) (Object) this);
        if (receiver == (Object) this) {
            original.call(pos, oldState, newState);
        } else if (receiver != null) {
            receiver.setBlockDirty(pos, oldState, newState);
        }
    }

    @WrapMethod(method = "setSectionDirtyWithNeighbors")
    private void forwardSectionDirtyWithNeighbors(int x, int y, int z, Operation<Void> original) {
        var receiver = ClientViews.receiverOf((LevelExtractor) (Object) this);
        if (receiver == (Object) this) {
            original.call(x, y, z);
        } else if (receiver != null) {
            receiver.setSectionDirtyWithNeighbors(x, y, z);
        }
    }

    @WrapMethod(method = "setSectionRangeDirty")
    private void forwardSectionRangeDirty(int x0, int y0, int z0, int x1, int y1, int z1, Operation<Void> original) {
        var receiver = ClientViews.receiverOf((LevelExtractor) (Object) this);
        if (receiver == (Object) this) {
            original.call(x0, y0, z0, x1, y1, z1);
        } else if (receiver != null) {
            receiver.setSectionRangeDirty(x0, y0, z0, x1, y1, z1);
        }
    }

    @WrapMethod(method = "setSectionDirty(III)V")
    private void forwardSectionDirty(int x, int y, int z, Operation<Void> original) {
        var receiver = ClientViews.receiverOf((LevelExtractor) (Object) this);
        if (receiver == (Object) this) {
            original.call(x, y, z);
        } else if (receiver != null) {
            receiver.setSectionDirty(x, y, z);
        }
    }

    @WrapMethod(method = "queueTransientBlock")
    private void forwardTransientBlock(TransientBlock transientBlock, Operation<Void> original) {
        var receiver = ClientViews.receiverOf((LevelExtractor) (Object) this);
        if (receiver == (Object) this) {
            original.call(transientBlock);
        } else if (receiver != null) {
            receiver.queueTransientBlock(transientBlock);
        }
    }

    /**
     * The particle engine belongs to the active client's level, a view has none.
     */
    @WrapOperation(method = "extract", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/particle/ParticleEngine;extract(Lnet/minecraft/client/renderer/state/level/ParticlesRenderState;Lnet/minecraft/client/renderer/culling/Frustum;Lnet/minecraft/client/Camera;F)V"))
    private void skipParticlesInViews(ParticleEngine engine, ParticlesRenderState state, Frustum frustum, Camera camera,
                                      float partialTicks, Operation<Void> original) {
        if (ClientViews.isView((LevelExtractor) (Object) this)) {
            state.reset();
        } else {
            original.call(engine, state, frustum, camera, partialTicks);
        }
    }

    /**
     * Views are built again with the new resources once they are watched.
     */
    @Inject(method = "onResourceManagerReload", at = @At("HEAD"))
    private void closeViews(ResourceManager resourceManager, CallbackInfo ci) {
        if (!ClientViews.isView((LevelExtractor) (Object) this)) {
            ClientViews.closeAll();
        }
    }

}
