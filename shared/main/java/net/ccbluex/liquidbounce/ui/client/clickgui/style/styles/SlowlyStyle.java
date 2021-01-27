/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IFontRenderer;
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper;
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement;
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style;
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.ui.font.Fonts.FontInfo;
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer;
import net.ccbluex.liquidbounce.utils.block.BlockUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import net.ccbluex.liquidbounce.value.*;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import org.lwjgl.input.Mouse;

@SideOnly(Side.CLIENT)
public class SlowlyStyle extends Style
{

	private boolean mouseDown;
	private boolean rightMouseDown;

	public static float drawSlider(final float value, final float min, final float max, final int x, final int y, final int width, final int mouseX, final int mouseY, final Color color)
	{
		final float displayValue = Math.max(min, Math.min(value, max));

		RenderUtils.drawRect(x, y, x + width, y + 2, Integer.MAX_VALUE);

		final float sliderValue = x + width * (displayValue - min) / (max - min);

		RenderUtils.drawRect(x, y, sliderValue, y + 2, color);
		RenderUtils.drawFilledCircle((int) sliderValue, y + 1, 3, color);

		if (mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 3 && Mouse.isButtonDown(0))
		{
			final double d = WMathHelper.clamp_double(((double) mouseX - x) / ((double) width - 3), 0, 1);

			BigDecimal bigDecimal = new BigDecimal(Double.toString(min + (max - min) * d));
			bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
			return bigDecimal.floatValue();
		}

		return value;
	}

	@Override
	public void drawPanel(final int mouseX, final int mouseY, final Panel panel)
	{
		RenderUtils.drawBorderedRect(panel.getX(), (float) panel.getY() - 3, (float) panel.getX() + panel.getWidth(), (float) panel.getY() + 17, 3, new Color(42, 57, 79).getRGB(), new Color(42, 57, 79).getRGB());
		if (panel.getFade() > 0)
		{
			RenderUtils.drawBorderedRect(panel.getX(), (float) panel.getY() + 17, (float) panel.getX() + panel.getWidth(), panel.getY() + 19 + panel.getFade(), 3, new Color(54, 71, 96).getRGB(), new Color(54, 71, 96).getRGB());
			RenderUtils.drawBorderedRect(panel.getX(), panel.getY() + 17 + panel.getFade(), (float) panel.getX() + panel.getWidth(), panel.getY() + 19 + panel.getFade() + 5, 3, new Color(42, 57, 79).getRGB(), new Color(42, 57, 79).getRGB());
		}
		GlStateManager.resetColor();
		final float textWidth = Fonts.font35.getStringWidth("\u00A7f" + StringUtils.stripControlCodes(panel.getName()));
		Fonts.font35.drawString(panel.getName(), (int) (panel.getX() - (textWidth - 100.0F) / 2.0F), panel.getY() + 7 - 3, Color.WHITE.getRGB());
	}

	@Override
	public void drawDescription(final int mouseX, final int mouseY, final String text)
	{
		final int textWidth = Fonts.font35.getStringWidth(text);

		RenderUtils.drawBorderedRect(mouseX + 9, mouseY, mouseX + textWidth + 14, mouseY + Fonts.font35.getFontHeight() + 3, 3.0F, new Color(42, 57, 79).getRGB(), new Color(42, 57, 79).getRGB());
		GlStateManager.resetColor();
		Fonts.font35.drawString(text, mouseX + 12, mouseY + Fonts.font35.getFontHeight() / 2, Color.WHITE.getRGB());
	}

	@Override
	public void drawButtonElement(final int mouseX, final int mouseY, final ButtonElement buttonElement)
	{
		Gui.drawRect(buttonElement.getX() - 1, buttonElement.getY() - 1, buttonElement.getX() + buttonElement.getWidth() + 1, buttonElement.getY() + buttonElement.getHeight() + 1, hoverColor(buttonElement.getColor() == Integer.MAX_VALUE ? new Color(54, 71, 96) : new Color(7, 152, 252), buttonElement.hoverTime).getRGB());

		GlStateManager.resetColor();

		Fonts.font35.drawString(buttonElement.getDisplayName(), buttonElement.getX() + 5, buttonElement.getY() + 5, Color.WHITE.getRGB());
	}

	/*
	 * public static boolean drawCheckbox(final boolean value, final int x, final int y, final int mouseX, final int mouseY, final Color color) { RenderUtils.drawRect(x, y, x + 20, y + 10, value ? Color.GREEN : Color.RED); RenderUtils.drawFilledCircle(x +
	 * (value ? 15 : 5),y + 5, 5, Color.WHITE);
	 * 
	 * if(mouseX >= x && mouseX <= x + 20 && mouseY >= y && mouseY <= y + 10 && Mouse.isButtonDown(0)) return !value;
	 * 
	 * return value; }
	 */

	@Override
	public void drawModuleElement(final int mouseX, final int mouseY, final ModuleElement moduleElement)
	{
		Gui.drawRect(moduleElement.getX() - 1, moduleElement.getY() - 1, moduleElement.getX() + moduleElement.getWidth() + 1, moduleElement.getY() + moduleElement.getHeight() + 1, hoverColor(new Color(54, 71, 96), moduleElement.hoverTime).getRGB());
		Gui.drawRect(moduleElement.getX() - 1, moduleElement.getY() - 1, moduleElement.getX() + moduleElement.getWidth() + 1, moduleElement.getY() + moduleElement.getHeight() + 1, hoverColor(new Color(7, 152, 252, moduleElement.slowlyFade), moduleElement.hoverTime).getRGB());
		GlStateManager.resetColor();
		Fonts.font35.drawString(moduleElement.getDisplayName(), moduleElement.getX() + 5, moduleElement.getY() + 5, Color.WHITE.getRGB());

		// Draw settings
		final List<Value<?>> moduleValues = moduleElement.getModule().getValues();

		if (!moduleValues.isEmpty())
		{
			Fonts.font35.drawString(">", moduleElement.getX() + moduleElement.getWidth() - 8, moduleElement.getY() + 5, Color.WHITE.getRGB());

			if (moduleElement.isShowSettings())
			{
				if (moduleElement.getSettingsWidth() > 0.0F && moduleElement.slowlySettingsYPos > moduleElement.getY() + 6)
					RenderUtils.drawBorderedRect(moduleElement.getX() + moduleElement.getWidth() + 4, moduleElement.getY() + 6, moduleElement.getX() + moduleElement.getWidth() + moduleElement.getSettingsWidth(), moduleElement.slowlySettingsYPos + 2, 3.0F, new Color(54, 71, 96).getRGB(), new Color(54, 71, 96).getRGB());

				moduleElement.slowlySettingsYPos = moduleElement.getY() + 6;

				final Color sliderColor = new Color(7, 152, 252);

				for (final Value value : moduleValues)
				{
					final boolean isNumber = value.get() instanceof Number;

					if (isNumber)
						AWTFontRenderer.Companion.setAssumeNonVolatile(false);

					if (value instanceof BoolValue)
					{
						final String text = value.getName();
						final float textWidth = Fonts.font35.getStringWidth(text);

						if (moduleElement.getSettingsWidth() < textWidth + 8)
							moduleElement.setSettingsWidth(textWidth + 8);

						if (mouseX >= moduleElement.getX() + moduleElement.getWidth() + 4 && mouseX <= moduleElement.getX() + moduleElement.getWidth() + moduleElement.getSettingsWidth() && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + 12 && Mouse.isButtonDown(0) && moduleElement.isntPressed())
						{
							final BoolValue boolValue = (BoolValue) value;

							boolValue.set(!boolValue.get());
							mc.getSoundHandler().playSound("gui.button.press", 1.0F);
						}

						Fonts.font35.drawString(text, moduleElement.getX() + moduleElement.getWidth() + 6, moduleElement.slowlySettingsYPos + 2, ((BoolValue) value).get() ? Color.WHITE.getRGB() : Integer.MAX_VALUE);
						moduleElement.slowlySettingsYPos += 11;
					}
					else if (value instanceof ListValue)
					{
						final ListValue listValue = (ListValue) value;

						final String text = value.getName();
						final float textWidth = Fonts.font35.getStringWidth(text);

						if (moduleElement.getSettingsWidth() < textWidth + 16)
							moduleElement.setSettingsWidth(textWidth + 16);

						Fonts.font35.drawString(text, moduleElement.getX() + moduleElement.getWidth() + 6, moduleElement.slowlySettingsYPos + 2, 0xffffff);
						Fonts.font35.drawString(listValue.openList ? "-" : "+", (int) (moduleElement.getX() + moduleElement.getWidth() + moduleElement.getSettingsWidth() - (listValue.openList ? 5 : 6)), moduleElement.slowlySettingsYPos + 2, 0xffffff);

						if (mouseX >= moduleElement.getX() + moduleElement.getWidth() + 4 && mouseX <= moduleElement.getX() + moduleElement.getWidth() + moduleElement.getSettingsWidth() && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + Fonts.font35.getFontHeight() && Mouse.isButtonDown(0) && moduleElement.isntPressed())
						{
							listValue.openList = !listValue.openList;
							mc.getSoundHandler().playSound("gui.button.press", 1.0F);
						}

						moduleElement.slowlySettingsYPos += Fonts.font35.getFontHeight() + 1;

						for (final String valueOfList : listValue.getValues())
						{
							final float textWidth2 = Fonts.font35.getStringWidth("> " + valueOfList);

							if (moduleElement.getSettingsWidth() < textWidth2 + 12)
								moduleElement.setSettingsWidth(textWidth2 + 12);

							if (listValue.openList)
							{
								if (mouseX >= moduleElement.getX() + moduleElement.getWidth() + 4 && mouseX <= moduleElement.getX() + moduleElement.getWidth() + moduleElement.getSettingsWidth() && mouseY >= moduleElement.slowlySettingsYPos + 2 && mouseY <= moduleElement.slowlySettingsYPos + 14 && Mouse.isButtonDown(0) && moduleElement.isntPressed())
								{
									listValue.set(valueOfList);
									mc.getSoundHandler().playSound("gui.button.press", 1.0F);
								}

								GlStateManager.resetColor();
								Fonts.font35.drawString("> " + valueOfList, moduleElement.getX() + moduleElement.getWidth() + 6, moduleElement.slowlySettingsYPos + 2, listValue.get() != null && listValue.get().equalsIgnoreCase(valueOfList) ? Color.WHITE.getRGB() : Integer.MAX_VALUE);
								moduleElement.slowlySettingsYPos += Fonts.font35.getFontHeight() + 1;
							}
						}

						if (!listValue.openList)
							moduleElement.slowlySettingsYPos += 1;
					}
					else if (value instanceof FloatValue)
					{
						final FloatValue floatValue = (FloatValue) value;
						final String text = value.getName() + "\u00A7f: " + round(floatValue.get());
						final float textWidth = Fonts.font35.getStringWidth(text);

						if (moduleElement.getSettingsWidth() < textWidth + 8)
							moduleElement.setSettingsWidth(textWidth + 8);

						final float valueOfSlide = drawSlider(floatValue.get(), floatValue.getMinimum(), floatValue.getMaximum(), moduleElement.getX() + moduleElement.getWidth() + 8, moduleElement.slowlySettingsYPos + 14, (int) moduleElement.getSettingsWidth() - 12, mouseX, mouseY, sliderColor);

						if (valueOfSlide != floatValue.get())
							floatValue.set(valueOfSlide);

						Fonts.font35.drawString(text, moduleElement.getX() + moduleElement.getWidth() + 6, moduleElement.slowlySettingsYPos + 3, 0xffffff);
						moduleElement.slowlySettingsYPos += 19;
					}
					else if (value instanceof IntegerValue)
					{
						final IntegerValue integerValue = (IntegerValue) value;
						final String text = value.getName() + "\u00A7f: " + (value instanceof BlockValue ? BlockUtils.getBlockName(integerValue.get()) + " (" + integerValue.get() + ")" : integerValue.get());
						final float textWidth = Fonts.font35.getStringWidth(text);

						if (moduleElement.getSettingsWidth() < textWidth + 8)
							moduleElement.setSettingsWidth(textWidth + 8);

						final float valueOfSlide = drawSlider(integerValue.get(), integerValue.getMinimum(), integerValue.getMaximum(), moduleElement.getX() + moduleElement.getWidth() + 8, moduleElement.slowlySettingsYPos + 14, (int) moduleElement.getSettingsWidth() - 12, mouseX, mouseY, sliderColor);

						if (valueOfSlide != integerValue.get())
							integerValue.set((int) valueOfSlide);

						Fonts.font35.drawString(text, moduleElement.getX() + moduleElement.getWidth() + 6, moduleElement.slowlySettingsYPos + 3, 0xffffff);
						moduleElement.slowlySettingsYPos += 19;
					}
					else if (value instanceof FontValue)
					{
						final FontValue fontValue = (FontValue) value;
						final IFontRenderer fontRenderer = fontValue.get();

						String displayString = "Font: Unknown";

						if (fontRenderer.isGameFontRenderer())
						{
							final GameFontRenderer liquidFontRenderer = fontRenderer.getGameFontRenderer();

							displayString = "Font: " + liquidFontRenderer.getDefaultFont().getFont().getName() + " - " + liquidFontRenderer.getDefaultFont().getFont().getSize();
						}
						else if (fontRenderer == Fonts.minecraftFont)
							displayString = "Font: Minecraft";
						else
						{
							final FontInfo objects = Fonts.getFontDetails(fontRenderer);

							if (objects != null)
								displayString = objects.getName() + (objects.getFontSize() == -1 ? "" : " - " + objects.getFontSize());
						}

						Fonts.font35.drawString(displayString, moduleElement.getX() + moduleElement.getWidth() + 6, moduleElement.slowlySettingsYPos + 2, Color.WHITE.getRGB());
						final int stringWidth = Fonts.font35.getStringWidth(displayString);

						if (moduleElement.getSettingsWidth() < stringWidth + 8)
							moduleElement.setSettingsWidth(stringWidth + 8);

						if ((Mouse.isButtonDown(0) && !mouseDown || Mouse.isButtonDown(1) && !rightMouseDown) && mouseX >= moduleElement.getX() + moduleElement.getWidth() + 4 && mouseX <= moduleElement.getX() + moduleElement.getWidth() + moduleElement.getSettingsWidth() && mouseY >= moduleElement.slowlySettingsYPos && mouseY <= moduleElement.slowlySettingsYPos + 12)
						{
							final List<IFontRenderer> fonts = Fonts.getFonts();

							if (Mouse.isButtonDown(0))
								for (int i = 0, j = fonts.size(); i < j; i++)
								{
									final IFontRenderer font = fonts.get(i);

									if (font.equals(fontRenderer))
									{
										i++;

										if (i >= fonts.size())
											i = 0;

										fontValue.set(fonts.get(i));
										break;
									}
								}
							else
								for (int i = fonts.size() - 1; i >= 0; i--)
								{
									final IFontRenderer font = fonts.get(i);

									if (font.equals(fontRenderer))
									{
										i--;

										if (i >= fonts.size())
											i = 0;

										if (i < 0)
											i = fonts.size() - 1;

										fontValue.set(fonts.get(i));
										break;
									}
								}
						}

						moduleElement.slowlySettingsYPos += 11;
					}
					else
					{
						final String text = value.getName() + "\u00A7f: " + value.get();
						final float textWidth = Fonts.font35.getStringWidth(text);

						if (moduleElement.getSettingsWidth() < textWidth + 8)
							moduleElement.setSettingsWidth(textWidth + 8);

						GlStateManager.resetColor();
						Fonts.font35.drawString(text, moduleElement.getX() + moduleElement.getWidth() + 6, moduleElement.slowlySettingsYPos + 4, 0xffffff);
						moduleElement.slowlySettingsYPos += 12;
					}

					if (isNumber)
						AWTFontRenderer.Companion.setAssumeNonVolatile(true);
				}

				moduleElement.updatePressed();
				mouseDown = Mouse.isButtonDown(0);
				rightMouseDown = Mouse.isButtonDown(1);
			}
		}
	}

	private static BigDecimal round(final float v)
	{
		BigDecimal bigDecimal = new BigDecimal(Float.toString(v));
		bigDecimal = bigDecimal.setScale(2, RoundingMode.HALF_UP);
		return bigDecimal;
	}

	private static Color hoverColor(final Color color, final int hover)
	{
		final int red = color.getRed() - (hover << 1);
		final int green = color.getGreen() - (hover << 1);
		final int blue = color.getBlue() - (hover << 1);

		return new Color(Math.max(red, 0), Math.max(green, 0), Math.max(blue, 0), color.getAlpha());
	}
}
