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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.renderer;

import com.mojang.blaze3d.buffers.GpuFence;
import net.ccbluex.liquidbounce.additions.MappableRingBufferAddition;
import net.minecraft.client.input.InputQuirks;
import net.minecraft.client.renderer.MappableRingBuffer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@NullMarked
@Mixin(MappableRingBuffer.class)
public abstract class MixinMappableRingBuffer implements MappableRingBufferAddition {

    @Shadow
    @Final
    private @Nullable GpuFence[] fences;

    @Shadow
    public abstract void rotate();

    @Shadow
    private int current;

    /**
     * <a href="https://github.com/CCBlueX/LiquidBounce/issues/7721">GitHub Issue</a>
     */
    @ModifyArg(
        method = "currentBuffer",
        at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/buffers/GpuFence;awaitCompletion(J)Z")
    )
    private long changeAwaitTimeout(long original) {
        if (InputQuirks.REPLACE_CTRL_KEY_WITH_CMD_KEY) {
            return 50L;
        }
        return original;
    }

    @Unique
    @Override
    public boolean liquidBounce$isSafeForClose() {
        for (GpuFence fence : this.fences) {
            if (fence != null && !fence.awaitCompletion(0L)) {
                return false;
            }
        }
        return true;
    }

    @Unique
    @Override
    public void liquidBounce$awaitAndRotate() {
        GpuFence currentFence = this.fences[this.current];
        if (currentFence != null) {
            currentFence.awaitCompletion(Long.MAX_VALUE);
        }

        this.rotate();
    }
}
