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
import net.minecraft.client.gl.GlUniform;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.render.Fog;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.util.Window;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ShaderProgram.class)
public abstract class MixinShaderProgram {

  @Shadow
  public abstract void addSamplerTexture(String name, int texture);

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

  @Unique
  private static final String[] SAMPLER_NAMES = new String[12];
  static {
      for (int i = 0; i < 12; i++) {
          SAMPLER_NAMES[i] = "Sampler" + i;
      }
  }

  /**
   * @author MukjepScarlet
   * @reason Mojang's super code creates tons of byte[] and String: `"Sampler" + i`
   */
  @Overwrite
  public void initializeUniforms(VertexFormat.DrawMode drawMode, Matrix4f viewMatrix, Matrix4f projectionMatrix, Window window) {
    for (int i = 0; i < 12; i++) {
      int j = RenderSystem.getShaderTexture(i);
      this.addSamplerTexture(SAMPLER_NAMES[i], j);
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

    if (this.screenSize != null) {
      this.screenSize.set((float)window.getFramebufferWidth(), (float)window.getFramebufferHeight());
    }

    if (this.lineWidth != null && (drawMode == VertexFormat.DrawMode.LINES || drawMode == VertexFormat.DrawMode.LINE_STRIP)) {
      this.lineWidth.set(RenderSystem.getShaderLineWidth());
    }

    RenderSystem.setupShaderLights((ShaderProgram) (Object) this);
  }

}
