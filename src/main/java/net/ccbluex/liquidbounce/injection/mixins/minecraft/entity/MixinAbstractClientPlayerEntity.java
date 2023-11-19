package net.ccbluex.liquidbounce.injection.mixins.minecraft.entity;

import net.ccbluex.liquidbounce.features.module.modules.render.ModuleNoFov;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class MixinAbstractClientPlayerEntity {

    @Inject(method = "getFovMultiplier", cancellable = true, at = @At("RETURN"))
    private void injectFovMultiplier(CallbackInfoReturnable<Float> cir) {
        if (ModuleNoFov.INSTANCE.getEnabled())
            cir.setReturnValue(ModuleNoFov.INSTANCE.getFov(cir.getReturnValue()));
    }
}
