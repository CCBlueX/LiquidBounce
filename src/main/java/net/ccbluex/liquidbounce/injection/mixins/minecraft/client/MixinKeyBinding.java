package net.ccbluex.liquidbounce.injection.mixins.minecraft.client;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.events.KeyBindingEvent;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(KeyBinding.class)
public class MixinKeyBinding {

    @Shadow
    @Final
    private static Map<InputUtil.Key, KeyBinding> KEY_TO_BINDINGS;

    @Inject(method = "onKeyPressed", at = @At(value = "FIELD", target = "Lnet/minecraft/client/option/KeyBinding;timesPressed:I", shift = At.Shift.AFTER))
    private static void hookKeyBindingEvent(InputUtil.Key key, CallbackInfo ci) {
        EventManager.INSTANCE.callEvent(new KeyBindingEvent(KEY_TO_BINDINGS.get(key)));
    }
}
