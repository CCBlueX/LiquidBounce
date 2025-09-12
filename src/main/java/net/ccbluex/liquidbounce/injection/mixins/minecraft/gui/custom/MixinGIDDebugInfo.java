package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import com.mojang.blaze3d.platform.GlDebugInfo;
import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleSpecSpoof;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlDebugInfo.class)
public class MixinGIDDebugInfo {

    @Inject(method = "getCpuInfo", at = @At("RETURN"), cancellable = true)
    private static void spoofCPU(CallbackInfoReturnable<String> cir) {
        applySpoof(cir, "CPU");
    }

    @Inject(method = "getRenderer", at = @At("RETURN"), cancellable = true)
    private static void spoofGPU(CallbackInfoReturnable<String> cir) {
        applySpoof(cir, "GPU");
    }

    @Inject(method = "getVersion", at = @At("RETURN"), cancellable = true)
    private static void spoofDriver(CallbackInfoReturnable<String> cir) {
        applySpoof(cir, "Driver");
    }

    @Inject(method = "getVendor", at = @At("RETURN"), cancellable = true)
    private static void spoofVendor(CallbackInfoReturnable<String> cir) {
        applySpoof(cir, "Vendor");
    }

    @Unique
    private static void applySpoof(CallbackInfoReturnable<String> cir, String type) {
        if (ModuleSpecSpoof.INSTANCE.getRunning()) {
            cir.setReturnValue(ModuleSpecSpoof.INSTANCE.getSpec(type));
        }
    }
}
