package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.interfaces.PostEffectPassTextureAddition;
import net.minecraft.client.gl.JsonEffectShaderProgram;
import net.minecraft.client.gl.PostEffectPass;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(PostEffectPass.class)
public class MixinPostEffectPass implements PostEffectPassTextureAddition {
    @Shadow
    @Final
    private JsonEffectShaderProgram program;
    @Unique
    private final Map<String, Integer> textureSamplerMap = new HashMap<>();

    @Override
    public void liquid_bounce$setTextureSampler(String name, int textureId) {
        this.textureSamplerMap.put(name, textureId);
    }

    @Inject(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gl/Framebuffer;endWrite()V", ordinal = 0))
    private void injectTextureSamplerMap(float time, CallbackInfo ci) {
        for (Map.Entry<String, Integer> stringIntegerEntry : this.textureSamplerMap.entrySet()) {
            this.program.bindSampler(stringIntegerEntry.getKey(), stringIntegerEntry::getValue);
        }
    }

}
