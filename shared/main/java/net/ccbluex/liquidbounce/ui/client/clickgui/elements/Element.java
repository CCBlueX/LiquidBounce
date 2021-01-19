/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.elements;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;

public class Element extends MinecraftInstance
{

	private int x;
	private int y;
	private int width;
	private int height;
	private boolean visible;

	public void setLocation(final int x, final int y)
	{
		this.x = x;
		this.y = y;
	}

	public void drawScreen(final int mouseX, final int mouseY, final float button)
	{
	}

	public void mouseClicked(final int mouseX, final int mouseY, final int mouseButton)
	{
	}

	public void mouseReleased(final int mouseX, final int mouseY, final int state)
	{
	}

	public int getX()
	{
		return x;
	}

	public void setX(final int x)
	{
		this.x = x;
	}

	public int getY()
	{
		return y;
	}

	public void setY(final int y)
	{
		this.y = y;
	}

	public int getWidth()
	{
		return width;
	}

	public void setWidth(final int width)
	{
		this.width = width;
	}

	public int getHeight()
	{
		return height;
	}

	public void setHeight(final int height)
	{
		this.height = height;
	}

	public boolean isVisible()
	{
		return visible;
	}

	public void setVisible(final boolean visible)
	{
		this.visible = visible;
	}
}
