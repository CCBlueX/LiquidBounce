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

import net.ccbluex.liquidbounce.interfaces.IMixinGameRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Matrix4f;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(GameRenderer.class)
public abstract class MixinGameRenderer implements IMixinGameRenderer {

    @Shadow
    @Final
    private Camera camera;
    @Shadow
    @Final
    private MinecraftClient client;
    @Shadow
    private int ticks;

    @Shadow
    public abstract Matrix4f getBasicProjectionMatrix(Camera camera, float f, boolean bl);

    @Shadow
    protected abstract void bobViewWhenHurt(MatrixStack matrixStack, float f);

    @Shadow
    protected abstract void bobView(MatrixStack matrixStack, float f);

    @Override
    public Matrix4f getCameraMVPMatrix(float tickDelta, boolean bobbing) {
        MatrixStack matrixStack = new MatrixStack();

        matrixStack.peek().getModel().multiply(this.getBasicProjectionMatrix(this.camera, tickDelta, true));

        if (bobbing) {
            this.bobViewWhenHurt(matrixStack, tickDelta);

            if (this.client.options.bobView) {
                this.bobView(matrixStack, tickDelta);
            }

            float f = MathHelper.lerp(tickDelta,
                                      this.client.player.lastNauseaStrength,
                                      this.client.player.nextNauseaStrength) * this.client.options.distortionEffectScale * this.client.options.distortionEffectScale;

            if (f > 0.0F) {
                int i = this.client.player.hasStatusEffect(StatusEffects.NAUSEA) ? 7 : 20;

                float g = 5.0F / (f * f + 5.0F) - f * 0.04F;

                g *= g;

                Vector3f vector3f = new Vector3f(0.0F,
                                                 MathHelper.SQUARE_ROOT_OF_TWO / 2.0F,
                                                 MathHelper.SQUARE_ROOT_OF_TWO / 2.0F);
                matrixStack.multiply(vector3f.getDegreesQuaternion(((float) this.ticks + tickDelta) * (float) i));
                matrixStack.scale(1.0F / g, 1.0F, 1.0F);
                float h = -((float) this.ticks + tickDelta) * (float) i;
                matrixStack.multiply(vector3f.getDegreesQuaternion(h));
            }
        }

        matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(camera.getPitch()));
        matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(camera.getYaw() + 180.0F));

        Vec3d pos = this.camera.getPos();

        Matrix4f model = matrixStack.peek().getModel();

        model.multiply(Matrix4f.translate(-(float) pos.x, -(float) pos.y, -(float) pos.z));

        return model;
    }
}
