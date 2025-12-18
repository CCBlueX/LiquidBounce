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
import com.mojang.blaze3d.pipeline.RenderTarget;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderTarget.class)
public abstract class MixinRenderTarget implements FramebufferAddition {

  @Shadow
  @Nullable
  protected GpuTexture colorTexture;

  @Shadow
  @Nullable
  protected GpuTexture depthTexture;

  @Shadow
  public abstract @Nullable GpuTexture getColorTexture();

  @Shadow
  public abstract @Nullable GpuTexture getDepthTexture();

  @Override
  public @Nullable GpuTexture liquidbounce$setColorAttachment(@Nullable GpuTexture texture) {
    var old = this.getColorTexture();
    this.colorTexture = texture;
    return old;
  }

  @Override
  public @Nullable GpuTexture liquidbounce$setDepthAttachment(@Nullable GpuTexture texture) {
    var old = this.getDepthTexture();
    this.depthTexture = texture;
    return old;
  }

}
