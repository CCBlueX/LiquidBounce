package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.injection.implementations.IItemStack;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.item.ArmorComparator;
import net.ccbluex.liquidbounce.utils.item.ArmorPiece;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.network.play.client.C09PacketHeldItemChange;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @author CCBlueX
 * @game Minecraft
 */
@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.COMBAT)
public class AutoArmor extends Module {
    public static final ArmorComparator ARMOR_COMPARATOR = new ArmorComparator();
    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 200, 0, 400) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int minDelay = minDelayValue.get();

            if (minDelay > newValue) set(minDelay);
        }
    };
    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 100, 0, 400) {

        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxDelayValue.get();

            if (maxDelay < newValue) set(maxDelay);
        }
    };
    private final BoolValue invOpenValue = new BoolValue("InvOpen", false);
    private final BoolValue simulateInventory = new BoolValue("SimulateInventory", true);
    private final BoolValue noMoveValue = new BoolValue("NoMove", false);
    private final IntegerValue itemDelayValue = new IntegerValue("ItemDelay", 0, 0, 5000);
    private final BoolValue hotbarValue = new BoolValue("Hotbar", true);

    private final MSTimer msTimer = new MSTimer();
    // To save memory
    private final ArmorPiece[] bestArmor = new ArmorPiece[4];
    private long delay;

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if (!msTimer.hasTimePassed(delay) || (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0))
            return;

        Map<Integer, List<ArmorPiece>> collect = IntStream.range(0, 36)
                .filter(i -> {
                            ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

                            return itemStack != null
                                    && itemStack.getItem() instanceof ItemArmor
                                    && (i < 9 || System.currentTimeMillis() - ((IItemStack) (Object) itemStack).getItemDelay() >= itemDelayValue.get());
                        }
                )
                .mapToObj(i -> new ArmorPiece(mc.thePlayer.inventory.getStackInSlot(i), i))
                .collect(Collectors.groupingBy(ArmorPiece::getArmorType));

        Arrays.fill(bestArmor, null);

        for (Map.Entry<Integer, List<ArmorPiece>> integerListEntry : collect.entrySet()) {
            bestArmor[integerListEntry.getKey()] = integerListEntry.getValue().stream().max(ARMOR_COMPARATOR).orElse(null);
        }

        for (int i = 0; i < 4; i++) {
            ArmorPiece bestArmorPiece = bestArmor[i];

            if (bestArmorPiece != null) {
                int armorSlot = 3 - i;
                ArmorPiece oldArmor = new ArmorPiece(mc.thePlayer.inventory.armorItemInSlot(armorSlot), -1);

                if (oldArmor.getItemStack() == null || ARMOR_COMPARATOR.compare(oldArmor, bestArmorPiece) < 0) {
                    if (oldArmor.getItemStack() != null) {
                        if (move(8 - armorSlot, true)) {
                            return;
                        }
                    }

                    if (move(bestArmorPiece.getSlot(), false)) {
                        return;
                    }
                }

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
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(item));
            mc.getNetHandler().addToSendQueue(new C08PacketPlayerBlockPlacement(mc.thePlayer.inventoryContainer.getSlot(item).getStack()));
            mc.getNetHandler().addToSendQueue(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));

            msTimer.reset();
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
        } else if (!(noMoveValue.get() && MovementUtils.isMoving()) && (!invOpenValue.get() || mc.currentScreen instanceof GuiInventory) && item != -1) {
            final boolean openInventory = simulateInventory.get() && !(mc.currentScreen instanceof GuiInventory);

            if (openInventory)
                mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));

            mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, isArmorSlot ? item : (item < 9 ? item + 36 : item), 0, 1, mc.thePlayer);

            msTimer.reset();
            delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());

            if (openInventory)
                mc.getNetHandler().addToSendQueue(new C0DPacketCloseWindow());

            return false;
        }
        return true;
    }

}
