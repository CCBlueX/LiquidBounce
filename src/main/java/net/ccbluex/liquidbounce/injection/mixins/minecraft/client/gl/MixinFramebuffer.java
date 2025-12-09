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

package net.ccbluex.liquidbounce.injection.mixins.minecraft.client.gl;

import com.mojang.blaze3d.textures.GpuTexture;
import net.ccbluex.liquidbounce.additions.FramebufferAddition;
import net.minecraft.client.gl.Framebuffer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Framebuffer.class)
public abstract class MixinFramebuffer implements FramebufferAddition {

  @Shadow
  @Nullable
  protected GpuTexture colorAttachment;

  @Shadow
  @Nullable
  protected GpuTexture depthAttachment;

  @Shadow
  public abstract @Nullable GpuTexture getColorAttachment();

  @Shadow
  public abstract @Nullable GpuTexture getDepthAttachment();

  @Override
  public @Nullable GpuTexture liquidbounce$setColorAttachment(@Nullable GpuTexture texture) {
    var old = this.getColorAttachment();
    this.colorAttachment = texture;
    return old;
  }

  @Override
  public @Nullable GpuTexture liquidbounce$setDepthAttachment(@Nullable GpuTexture texture) {
    var old = this.getDepthAttachment();
    this.depthAttachment = texture;
    return old;
  }

}
