package net.ccbluex.liquidbounce.injection.mixins.minecraft.render;

import net.ccbluex.liquidbounce.features.module.modules.render.DoRender;
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleAntiBlind;
import net.minecraft.client.render.block.BlockRenderManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderManager.class)
public class MixinBlockRenderManager {
    @Inject(method = "renderDamage", at = @At("HEAD"), cancellable = true)
    private void hookRenderDamage(CallbackInfo ci) {
        if (!ModuleAntiBlind.canRender(DoRender.BLOCK_BREAK_OVERLAY)) {
            ci.cancel();
        }
    }
}
