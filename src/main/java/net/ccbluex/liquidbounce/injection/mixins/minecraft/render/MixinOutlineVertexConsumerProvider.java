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
 *
 */
package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.interfaces.OutlineVertexConsumerProviderSingleDrawAddition;
import net.minecraft.client.render.OutlineVertexConsumerProvider;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(OutlineVertexConsumerProvider.class)
public class MixinOutlineVertexConsumerProvider implements OutlineVertexConsumerProviderSingleDrawAddition {
    @Shadow
    @Final
    private VertexConsumerProvider.Immediate plainDrawer;

    @Shadow
    private int red;

    @Shadow
    private int green;

    @Shadow
    private int blue;

    @Shadow
    private int alpha;

    public VertexConsumer liquid_bounce_getSingleDrawBuffers(RenderLayer layer) {
        var affectedOutline = layer.getAffectedOutline();

        if (affectedOutline.isEmpty()) {
            return null;
        }

        VertexConsumer vertexConsumer = this.plainDrawer.getBuffer(affectedOutline.get());

        return new OutlineVertexConsumerProvider.OutlineVertexConsumer(vertexConsumer, this.red, this.green, this.blue, this.alpha);
    }
}
