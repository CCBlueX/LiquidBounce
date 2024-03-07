/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleCombineMobs;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleMobOwners;
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class MixinEntityRenderer<T extends Entity> {

    @Shadow
    @Final
    protected EntityRenderDispatcher dispatcher;

    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void shouldRender(T entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        if (ModuleCombineMobs.INSTANCE.getEnabled() && ModuleCombineMobs.INSTANCE.trackEntity(entity)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void renderMobOwners(T entity, float yaw, float tickDelta, MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        var ownerName = ModuleMobOwners.INSTANCE.getOwnerInfoText(entity);

        if (ownerName != null) {
            renderLabel(entity, ownerName, matrices, vertexConsumers, light);
        }
    }

    @Unique
    private void renderLabel(Entity entity, OrderedText text, MatrixStack matrices,
                             VertexConsumerProvider vertexConsumers, int light) {
        var d = this.dispatcher.getSquaredDistanceToCamera(entity);

        if (d > 4096.0) {
            return;
        }

        var f = entity.getHeight() / 2.0F;

        matrices.push();
        matrices.translate(0.0D, f, 0.0D);
        matrices.multiply(this.dispatcher.getRotation());
        matrices.scale(-0.025F, -0.025F, 0.025F);

        Matrix4f matrix4f = matrices.peek().getPositionMatrix();

        var g = MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.25F);
        var j = (int) (g * 255.0F) << 24;
        var textRenderer = this.getTextRenderer();
        var h = (float) (-textRenderer.getWidth(text) / 2);

        textRenderer.draw(text, h, 0, -1, false, matrix4f, vertexConsumers,
                TextRenderer.TextLayerType.NORMAL, j, light);
        matrices.pop();
    }

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void disableDuplicateNametagsAndInjectMobOwners(T entity, Text text, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float tickDelta, CallbackInfo ci) {
        // Don't render nametags
        if (ModuleNametags.INSTANCE.getEnabled() && ModuleNametags.shouldRenderNametag(entity)) {
            ci.cancel();
        }
    }

}
