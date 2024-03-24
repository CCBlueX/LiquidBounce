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
