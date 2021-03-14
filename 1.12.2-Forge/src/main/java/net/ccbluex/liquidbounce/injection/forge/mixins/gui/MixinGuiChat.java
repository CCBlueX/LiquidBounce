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

@Mixin(GuiChat.class)
@SideOnly(Side.CLIENT)
public abstract class MixinGuiChat extends MixinGuiScreen
{
	@Shadow
	protected GuiTextField inputField;

	private float yPosOfInputField;
	private float fade;

	@Shadow
	public abstract void setCompletions(String... p_setCompletions_1_);

	@Inject(method = "initGui", at = @At("RETURN"))
	private void init(final CallbackInfo callbackInfo)
	{
		inputField.y = height + 1;
		yPosOfInputField = inputField.y;
	}

	@Inject(method = "keyTyped", at = @At("RETURN"))
	private void updateLength(final CallbackInfo callbackInfo)
	{
		if (!inputField.getText().startsWith(String.valueOf(LiquidBounce.commandManager.getPrefix())))
			return;
		LiquidBounce.commandManager.autoComplete(inputField.getText());

		if (!inputField.getText().startsWith(LiquidBounce.commandManager.getPrefix() + "lc"))
			inputField.setMaxStringLength(10000);
		else
			inputField.setMaxStringLength(100);
	}

	@Inject(method = "updateScreen", at = @At("HEAD"))
	private void updateScreen(final CallbackInfo callbackInfo)
	{
		final int delta = RenderUtils.deltaTime;

		if (fade < 14)
			fade += 0.4F * delta;
		if (fade > 14)
			fade = 14;

		if (yPosOfInputField > height - 12)
			yPosOfInputField -= 0.4F * delta;
		if (yPosOfInputField < height - 12)
			yPosOfInputField = height - 12;

		inputField.y = (int) yPosOfInputField;
	}

	/**
	 * @author CCBlueX
	 */
	@Overwrite
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		Gui.drawRect(2, height - (int) fade, width - 2, height, Integer.MIN_VALUE);
		inputField.drawTextBox();

		if (LiquidBounce.commandManager.getLatestAutoComplete().length > 0 && !inputField.getText().isEmpty() && inputField.getText().startsWith(String.valueOf(LiquidBounce.commandManager.getPrefix())))
		{
			final String[] latestAutoComplete = LiquidBounce.commandManager.getLatestAutoComplete();
			final String[] textArray = inputField.getText().split(" ");
			final String trimmedString = latestAutoComplete[0].replaceFirst("(?i)" + textArray[textArray.length - 1], "");

			mc.fontRenderer.drawStringWithShadow(trimmedString, inputField.x + mc.fontRenderer.getStringWidth(inputField.getText()), inputField.y, new Color(165, 165, 165).getRGB());
		}

		final ITextComponent ichatcomponent = mc.ingameGUI.getChatGUI().getChatComponent(Mouse.getX(), Mouse.getY());

		if (ichatcomponent != null)
			handleComponentHover(ichatcomponent, mouseX, mouseY);
	}
}
