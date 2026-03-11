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

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.ccbluex.liquidbounce.interfaces.OutlineBufferSourceSingleDrawAddition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.OutlineBufferSource;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@NullMarked
@Mixin(OutlineBufferSource.class)
public abstract class MixinOutlineBufferSource implements OutlineBufferSourceSingleDrawAddition {
    @Shadow
    @Final
    private MultiBufferSource.BufferSource outlineBufferSource;

    @Shadow
    private int outlineColor;

    @Override
    public @Nullable VertexConsumer liquid_bounce_getSingleDrawBuffers(RenderType layer) {
        var affectedOutline = layer.outline();

        if (affectedOutline.isEmpty()) {
            return null;
        }

        VertexConsumer vertexConsumer = this.outlineBufferSource.getBuffer(affectedOutline.get());

        return new OutlineBufferSource.EntityOutlineGenerator(vertexConsumer, this.outlineColor);
    }
}
