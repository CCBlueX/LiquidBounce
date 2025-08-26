package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.utils.client.VanillaTranslationRecognizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(GameOptions.class)
public class MixinGameOptions {

    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/Object;<init>()V", shift = At.Shift.AFTER), require = 1)
    private void injectKeyBindRegisterStart(MinecraftClient client, File optionsFile, CallbackInfo ci) {
        VanillaTranslationRecognizer.INSTANCE.setBuildingVanillaKeybinds(true);
    }
    @Inject(method = "<init>", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/GameOptions;client:Lnet/minecraft/client/MinecraftClient;"), require = 1)
    private void injectKeyBindRegisterEnd(MinecraftClient client, File optionsFile, CallbackInfo ci) {
        VanillaTranslationRecognizer.INSTANCE.setBuildingVanillaKeybinds(false);
    }

    /**
     * Hook GUI scale adjustment
     * <p>
     * This is used to set the default GUI scale to 2X on AUTO because the default is TOO HUGE.
     * On WQHD and HD displays, the default GUI scale is way too big. 4K might be fine, but
     * the majority of players are not using 4K displays.
     */
    @Inject(method = "getGuiScale", at = @At("RETURN"))
    private void injectGuiScale(CallbackInfoReturnable<SimpleOption<Integer>> cir) {
        if (cir.getReturnValue().getValue() == 0) {
            cir.getReturnValue().setValue(2);
        }
    }

}
