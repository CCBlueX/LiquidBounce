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

    @Shadow protected TextFieldWidget chatField;

    /**
     * We want to close the screen before sending the message to make sure it doesn't affect commands.
     */
    @Inject(method = "keyPressed", at = @At(value = "INVOKE", target = "Ljava/lang/String;trim()Ljava/lang/String;", shift = At.Shift.BEFORE), cancellable = true)
    private void fixOrder(CallbackInfoReturnable<Boolean> callbackInfo) {
        this.client.setScreen(null);

        String string = this.chatField.getText().trim();
        if (!string.isEmpty()) {
            this.sendMessage(string);
        }

        callbackInfo.setReturnValue(true);
    }

}
