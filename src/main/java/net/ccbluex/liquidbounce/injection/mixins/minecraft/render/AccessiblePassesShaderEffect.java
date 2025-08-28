package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;


import net.minecraft.client.gl.PostEffectPass;
import net.minecraft.client.gl.PostEffectProcessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(PostEffectProcessor.class)
public interface AccessiblePassesShaderEffect {
    @Accessor
    List<PostEffectPass> getPasses();
}
