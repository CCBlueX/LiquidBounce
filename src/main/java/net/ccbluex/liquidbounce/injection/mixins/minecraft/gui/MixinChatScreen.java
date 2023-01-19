package net.ccbluex.liquidbounce.injection.mixins.minecraft.gui;

import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class MixinChatScreen extends MixinScreen {

    /**
     * We want to close the screen before sending the message to make sure it doesn't affect commands.
     */
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/ChatScreen;sendMessage(Ljava/lang/String;Z)Z", shift = At.Shift.BEFORE))
    private void fixOrder(CallbackInfoReturnable<Boolean> callbackInfo) {
        this.client.setScreen(null);
    }

}
