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
		buttonList.add(toggleButton = new GuiButton(1, (this.width >> 1) - 100, (this.height >> 2) + 145, enabled ? "Disable Formatting codes" : "Enable Formatting codes"));

		this.signCommand1 = new GuiTextField(0, this.fontRenderer, (this.width >> 1) - 100, height - 15, 200, 10);
		this.signCommand2 = new GuiTextField(1, this.fontRenderer, (this.width >> 1) - 100, height - (15 << 1), 200, 10);
		this.signCommand3 = new GuiTextField(2, this.fontRenderer, (this.width >> 1) - 100, height - 15 * 3, 200, 10);
		this.signCommand4 = new GuiTextField(3, this.fontRenderer, (this.width >> 1) - 100, height - (15 << 2), 200, 10);

		this.signCommand1.setText("");
		this.signCommand2.setText("");
		this.signCommand3.setText("");
		this.signCommand4.setText("");
	}

	@Inject(method = "actionPerformed", at = @At("HEAD"))
	private void actionPerformed(GuiButton button, CallbackInfo callbackInfo)
	{
		switch (button.id)
		{
			case 0:
				if (!signCommand1.getText().isEmpty())
					tileSign.signText[0].setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, signCommand1.getText())));

				if (!signCommand2.getText().isEmpty())
					tileSign.signText[1].setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, signCommand2.getText())));

				if (!signCommand3.getText().isEmpty())
					tileSign.signText[2].setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, signCommand3.getText())));

				if (!signCommand4.getText().isEmpty())
					tileSign.signText[3].setStyle(new Style().setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, signCommand4.getText())));
				break;
			case 1:
				enabled = !enabled;
				toggleButton.displayString = enabled ? "Disable Formatting codes" : "Enable Formatting codes";
				break;
		}
	}

	@Inject(method = "drawScreen", at = @At("RETURN"))
	private void drawFields(CallbackInfo callbackInfo)
	{
		this.fontRenderer.drawString("\u00A7c\u00A7lCommands \u00A77(\u00A7f\u00A7l1.8\u00A77)", (width >> 1) - 100, height - 15 * 5, Color.WHITE.getRGB());

		signCommand1.drawTextBox();
		signCommand2.drawTextBox();
		signCommand3.drawTextBox();
		signCommand4.drawTextBox();
	}

	@Override
	protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException
	{
		this.signCommand1.mouseClicked(mouseX, mouseY, mouseButton);
		this.signCommand2.mouseClicked(mouseX, mouseY, mouseButton);
		this.signCommand3.mouseClicked(mouseX, mouseY, mouseButton);
		this.signCommand4.mouseClicked(mouseX, mouseY, mouseButton);

		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	/**
	 * @author CCBlueX
	 */
	@Overwrite
	protected void keyTyped(char typedChar, int keyCode) throws IOException
	{
		this.signCommand1.textboxKeyTyped(typedChar, keyCode);
		this.signCommand2.textboxKeyTyped(typedChar, keyCode);
		this.signCommand3.textboxKeyTyped(typedChar, keyCode);
		this.signCommand4.textboxKeyTyped(typedChar, keyCode);

		if (signCommand1.isFocused() || signCommand2.isFocused() || signCommand3.isFocused() || signCommand4.isFocused())
			return;

		if (keyCode == 200)
		{
			this.editLine = this.editLine - 1 & 3;
		}

		if (keyCode == 208 || keyCode == 28 || keyCode == 156)
		{
			this.editLine = this.editLine + 1 & 3;
		}

		String s = this.tileSign.signText[this.editLine].getUnformattedText();
		if (keyCode == 14 && s.length() > 0)
		{
			s = s.substring(0, s.length() - 1);
		}

		if ((ChatAllowedCharacters.isAllowedCharacter(typedChar) || (enabled && typedChar == '\u00A7')) && this.fontRenderer.getStringWidth(s + typedChar) <= 90)
		{
			s = s + typedChar;
		}

		this.tileSign.signText[this.editLine] = new TextComponentString(s);
		if (keyCode == 1)
		{
			this.actionPerformed(this.doneBtn);
		}
	}
}
