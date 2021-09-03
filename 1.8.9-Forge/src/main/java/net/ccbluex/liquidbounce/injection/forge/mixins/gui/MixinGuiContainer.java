package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.Sets;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor;
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura;
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner;
import net.ccbluex.liquidbounce.features.module.modules.world.ChestStealer;
import net.ccbluex.liquidbounce.injection.implementations.IMixinGuiContainer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer extends MixinGuiScreen implements IMixinGuiContainer
{
	@Shadow
	@Final
	protected static ResourceLocation inventoryBackground;
	@Shadow
	public Container inventorySlots;
	@Shadow
	protected int guiLeft;
	@Shadow
	protected int guiTop;
	@Shadow
	protected Slot theSlot;
	@Shadow
	protected boolean isRightMouseClick;
	@Shadow
	protected ItemStack draggedStack;
	@Shadow
	protected int touchUpX;
	@Shadow
	protected int touchUpY;
	@Shadow
	protected Slot returningStackDestSlot;
	@Shadow
	protected long returningStackTime;
	@Shadow
	protected ItemStack returningStack;
	@Shadow
	protected final Set<Slot> dragSplittingSlots = Sets.newHashSet();
	@Shadow
	protected boolean dragSplitting;
	@Shadow
	protected int dragSplittingRemnant;
	@Shadow
	protected int xSize;

	private final Map<Integer, Entry<Long, Entry<Long, Integer>>> highlightingMap = new HashMap<>();

	@SuppressWarnings("NoopMethodInAbstractClass")
	@Shadow
	private void drawItemStack(final ItemStack stack, final int x, final int y, final String altText)
	{
	}

	@Shadow
	protected abstract void drawGuiContainerForegroundLayer(int mouseX, int mouseY);

	@Shadow
	protected abstract void drawGuiContainerBackgroundLayer(float var1, int var2, int var3);

	@SuppressWarnings("NoopMethodInAbstractClass")
	@Shadow
	private void drawSlot(final Slot slotIn)
	{
	}

	@Shadow
	private boolean isMouseOverSlot(final Slot slotIn, final int mouseX, final int mouseY)
	{
		return false;
	}

	@Inject(method = "initGui", at = @At("RETURN"))
	private void injectButtons(final CallbackInfo ci)
	{
		// noinspection ConstantConditions,InstanceofThis
		if ((Object) this instanceof GuiInventory)
			return;

		final int screenCenterX = width >> 1;
		final int screenQuarterY = height >> 2;

		buttonList.add(new GuiButton(996, screenCenterX - 100, screenQuarterY - 52, "Disable KillAura"));

		final int disableModuleButtonsY = screenQuarterY - 30;
		buttonList.add(new GuiButton(997, screenCenterX - 155, disableModuleButtonsY, 100, 20, "Disable ChestStealer"));
		buttonList.add(new GuiButton(998, screenCenterX - 50, disableModuleButtonsY, 100, 20, "Disable InventoryCleaner"));
		buttonList.add(new GuiButton(999, screenCenterX + 55, disableModuleButtonsY, 100, 20, "Disable AutoArmor"));
	}

	@Override
	protected void injectedActionPerformed(final GuiButton button)
	{
		// noinspection ConstantConditions,InstanceofThis
		if ((Object) this instanceof GuiInventory)
			return;

		// Disable KillAura button
		if (button.id == 996)
		{
			final KillAura killAura = (KillAura) LiquidBounce.moduleManager.get(KillAura.class);
			if (killAura.getState())
				killAura.setState(false);
		}

		// Disable ChestStealer button
		if (button.id == 997)
		{
			final ChestStealer chestStealer = (ChestStealer) LiquidBounce.moduleManager.get(ChestStealer.class);
			if (chestStealer.getState())
				chestStealer.setState(false);
		}

		// Disable InventoryCleaner button
		if (button.id == 998)
		{
			final InventoryCleaner inventoryCleaner = (InventoryCleaner) LiquidBounce.moduleManager.get(InventoryCleaner.class);
			if (inventoryCleaner.getState())
				inventoryCleaner.setState(false);
		}

		// Disable AutoArmor button
		if (button.id == 999)
		{
			final AutoArmor autoArmor = (AutoArmor) LiquidBounce.moduleManager.get(AutoArmor.class);
			if (autoArmor.getState())
				autoArmor.setState(false);
		}
	}

	@Override
	public final void highlight(final int slotNumber, final long duration, final int color)
	{
		highlightingMap.put(slotNumber, new SimpleEntry<>(System.currentTimeMillis(), new SimpleEntry<>(duration, color == -1 ? 0x80FFFFFF : color)));
	}

	/**
	 * @author eric0210
	 * @reason ChestStealer slot highlighting
	 */
	@Overwrite
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		drawDefaultBackground();
		drawGuiContainerBackgroundLayer(partialTicks, mouseX, mouseY);
		GlStateManager.disableRescaleNormal();
		RenderHelper.disableStandardItemLighting();
		GlStateManager.disableLighting();
		GlStateManager.disableDepth();
		super.drawScreen(mouseX, mouseY, partialTicks);
		RenderHelper.enableGUIStandardItemLighting();
		GlStateManager.pushMatrix();
		GlStateManager.translate(guiLeft, guiTop, 0.0F);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		GlStateManager.enableRescaleNormal();
		theSlot = null;
		OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240, 240);
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

		int slotYPos;

		for (int slotNumber = 0, slotLength = inventorySlots.inventorySlots.size(); slotNumber < slotLength; ++slotNumber)
		{
			final Slot slot = inventorySlots.inventorySlots.get(slotNumber);
			drawSlot(slot);

			final boolean highlightContainsTheSlot = highlightingMap.containsKey(slotNumber);
			final boolean highlightTheSlot = highlightContainsTheSlot && highlightingMap.get(slotNumber).getKey() + highlightingMap.get(slotNumber).getValue().getKey() >= System.currentTimeMillis();

			if ((isMouseOverSlot(slot, mouseX, mouseY) || highlightTheSlot) && slot.canBeHovered())
			{
				final int color = highlightTheSlot ? highlightingMap.get(slotNumber).getValue().getValue() : 0x80FFFFFF /* DWORD -2130706433 */;

				theSlot = slot;
				GlStateManager.disableLighting();
				GlStateManager.disableDepth();
				final int slotXPos = slot.xDisplayPosition;
				slotYPos = slot.yDisplayPosition;
				GlStateManager.colorMask(true, true, true, false);
				drawGradientRect(slotXPos, slotYPos, slotXPos + 16, slotYPos + 16, color, color);
				GlStateManager.colorMask(true, true, true, true);
				GlStateManager.enableLighting();
				GlStateManager.enableDepth();
			}

			if (highlightContainsTheSlot && highlightingMap.get(slotNumber).getKey() + highlightingMap.get(slotNumber).getValue().getKey() < System.currentTimeMillis())
				highlightingMap.remove(slotNumber); // Collect garbage
		}

		RenderHelper.disableStandardItemLighting();
		drawGuiContainerForegroundLayer(mouseX, mouseY);
		RenderHelper.enableGUIStandardItemLighting();

		final InventoryPlayer inventoryplayer = mc.thePlayer.inventory;
		ItemStack itemstack = Optional.ofNullable(draggedStack).orElseGet(inventoryplayer::getItemStack);

		if (itemstack != null)
		{
			slotYPos = draggedStack == null ? 8 : 16;
			String s = null;
			if (draggedStack != null && isRightMouseClick)
			{
				itemstack = itemstack.copy();
				itemstack.stackSize = MathHelper.ceiling_float_int(itemstack.stackSize * 0.5f);
			}
			else if (dragSplitting && dragSplittingSlots.size() > 1)
			{
				itemstack = itemstack.copy();
				itemstack.stackSize = dragSplittingRemnant;
				if (itemstack.stackSize == 0)
					s = EnumChatFormatting.YELLOW + "0";
			}

			drawItemStack(itemstack, mouseX - guiLeft - 8, mouseY - guiTop - slotYPos, s);
		}

		if (returningStack != null)
		{
			float f = (Minecraft.getSystemTime() - returningStackTime) * 0.01f;
			if (f >= 1.0F)
			{
				f = 1.0F;
				returningStack = null;
			}

			slotYPos = returningStackDestSlot.xDisplayPosition - touchUpX;
			drawItemStack(returningStack, touchUpX + (int) (slotYPos * f), touchUpY + (int) ((returningStackDestSlot.yDisplayPosition - touchUpY) * f), null);
		}

		GlStateManager.popMatrix();
		if (inventoryplayer.getItemStack() == null && theSlot != null && theSlot.getHasStack())
			renderToolTip(theSlot.getStack(), mouseX, mouseY);

		GlStateManager.enableLighting();
		GlStateManager.enableDepth();
		RenderHelper.enableStandardItemLighting();
	}
}
