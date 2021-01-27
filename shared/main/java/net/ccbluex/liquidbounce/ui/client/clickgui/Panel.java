/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI;
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.Element;
import net.ccbluex.liquidbounce.utils.MinecraftInstance;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public abstract class Panel extends MinecraftInstance
{

	private final String name;
	public int x;
	public int y;
	public int x2;
	public int y2;
	private final int width;
	private final int height;
	private int scroll;
	private int dragged;
	private boolean open;
	public boolean drag;
	private boolean scrollbar;
	private final List<Element> elements;
	private boolean visible;

	private float elementsHeight;

	private float fade;

	public Panel(final String name, final int x, final int y, final int width, final int height, final boolean open)
	{
		this.name = name;
		elements = new ArrayList<>();
		scrollbar = false;
		this.x = x;
		this.y = y;
		this.width = width;
		this.height = height;
		this.open = open;
		visible = true;

		setupItems();
	}

	public abstract void setupItems();

	public void drawScreen(final int mouseX, final int mouseY, final float button)
	{
		if (!visible)
			return;

		final int maxElements = ((ClickGUI) LiquidBounce.moduleManager.getModule(ClickGUI.class)).getMaxElementsValue().get();

		// Drag
		if (drag)
		{
			final int nx = x2 + mouseX;
			final int ny = y2 + mouseY;
			if (nx > -1)
				x = nx;

			if (ny > -1)
				y = ny;
		}

		elementsHeight = getElementsHeight() - 1;
		final boolean scrollbar = elements.size() >= maxElements;
		if (this.scrollbar != scrollbar)
			this.scrollbar = scrollbar;

		LiquidBounce.clickGui.style.drawPanel(mouseX, mouseY, this);

		int y = this.y + height - 2;
		int count = 0;

		for (final Element element : elements)
			if (++count > scroll && count < scroll + maxElements + 1 && scroll < elements.size()) {
				element.setLocation(x, y);
				element.setWidth(width);
				if (y <= this.y + fade)
					element.drawScreen(mouseX, mouseY, button);
				y += element.getHeight() + 1;
				element.setVisible(true);
			} else
				element.setVisible(false);
	}

	public void mouseClicked(final int mouseX, final int mouseY, final int mouseButton)
	{
		if (!visible)
			return;

		if (mouseButton == 1 && isHovering(mouseX, mouseY))
		{
			open = !open;
			mc.getSoundHandler().playSound("random.bow", 1.0F);
			return;
		}

		for (final Element element : elements)
			if (element.getY() <= y + fade)
				element.mouseClicked(mouseX, mouseY, mouseButton);
	}

	public void mouseReleased(final int mouseX, final int mouseY, final int state)
	{
		if (!visible)
			return;

		drag = false;

		if (!open)
			return;

		for (final Element element : elements)
			element.mouseReleased(mouseX, mouseY, state);
	}

	public boolean handleScroll(final int mouseX, final int mouseY, final int wheel)
	{
		final int maxElements = ((ClickGUI) LiquidBounce.moduleManager.getModule(ClickGUI.class)).getMaxElementsValue().get();

		if (mouseX >= x && mouseX <= x + 100 && mouseY >= y && mouseY <= y + 19 + elementsHeight)
		{
			if (wheel < 0 && scroll < elements.size() - maxElements)
			{
				++scroll;
				if (scroll < 0)
					scroll = 0;
			}
			else if (wheel > 0)
			{
				--scroll;
				if (scroll < 0)
					scroll = 0;
			}

			if (wheel < 0)
			{
				if (dragged < elements.size() - maxElements)
					++dragged;
			}
			else if (wheel > 0 && dragged >= 1)
				--dragged;

			return true;
		}
		return false;
	}

	void updateFade(final int delta)
	{
		if (open)
		{
			if (fade < elementsHeight)
				fade += 0.4F * delta;
			if (fade > elementsHeight)
				fade = (int) elementsHeight;
		}
		else
		{
			if (fade > 0)
				fade -= 0.4F * delta;
			if (fade < 0)
				fade = 0;
		}
	}

	public String getName()
	{
		return name;
	}

	public int getX()
	{
		return x;
	}

	public int getY()
	{
		return y;
	}

	public void setX(final int dragX)
	{
		x = dragX;
	}

	public void setY(final int dragY)
	{
		y = dragY;
	}

	public int getWidth()
	{
		return width;
	}

	public int getHeight()
	{
		return height;
	}

	public boolean getScrollbar()
	{
		return scrollbar;
	}

	public void setOpen(final boolean open)
	{
		this.open = open;
	}

	public boolean getOpen()
	{
		return open;
	}

	public void setVisible(final boolean visible)
	{
		this.visible = visible;
	}

	public boolean isVisible()
	{
		return visible;
	}

	public List<Element> getElements()
	{
		return elements;
	}

	public int getFade()
	{
		return (int) fade;
	}

	public int getDragged()
	{
		return dragged;
	}

	private int getElementsHeight()
	{
		int height = 0;
		int count = 0;
		for (final Element element : elements)
		{
			if (count >= ((ClickGUI) LiquidBounce.moduleManager.getModule(ClickGUI.class)).getMaxElementsValue().get())
				continue;
			height += element.getHeight() + 1;
			++count;
		}
		return height;
	}

	boolean isHovering(final int mouseX, final int mouseY)
	{
		final float textWidth = mc.getFontRendererObj().getStringWidth(StringUtils.stripControlCodes(name)) - 100.0F;
		return mouseX >= x - textWidth / 2.0F - 19.0F && mouseX <= x - textWidth / 2.0F + mc.getFontRendererObj().getStringWidth(StringUtils.stripControlCodes(name)) + 19.0F && mouseY >= y && mouseY <= y + height - (open ? 2 : 0);
	}
}
