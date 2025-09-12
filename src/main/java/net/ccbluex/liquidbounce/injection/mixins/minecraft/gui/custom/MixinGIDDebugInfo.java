package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.custom;

import com.mojang.blaze3d.platform.GlDebugInfo;
import net.ccbluex.liquidbounce.features.module.modules.fun.ModuleSpecSpoof;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GlDebugInfo.class)
public class MixinGIDDebugInfo {

    @Inject(method = "getCpuInfo", at = @At("RETURN"), cancellable = true)
    private static void modifyCPUGL(CallbackInfoReturnable<String> cir) {
        if (ModuleSpecSpoof.INSTANCE.getRunning()) {
            cir.setReturnValue(ModuleSpecSpoof.INSTANCE.getSpoofedCPU());
        }
    }

    @Inject(method = "getRenderer", at = @At("RETURN"), cancellable = true)
    private static void modifyGPUGL(CallbackInfoReturnable<String> cir) {
        if (ModuleSpecSpoof.INSTANCE.getRunning()) {
            cir.setReturnValue(ModuleSpecSpoof.INSTANCE.getSpoofedGPU());
        }
    }


    @Inject(method = "getVersion", at = @At("RETURN"), cancellable = true)
    private static void modifyDriverGL(CallbackInfoReturnable<String> cir) {
        if (ModuleSpecSpoof.INSTANCE.getRunning()) {
            cir.setReturnValue(ModuleSpecSpoof.INSTANCE.getSpoofedDriver());
        }
    }

    @Inject(method = "getVendor", at = @At("RETURN"), cancellable = true)
    private static void modifyVendorGL(CallbackInfoReturnable<String> cir) {
        if (ModuleSpecSpoof.INSTANCE.getRunning()) {
            cir.setReturnValue(ModuleSpecSpoof.INSTANCE.getSpoofedVendor());
        }
    }
}
