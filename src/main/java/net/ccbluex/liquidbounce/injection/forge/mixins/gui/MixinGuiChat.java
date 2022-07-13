/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.awt.*;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiChat;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiChat.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiChat extends MixinGuiScreen
{
    @Shadow
    protected GuiTextField inputField;

    @Shadow
    private List<String> foundPlayerNames;
    @Shadow
    private boolean waitingOnAutocomplete;
    private float yPosOfInputField;
    private float fade;

    @Shadow
    public abstract void onAutocompleteResponse(String[] p_onAutocompleteResponse_1_);

    @Inject(method = "initGui", at = @At("RETURN"))
    private void init(final CallbackInfo callbackInfo)
    {
        inputField.yPosition = height + 1;
        yPosOfInputField = inputField.yPosition;
    }

    @Inject(method = "keyTyped", at = @At("RETURN"))
    private void updateLength(final CallbackInfo callbackInfo)
    {
        if (!inputField.getText().startsWith(String.valueOf(LiquidBounce.commandManager.getPrefix())))
            return;
        LiquidBounce.commandManager.autoComplete(inputField.getText());

        if (inputField.getText().startsWith(LiquidBounce.commandManager.getPrefix() + "lc"))
            inputField.setMaxStringLength(100);
        else
            inputField.setMaxStringLength(10000);
    }

    @Inject(method = "updateScreen", at = @At("HEAD"))
    private void updateScreen(final CallbackInfo callbackInfo)
    {
        final int delta = RenderUtils.getFrameTime();

        if (fade < 14)
            fade += 0.4F * delta;
        if (fade > 14)
            fade = 14;

        if (yPosOfInputField > height - 12)
            yPosOfInputField -= 0.4F * delta;
        if (yPosOfInputField < height - 12)
            yPosOfInputField = height - 12;

        inputField.yPosition = (int) yPosOfInputField;
    }

    @Inject(method = "autocompletePlayerNames", at = @At("HEAD"))
    private void prioritizeClientFriends(final CallbackInfo callbackInfo)
    {
        foundPlayerNames.sort(Comparator.comparing(s -> !LiquidBounce.fileManager.friendsConfig.isFriend(s)));
    }

    /**
     * Adds client command auto completion and cancels sending an auto completion request packet to the server if the message contains a client command.
     *
     * @author NurMarvin
     */
    @Inject(method = "sendAutocompleteRequest", at = @At("HEAD"), cancellable = true)
    private void handleClientCommandCompletion(final String full, final String ignored, final CallbackInfo callbackInfo)
    {
        if (LiquidBounce.commandManager.autoComplete(full))
        {
            waitingOnAutocomplete = true;

            final String[] latestAutoComplete = LiquidBounce.commandManager.getLatestAutoComplete();

            if (full.toLowerCase(Locale.ENGLISH).endsWith(latestAutoComplete[latestAutoComplete.length - 1].toLowerCase(Locale.ENGLISH)))
                return;

            onAutocompleteResponse(latestAutoComplete);

            callbackInfo.cancel();
        }
    }

    /**
     * Add this callback, to check if the User complete a Playername or a Liquidbounce command.
     * To fix this bug, <a href="https://github.com/CCBlueX/LiquidBounce1.8-Issues/issues/3795">see here</a>
     *
     * @author derech1e
     */
    @Inject(method = "onAutocompleteResponse", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;autocompletePlayerNames()V", shift = Shift.BEFORE), cancellable = true)
    private void onAutocompleteResponse(final String[] autoCompleteResponse, final CallbackInfo callbackInfo)
    {
        if (LiquidBounce.commandManager.getLatestAutoComplete().length != 0)
            callbackInfo.cancel();
    }

    /**
     * @author CCBlueX
     * @reason Chat fade
     */
    @Redirect(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiChat;drawRect(IIIII)V"), require = 1)
    private void handleChatFade(final int left, final int top, final int right, final int bottom, final int color)
    {
        Gui.drawRect(2, height - (int) fade, width - 2, height, Integer.MIN_VALUE);
    }

    /**
     * @author CCBlueX
     * @reason LiquidBounce Command AutoComplete
     */
    @Inject(method = "drawScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiTextField;drawTextBox()V", shift = Shift.AFTER))
    public void handleAutoComplete(final CallbackInfo ci)
    {
        if (LiquidBounce.commandManager.getLatestAutoComplete().length > 0 && !inputField.getText().isEmpty() && inputField.getText().startsWith(String.valueOf(LiquidBounce.commandManager.getPrefix())))
        {
            final String[] latestAutoComplete = LiquidBounce.commandManager.getLatestAutoComplete();
            final String[] textArray = inputField.getText().split(" ");
            final String trimmedString = latestAutoComplete[0].replaceFirst("(?i)" + textArray[textArray.length - 1], "");

            mc.fontRendererObj.drawStringWithShadow(trimmedString, inputField.xPosition + mc.fontRendererObj.getStringWidth(inputField.getText()), inputField.yPosition, new Color(165, 165, 165).getRGB());
        }
    }
}
