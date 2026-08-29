package net.ccbluex.liquidbounce.injection.mixins.blaze3d;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.blaze3d.systems.RenderSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface MixinRenderSystemAccessor {

    @Accessor("currentPipelineCache")
    static PipelineCache getCurrentPipelineCache() {
        throw new AssertionError();
    }

}
