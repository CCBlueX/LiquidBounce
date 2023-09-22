package net.ccbluex.liquidbounce.injection.mixins.minecraft.registry;

import net.ccbluex.liquidbounce.features.tabs.Tabs;
import net.minecraft.registry.Registries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Registries.class)
public class MixinRegistries {

    @Inject(method = "bootstrap", at = @At(value = "INVOKE", target = "Lnet/minecraft/registry/Registries;freezeRegistries()V"))
    private static void injectInitializeTabs(CallbackInfo ci) {
        Tabs.INSTANCE.setup();
    }
}
