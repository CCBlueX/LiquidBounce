/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.elements;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ButtonElement extends Element
{

	protected String displayName;
	protected int color = 0xffffff;

	public int hoverTime;

	public ButtonElement(final String displayName)
	{
		createButton(displayName);
	}

	public void createButton(final String displayName)
	{
		this.displayName = displayName;
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float button)
	{
		LiquidBounce.clickGui.style.drawButtonElement(mouseX, mouseY, this);
		super.drawScreen(mouseX, mouseY, button);
	}

	@Override
	public int getHeight()
	{
		return 16;
	}

	public boolean isHovering(final int mouseX, final int mouseY)
	{
		return mouseX >= getX() && mouseX <= getX() + getWidth() && mouseY >= getY() && mouseY <= getY() + 16;
	}

	public String getDisplayName()
	{
		return displayName;
	}

	public void setColor(final int color)
	{
		this.color = color;
	}

	public int getColor()
	{
		return color;
	}
}
