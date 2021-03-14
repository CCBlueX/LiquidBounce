/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.io.IOException;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiEditSign;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatAllowedCharacters;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.event.ClickEvent;

import net.minecraft.util.text.event.ClickEvent.Action;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiEditSign.class)
public class MixinGuiEditSign extends GuiScreen
{

	@Shadow
	private int editLine;
	@Final
	@Shadow
	private TileEntitySign tileSign;
	@Shadow
	private GuiButton doneBtn;
	private boolean enabled;
	private GuiButton toggleButton;

	private GuiTextField signCommand1;
	private GuiTextField signCommand2;
	private GuiTextField signCommand3;
	private GuiTextField signCommand4;

	@Inject(method = "initGui", at = @At("RETURN"))
	private void initGui(final CallbackInfo callbackInfo)
	{
		buttonList.add(toggleButton = new GuiButton(1, (width >> 1) - 100, (height >> 2) + 145, enabled ? "Disable Formatting codes" : "Enable Formatting codes"));

		signCommand1 = new GuiTextField(0, fontRenderer, (width >> 1) - 100, height - 15, 200, 10);
		signCommand2 = new GuiTextField(1, fontRenderer, (width >> 1) - 100, height - (15 << 1), 200, 10);
		signCommand3 = new GuiTextField(2, fontRenderer, (width >> 1) - 100, height - 15 * 3, 200, 10);
		signCommand4 = new GuiTextField(3, fontRenderer, (width >> 1) - 100, height - (15 << 2), 200, 10);

		signCommand1.setText("");
		signCommand2.setText("");
		signCommand3.setText("");
		signCommand4.setText("");
	}

	@Inject(method = "actionPerformed", at = @At("HEAD"))
	private void actionPerformed(final GuiButton button, final CallbackInfo callbackInfo)
	{
		switch (button.id)
		{
			case 0:
				if (!signCommand1.getText().isEmpty())
					tileSign.signText[0].setStyle(new Style().setClickEvent(new ClickEvent(Action.RUN_COMMAND, signCommand1.getText())));

				if (!signCommand2.getText().isEmpty())
					tileSign.signText[1].setStyle(new Style().setClickEvent(new ClickEvent(Action.RUN_COMMAND, signCommand2.getText())));

				if (!signCommand3.getText().isEmpty())
					tileSign.signText[2].setStyle(new Style().setClickEvent(new ClickEvent(Action.RUN_COMMAND, signCommand3.getText())));

				if (!signCommand4.getText().isEmpty())
					tileSign.signText[3].setStyle(new Style().setClickEvent(new ClickEvent(Action.RUN_COMMAND, signCommand4.getText())));
				break;
			case 1:
				enabled = !enabled;
				toggleButton.displayString = enabled ? "Disable Formatting codes" : "Enable Formatting codes";
				break;
		}
	}

	@Inject(method = "drawScreen", at = @At("RETURN"))
	private void drawFields(final CallbackInfo callbackInfo)
	{
		fontRenderer.drawString("\u00A7c\u00A7lCommands \u00A77(\u00A7f\u00A7l1.8\u00A77)", (width >> 1) - 100, height - 15 * 5, Color.WHITE.getRGB());

		signCommand1.drawTextBox();
		signCommand2.drawTextBox();
		signCommand3.drawTextBox();
		signCommand4.drawTextBox();
	}

	@Override
	protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException
	{
		signCommand1.mouseClicked(mouseX, mouseY, mouseButton);
		signCommand2.mouseClicked(mouseX, mouseY, mouseButton);
		signCommand3.mouseClicked(mouseX, mouseY, mouseButton);
		signCommand4.mouseClicked(mouseX, mouseY, mouseButton);

		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	/**
	 * @author CCBlueX
	 */
	@Override
	@Overwrite
	protected void keyTyped(final char typedChar, final int keyCode) throws IOException
	{
		signCommand1.textboxKeyTyped(typedChar, keyCode);
		signCommand2.textboxKeyTyped(typedChar, keyCode);
		signCommand3.textboxKeyTyped(typedChar, keyCode);
		signCommand4.textboxKeyTyped(typedChar, keyCode);

		if (signCommand1.isFocused() || signCommand2.isFocused() || signCommand3.isFocused() || signCommand4.isFocused())
			return;

		if (keyCode == 200)
		{
			editLine = editLine - 1 & 3;
		}

		if (keyCode == 208 || keyCode == 28 || keyCode == 156)
		{
			editLine = editLine + 1 & 3;
		}

		String s = tileSign.signText[editLine].getUnformattedText();
		if (keyCode == 14 && s.length() > 0)
		{
			s = s.substring(0, s.length() - 1);
		}

		if ((ChatAllowedCharacters.isAllowedCharacter(typedChar) || enabled && typedChar == '\u00A7') && fontRenderer.getStringWidth(s + typedChar) <= 90)
		{
			s = s + typedChar;
		}

		tileSign.signText[editLine] = new TextComponentString(s);
		if (keyCode == 1)
		{
			actionPerformed(doneBtn);
		}
	}
}
