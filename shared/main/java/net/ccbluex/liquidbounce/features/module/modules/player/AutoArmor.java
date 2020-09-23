/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player;

import net.ccbluex.liquidbounce.api.enums.WEnumHand;
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack;
import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.UpdateEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.utils.CrossVersionUtilsKt;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.item.ArmorComparator;
import net.ccbluex.liquidbounce.utils.item.ArmorPiece;
import net.ccbluex.liquidbounce.utils.item.ItemUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.ccbluex.liquidbounce.utils.CrossVersionUtilsKt.createOpenInventoryPacket;

@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.PLAYER)
public class AutoArmor extends Module {

    public static final ArmorComparator ARMOR_COMPARATOR = new ArmorComparator();
    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 100, 0, 400) {

        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxDelayValue.get();

            if (maxDelay < newValue) set(maxDelay);
        }
    };
    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 200, 0, 400) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int minDelay = minDelayValue.get();

            if (minDelay > newValue) set(minDelay);
        }
    };
    private final IntegerValue startDelayValue = new IntegerValue("StartDelay", 0, 0, 5000);
    private final IntegerValue itemDelayValue = new IntegerValue("ItemDelay", 0, 0, 5000);

    private final BoolValue invOpenValue = new BoolValue("InvOpen", false);
    private final BoolValue simulateInventory = new BoolValue("SimulateInventory", true);
    private final BoolValue noMoveValue = new BoolValue("NoMove", false);
    private final BoolValue hotbarValue = new BoolValue("Hotbar", true);
    private final BoolValue dropOldValue = new BoolValue("DropOnFullInventory", true);

    private final MSTimer START_TIMER = new MSTimer();
    private long delay;

    @EventTarget
    public void onUpdate(UpdateEvent event) {
        if (mc.getThePlayer() == null || mc.getTheWorld() == null || (mc.getThePlayer().getOpenContainer() != null && mc.getThePlayer().getOpenContainer().getWindowId() != 0))
            return;

        //Reset start timer
        if ((noMoveValue.get() && MovementUtils.isMoving()) || (!classProvider.isGuiInventory(mc.getCurrentScreen()) && invOpenValue.get()))
            START_TIMER.reset();

        //Check if time has passed
        if (!START_TIMER.hasTimePassed(startDelayValue.get()) || !InventoryUtils.CLICK_TIMER.hasTimePassed(delay))
            return;


        // Find best armor
        final Map<Integer, List<ArmorPiece>> armorPieces = IntStream.range(0, 36)
                .filter(i -> {
                    final IItemStack itemStack = mc.getThePlayer().getInventory().getStackInSlot(i);

                    return itemStack != null && classProvider.isItemArmor(itemStack.getItem())
                            && (i < 9 || System.currentTimeMillis() - itemStack.getItemDelay() >= itemDelayValue.get());
                })
                .mapToObj(i -> new ArmorPiece(mc.getThePlayer().getInventory().getStackInSlot(i), i))
                .collect(Collectors.groupingBy(ArmorPiece::getArmorType));

        final ArmorPiece[] bestArmor = new ArmorPiece[4];

        for (final Map.Entry<Integer, List<ArmorPiece>> armorEntry : armorPieces.entrySet()) {
            bestArmor[armorEntry.getKey()] = armorEntry.getValue().stream()
                    .max(ARMOR_COMPARATOR).orElse(null);
        }

        // Swap armor
        for (int i = 0; i < 4; i++) {
            final ArmorPiece armorPiece = bestArmor[i];

            if (armorPiece == null)
                continue;

            int armorSlot = 3 - i;

            final ArmorPiece oldArmor = new ArmorPiece(mc.getThePlayer().getInventory().armorItemInSlot(armorSlot), -1);

            if (ItemUtils.isStackEmpty(oldArmor.getItemStack()) || !classProvider.isItemArmor(oldArmor.getItemStack().getItem()) ||
                    ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0) {
                if (!ItemUtils.isStackEmpty(oldArmor.getItemStack()) && move(8 - armorSlot, true))
                    return;


                if (ItemUtils.isStackEmpty(mc.getThePlayer().getInventory().armorItemInSlot(armorSlot)) && move(armorPiece.getSlot(), false))
                    return;

            }
        }
    }


    /**
     * Shift+Left clicks the specified item
     *
     * @param item        Slot of the item to click
     * @param isArmorSlot
     * @return True if it is unable to move the item
     */
    private boolean move(int item, boolean isArmorSlot) {
        if (!isArmorSlot && item < 9 && hotbarValue.get()) {
            mc.getNetHandler().addToSendQueue(classProvider.createCPacketHeldItemChange(item));
            mc.getNetHandler().addToSendQueue(CrossVersionUtilsKt.createUseItemPacket(mc.getThePlayer().getInventoryContainer().getSlot(item).getStack(), WEnumHand.MAIN_HAND));
            mc.getNetHandler().addToSendQueue(classProvider.createCPacketHeldItemChange(mc.getThePlayer().getInventory().getCurrentItem()));

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            return true;
        } else if (item != -1) {
            final boolean openInventory = simulateInventory.get() && !classProvider.isGuiInventory(mc.getCurrentScreen());

            if (openInventory)
                mc.getNetHandler().addToSendQueue(createOpenInventoryPacket());

            mc.getPlayerController().windowClick(mc.getThePlayer().getInventoryContainer().getWindowId(), (isArmorSlot ? item : (item < 9 ? item + 36 : item)), 0, isArmorSlot && dropOldValue.get() ? 4 : 1, mc.getThePlayer());

            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (openInventory)
                mc.getNetHandler().addToSendQueue(classProvider.createCPacketCloseWindow());

            return true;
        }
        return false;
    }
}
