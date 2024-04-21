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
package net.ccbluex.liquidbounce.interfaces;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;

import javax.annotation.Nullable;

public interface OutlineVertexConsumerProviderSingleDrawAddition {
    /**
     * {@link net.minecraft.client.render.OutlineVertexConsumerProvider#getBuffer(RenderLayer)} creates a consumer which
     * renders to the outline framebuffer but also to the original framebuffer.
     * <p>
     * If you only want to render to the outline framebuffer, use this method.
     */
    @Nullable
    VertexConsumer liquid_bounce_getSingleDrawBuffers(RenderLayer layer);
}
