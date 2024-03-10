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
