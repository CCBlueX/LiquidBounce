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

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Fog;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import static net.ccbluex.liquidbounce.utils.render.RenderExtensionsKt.SAMPLER_NAMES;

@Mixin(ShaderProgram.class)
public abstract class MixinShaderProgram {

  @Shadow
  public abstract void addSamplerTexture(String name, GpuTexture texture);

  @Shadow
  @Nullable
  public GlUniform modelViewMat;
  @Shadow
  @Nullable
  public GlUniform projectionMat;
  @Shadow
  @Nullable
  public GlUniform colorModulator;
  @Shadow
  @Nullable
  public GlUniform glintAlpha;
  @Shadow
  @Nullable
  public GlUniform fogStart;
  @Shadow
  @Nullable
  public GlUniform fogEnd;
  @Shadow
  @Nullable
  public GlUniform fogColor;
  @Shadow
  @Nullable
  public GlUniform fogShape;
  @Shadow
  @Nullable
  public GlUniform textureMat;
  @Shadow
  @Nullable
  public GlUniform gameTime;
  @Shadow
  @Nullable
  public GlUniform screenSize;
  @Shadow
  @Nullable
  public GlUniform lineWidth;

  @Shadow
  @Nullable
  public GlUniform modelOffset;
  @Shadow
  @Nullable
  public GlUniform light0Direction;
  @Shadow
  @Nullable
  public GlUniform light1Direction;

  /**
   * @author MukjepScarlet
   * @reason Mojang's super code creates tons of byte[] and String: `"Sampler" + i`
   */
  @Overwrite

  public void initializeUniforms(VertexFormat.DrawMode drawMode, Matrix4f viewMatrix, Matrix4f projectionMatrix, float screenWidth, float screenHeight) {
    for(int i = 0; i < 12; ++i) {
      GpuTexture gpuTexture = RenderSystem.getShaderTexture(i);
      this.addSamplerTexture(SAMPLER_NAMES[i], gpuTexture);
    }

    if (this.modelViewMat != null) {
      this.modelViewMat.set(viewMatrix);
    }

    if (this.projectionMat != null) {
      this.projectionMat.set(projectionMatrix);
    }

    if (this.colorModulator != null) {
      this.colorModulator.set(RenderSystem.getShaderColor());
    }

    if (this.glintAlpha != null) {
      this.glintAlpha.set(RenderSystem.getShaderGlintAlpha());
    }

    Fog fog = RenderSystem.getShaderFog();
    if (this.fogStart != null) {
      this.fogStart.set(fog.start());
    }

    if (this.fogEnd != null) {
      this.fogEnd.set(fog.end());
    }

    if (this.fogColor != null) {
      this.fogColor.setAndFlip(fog.red(), fog.green(), fog.blue(), fog.alpha());
    }

    if (this.fogShape != null) {
      this.fogShape.set(fog.shape().getId());
    }

    if (this.textureMat != null) {
      this.textureMat.set(RenderSystem.getTextureMatrix());
    }

    if (this.gameTime != null) {
      this.gameTime.set(RenderSystem.getShaderGameTime());
    }

    if (this.modelOffset != null) {
      this.modelOffset.set(RenderSystem.getModelOffset());
    }

    if (this.screenSize != null) {
      this.screenSize.set(screenWidth, screenHeight);
    }

    if (this.lineWidth != null && (drawMode == VertexFormat.DrawMode.LINES || drawMode == VertexFormat.DrawMode.LINE_STRIP)) {
      this.lineWidth.set(RenderSystem.getShaderLineWidth());
    }

    Vector3f[] vector3fs = RenderSystem.getShaderLights();
    if (this.light0Direction != null) {
      this.light0Direction.set(vector3fs[0]);
    }

    if (this.light1Direction != null) {
      this.light1Direction.set(vector3fs[1]);
    }

  }
}
