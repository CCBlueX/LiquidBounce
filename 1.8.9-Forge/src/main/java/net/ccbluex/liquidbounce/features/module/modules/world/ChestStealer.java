package net.ccbluex.liquidbounce.features.module.modules.world;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "ChestStealer", description = "Automatically steals all items from a chest.", category = ModuleCategory.WORLD)
public class ChestStealer extends Module {

	private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 200, 0, 400) {
		@Override
		protected void onChanged(final Integer oldValue, final Integer newValue) {
			final int i = minDelayValue.get();

			if(i > newValue)
				set(i);

			delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
		}
	};

	private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 150, 0, 400) {
		@Override
		protected void onChanged(final Integer oldValue, final Integer newValue) {
			final int i = maxDelayValue.get();

			if(i < newValue)
				set(i);

			delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
		}
	};

	private final BoolValue takeRandomizedValue = new BoolValue("TakeRandomized", false);
	private final BoolValue onlyItemsValue = new BoolValue("OnlyItems", false);
	private final BoolValue noCompassValue = new BoolValue("NoCompass", false);
	private final BoolValue autoCloseValue = new BoolValue("AutoClose", true);
	private final IntegerValue autoCloseMaxDelayValue = new IntegerValue("AutoCloseMaxDelay", 0, 0, 400) {
		@Override
		protected void onChanged(final Integer oldValue, final Integer newValue) {
			final int i = autoCloseMinDelayValue.get();

			if(i > newValue)
				set(i);

			autoCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get());
		}
	};

	private final IntegerValue autoCloseMinDelayValue = new IntegerValue("AutoCloseMinDelay", 0, 0, 400) {
		@Override
		protected void onChanged(final Integer oldValue, final Integer newValue) {
			final int i = autoCloseMaxDelayValue.get();

			if(i < newValue)
				set(i);

			autoCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get());
		}
	};
    private final BoolValue closeOnFullValue = new BoolValue("CloseOnFull", true);
	private final BoolValue chestTitleValue = new BoolValue("ChestTitle", false);

	private final MSTimer msTimer = new MSTimer();
	private long delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

	private final MSTimer autoCloseTimer = new MSTimer();
	private long autoCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get());

	@EventTarget
	public void onRender3D(final Render3DEvent event) {
        if (mc.currentScreen instanceof GuiChest && msTimer.hasTimePassed(delay) && (!noCompassValue.get() || mc.thePlayer.inventory.getCurrentItem() == null || !mc.thePlayer.inventory.getCurrentItem().getItem().getUnlocalizedName().equals("item.compass"))) {
            final GuiChest guiChest = (GuiChest) mc.currentScreen;

            if (chestTitleValue.get() && (guiChest.lowerChestInventory == null || !guiChest.lowerChestInventory.getName().contains(new ItemStack(Item.itemRegistry.getObject(new ResourceLocation("minecraft:chest"))).getDisplayName())))
                return;

			final InventoryCleaner inventoryCleaner = (InventoryCleaner) LiquidBounce.moduleManager.getModule(InventoryCleaner.class);

            if (inventoryCleaner.getState()) {
                inventoryCleaner.updateItems();
            }

            final boolean takeRandomized = takeRandomizedValue.get();

            if (!isEmpty(guiChest) && !(closeOnFullValue.get() && isInventoryFull())) {
                autoCloseTimer.reset();

                if (takeRandomized) {
                    final List<Slot> items = new ArrayList<>();

                    for (int i = 0; i < guiChest.inventoryRows * 9; i++) {
						final Slot slot = guiChest.inventorySlots.inventorySlots.get(i);

                        if (slot.getStack() != null && (!onlyItemsValue.get() || !(slot.getStack().getItem() instanceof ItemBlock)) && (!inventoryCleaner.getState() || inventoryCleaner.isUseful(slot.getStack(), -1)))
                            items.add(slot);
					}

					final int randomSlot = new Random().nextInt(items.size());
					final Slot slot = items.get(randomSlot);

					guiChest.handleMouseClick(slot, slot.slotNumber, 0, 1);

					msTimer.reset();
					delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
				}else{
					for(int i = 0; i < guiChest.inventoryRows * 9; i++) {
						final Slot slot = guiChest.inventorySlots.inventorySlots.get(i);

                        if (msTimer.hasTimePassed(delay) && slot.getStack() != null && (!onlyItemsValue.get() || !(slot.getStack().getItem() instanceof ItemBlock)) && (!inventoryCleaner.getState() || inventoryCleaner.isUseful(slot.getStack(), -1))) {
                            guiChest.handleMouseClick(slot, slot.slotNumber, 0, 1);

                            msTimer.reset();
                            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
                        }
					}
				}
			}else if(autoCloseValue.get() && autoCloseTimer.hasTimePassed(autoCloseDelay)) {
				mc.thePlayer.closeScreen();
				autoCloseDelay = TimeUtils.randomDelay(autoCloseMinDelayValue.get(), autoCloseMaxDelayValue.get());
			}
		}else
			autoCloseTimer.reset();
	}

    private boolean isEmpty(final GuiChest guiChest) {
		final InventoryCleaner inventoryCleaner = (InventoryCleaner) LiquidBounce.moduleManager.getModule(InventoryCleaner.class);

		for(int i = 0; i < guiChest.inventoryRows * 9; i++) {
            final Slot slot = guiChest.inventorySlots.inventorySlots.get(i);

            if (slot.getStack() != null && (!onlyItemsValue.get() || !(slot.getStack().getItem() instanceof ItemBlock)) && (!inventoryCleaner.getState() || inventoryCleaner.isUseful(slot.getStack(), -1)))
                return false;
        }

        return true;
    }

    private boolean isInventoryFull() {
        for(int i = 0; i < mc.thePlayer.inventory.mainInventory.length; i++)
            if(mc.thePlayer.inventory.mainInventory[i] == null)
                return false;

        return true;
    }
}
