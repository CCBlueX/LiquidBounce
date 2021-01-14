package net.ccbluex.liquidbounce.injection.mixins;

import net.ccbluex.liquidbounce.event.EventManager;
import net.ccbluex.liquidbounce.event.KeyEvent;
import net.minecraft.client.Keyboard;
import net.minecraft.client.util.InputUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Keyboard.class)
public class MixinKeyboard {

    /**
     * Hook key event
     */
    @Inject(method = "onKey", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/InputUtil;fromKeyCode(II)Lnet/minecraft/client/util/InputUtil$Key;", shift = At.Shift.AFTER))
    private void handleKey(long window, int key, int scancode, int i, int j, final CallbackInfo callback) {
        EventManager.INSTANCE.callEvent(new KeyEvent(InputUtil.fromKeyCode(key, scancode), i, j));
    }

}
