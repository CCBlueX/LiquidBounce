/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiKeyBindingList;
import net.minecraft.client.gui.GuiSlot;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(GuiKeyBindingList.class)
public abstract class MixinGuiKeyBindingList extends GuiSlot
{

	public MixinGuiKeyBindingList(final Minecraft mcIn, final int width, final int height, final int topIn, final int bottomIn, final int slotHeightIn)
	{
		super(mcIn, width, height, topIn, bottomIn, slotHeightIn);
	}

	/**
	 * @author CCBlueX
	 * @reason
	 */
	@Overwrite
	protected int getScrollBarX()
	{
		return width - 5;
	}
}
