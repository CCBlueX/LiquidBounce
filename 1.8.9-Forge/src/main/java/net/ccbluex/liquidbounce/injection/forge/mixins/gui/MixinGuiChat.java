/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.util.Comparator;
import java.util.List;

@Mixin(GuiChat.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiChat extends MixinGuiScreen {
    @Shadow
    protected GuiTextField inputField;

    @Shadow
    private List<String> foundPlayerNames;
    @Shadow
    private boolean waitingOnAutocomplete;
    private float yPosOfInputField;
    private float fade = 0;

    @Shadow
    public abstract void onAutocompleteResponse(String[] p_onAutocompleteResponse_1_);

    @Inject(method = "initGui", at = @At("RETURN"))
    private void init(CallbackInfo callbackInfo) {
        inputField.yPosition = height + 1;
        yPosOfInputField = inputField.yPosition;
    }

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void updateLength(CallbackInfo callbackInfo) {
        if (!inputField.getText().startsWith(String.valueOf(LiquidBounce.commandManager.getPrefix()))) return;
        LiquidBounce.commandManager.autoComplete(inputField.getText());

        if (!inputField.getText().startsWith(LiquidBounce.commandManager.getPrefix() + "lc"))
            inputField.setMaxStringLength(10000);
        else
            inputField.setMaxStringLength(100);
    }

    @Inject(method = "updateScreen", at = @At("HEAD"))
    private void updateScreen(CallbackInfo callbackInfo) {
        final int delta = RenderUtils.deltaTime;

        if (fade < 14) fade += 0.4F * delta;
        if (fade > 14) fade = 14;

        if (yPosOfInputField > height - 12) yPosOfInputField -= 0.4F * delta;
        if (yPosOfInputField < height - 12) yPosOfInputField = height - 12;

        inputField.yPosition = (int) yPosOfInputField;
    }

    @Inject(method = "autocompletePlayerNames", at = @At("HEAD"))
    private void prioritizeClientFriends(final CallbackInfo callbackInfo) {
        foundPlayerNames.sort(
                Comparator.comparing(s -> !LiquidBounce.fileManager.friendsConfig.isFriend(s)));
    }

    /**
     * Adds client command auto completion and cancels sending an auto completion request packet
     * to the server if the message contains a client command.
     *
     * @author NurMarvin
     */
    @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"), cancellable = true)
    private void handleClientCommandCompletion(String full, final String ignored, CallbackInfo callbackInfo) {
        if (LiquidBounce.commandManager.autoComplete(full)) {
            waitingOnAutocomplete = true;

            String[] latestAutoComplete = LiquidBounce.commandManager.getLatestAutoComplete();

            if (full.toLowerCase().endsWith(latestAutoComplete[latestAutoComplete.length - 1].toLowerCase()))
                return;

            this.onAutocompleteResponse(latestAutoComplete);

            callbackInfo.cancel();
        }
    }

    /**
     * Add this callback, to check if the User complete a Playername or a Liquidbounce command.
     * To fix this bug: https://github.com/CCBlueX/LiquidBounce1.8-Issues/issues/3795
     *
     * @author derech1e
     */
    @Inject(method = "onAutocompleteResponse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;autocompletePlayerNames()V", shift = At.Shift.BEFORE), cancellable = true)
    private void onAutocompleteResponse(String[] autoCompleteResponse, CallbackInfo callbackInfo) {
        if (LiquidBounce.commandManager.getLatestAutoComplete().length != 0) callbackInfo.cancel();
    }

    /**
     * @author CCBlueX
     */
    @Overwrite
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        Gui.drawRect(2, this.height - (int) fade, this.width - 2, this.height, Integer.MIN_VALUE);
        this.inputField.drawTextBox();

        if (LiquidBounce.commandManager.getLatestAutoComplete().length > 0 && !inputField.getText().isEmpty() && inputField.getText().startsWith(String.valueOf(LiquidBounce.commandManager.getPrefix()))) {
            String[] latestAutoComplete = LiquidBounce.commandManager.getLatestAutoComplete();
            String[] textArray = inputField.getText().split(" ");
            String trimmedString = latestAutoComplete[0].replaceFirst("(?i)" + textArray[textArray.length - 1], "");

            mc.fontRendererObj.drawStringWithShadow(trimmedString, inputField.xPosition + mc.fontRendererObj.getStringWidth(inputField.getText()), inputField.yPosition, new Color(165, 165, 165).getRGB());
        }

        IChatComponent ichatcomponent =
                this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());

        if (ichatcomponent != null)
            this.handleComponentHover(ichatcomponent, mouseX, mouseY);
    }
}
