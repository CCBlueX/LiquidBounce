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
import net.minecraft.util.text.ITextComponent;
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

@Mixin(GuiChat.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiChat extends MixinGuiScreen {
    @Shadow
    protected GuiTextField inputField;

    private float yPosOfInputField;
    private float fade = 0;

    @Shadow
    public abstract void setCompletions(String... p_setCompletions_1_);

    @Inject(method = "initGui", at = @At("RETURN"))
    private void init(CallbackInfo callbackInfo) {
        inputField.y = height + 1;
        yPosOfInputField = inputField.y;
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

        inputField.y = (int) yPosOfInputField;
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

            mc.fontRenderer.drawStringWithShadow(trimmedString, inputField.x + mc.fontRenderer.getStringWidth(inputField.getText()), inputField.y, new Color(165, 165, 165).getRGB());
        }

        ITextComponent ichatcomponent =
                this.mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());

        if (ichatcomponent != null)
            this.handleComponentHover(ichatcomponent, mouseX, mouseY);
    }
}
