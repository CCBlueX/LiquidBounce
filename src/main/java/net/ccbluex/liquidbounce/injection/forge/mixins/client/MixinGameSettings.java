package net.ccbluex.liquidbounce.injection.forge.mixins.client;

import net.minecraft.client.settings.GameSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameSettings.class)
public class MixinGameSettings {

    @Shadow public int guiScale;

    /**
     * Defaults gui scale to 2
     *
     * @reason Most people use 2x gui scale, so we default to that and most UI elements are designed for it
     * @param callbackInfo Unused
     */
    @Inject(method = "<init>()V", at = @At("RETURN"))
    private void injectGuiScaleDefault(final CallbackInfo callbackInfo) {
        this.guiScale = 2;
    }

}
