package net.ccbluex.liquidbounce.injection.mixins.minecraft.world.chuck;

import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.world.chunk.light.ChunkSkyLightProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkSkyLightProvider.class)
public class MixinChunkSkyLightProvider {
    @Inject(at = @At("HEAD"), method = "method_51531", cancellable = true)
    private void hookNoBlindSkyLightUpdates(long blockPos, long l, int lightLevel, CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.SKYLIGHT_UPDATES)) {
            ci.cancel();
        }
    }
}
