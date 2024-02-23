package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.utils.client.SignTranslationFixKt;
import net.ccbluex.liquidbounce.utils.client.VanillaTranslationRecognizer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

}
