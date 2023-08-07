/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.TickEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.injection.implementations.IMixinItemStack;
import net.ccbluex.liquidbounce.utils.InventoryUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.item.ArmorComparator;
import net.ccbluex.liquidbounce.utils.item.ArmorPiece;
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
import net.minecraftforge.event.ForgeEventFactory;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket;
import static net.ccbluex.liquidbounce.utils.item.ItemUtilsKt.isEmpty;
import static net.minecraft.network.play.client.C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT;

public class AutoArmor extends Module {

    public AutoArmor() {
        super("AutoArmor", ModuleCategory.COMBAT);
    }

    public static final ArmorComparator ARMOR_COMPARATOR = new ArmorComparator();
    private final IntegerValue maxTicksValue = new IntegerValue("MaxTicks", 4, 0, 10) {
        @Override
        protected Integer onChange(final Integer oldValue, final Integer newValue) {
            final int minDelay = minTicksValue.get();

            return newValue > minDelay ? newValue : minDelay;
        }
    };
    private final IntegerValue minTicksValue = new IntegerValue("MinTicks", 2, 0, 10) {

        @Override
        protected Integer onChange(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxTicksValue.get();

            return newValue < maxDelay ? newValue : maxDelay;
        }

        @Override
        public boolean isSupported() {
            return !maxTicksValue.isMinimal();
        }
    };

    private final BoolValue invOpenValue = new BoolValue("InvOpen", false);
    private final BoolValue simulateInventory = new BoolValue("SimulateInventory", true) {
        @Override
        public boolean isSupported() {
            return !invOpenValue.get();
        }
    };
    private final BoolValue noMoveValue = new BoolValue("NoMove", false);
    private final IntegerValue itemTicksValue = new IntegerValue("ItemTicks", 0, 0, 20);
    private final BoolValue hotbarValue = new BoolValue("Hotbar", true);

    // Sacrifices 1 tick speed for complete undetectability
    private final BoolValue switchBackLegit = new BoolValue("SwitchBackLegit", true) {

        @Override
        public boolean isSupported() {
            return hotbarValue.get();
        }
    };

    private long delay;

    private boolean switchBack = false;

    private boolean locked = false;

    @EventTarget
    public void onTick(final TickEvent event) {
        // After waiting for the next tick, we set it back to the original slot
        if (switchBack) {
            sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            switchBack = false;
            return;
        }

        if (!InventoryUtils.INSTANCE.getCLICK_TIMER().hasTimePassed(delay * 50L) || mc.thePlayer == null || (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0))
            return;

        // Find best armor
        final Map<Integer, List<ArmorPiece>> armorPieces = IntStream.range(0, 36).filter(i -> {
            final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(i);

            return itemStack != null && itemStack.getItem() instanceof ItemArmor && (i < 9 || System.currentTimeMillis() - ((IMixinItemStack) (Object) itemStack).getItemDelay() >= itemTicksValue.get() * 50L);
        }).mapToObj(i -> new ArmorPiece(mc.thePlayer.inventory.getStackInSlot(i), i)).collect(Collectors.groupingBy(ArmorPiece::getArmorType));

        final ArmorPiece[] bestArmor = new ArmorPiece[4];

        for (final Map.Entry<Integer, List<ArmorPiece>> armorEntry : armorPieces.entrySet()) {
            bestArmor[armorEntry.getKey()] = armorEntry.getValue().stream().max(ARMOR_COMPARATOR).orElse(null);
        }

        // Swap armor
        for (int i = 0; i < 4; i++) {
            final ArmorPiece armorPiece = bestArmor[i];

            if (armorPiece == null) continue;

            int armorSlot = 3 - i;

            final ArmorPiece oldArmor = new ArmorPiece(mc.thePlayer.inventory.armorItemInSlot(armorSlot), -1);

            if (isEmpty(oldArmor.getItemStack()) || !(oldArmor.getItemStack().getItem() instanceof ItemArmor) || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0) {
                if (!isEmpty(oldArmor.getItemStack()) && move(8 - armorSlot, true)) {
                    locked = true;
                    return;
                }

                if (isEmpty(mc.thePlayer.inventory.armorItemInSlot(armorSlot)) && move(armorPiece.getSlot(), false)) {
                    locked = true;
                    return;
                }
            }
        }

        locked = false;
    }

    public boolean isLocked() {
        return getState() && locked;
    }

    /**
     * Shift+Left-clicks the specified item
     *
     * @param item        Slot of the item to click
     * @param isArmorSlot
     * @return True if it is unable to move the item
     */
    private boolean move(int item, boolean isArmorSlot) {
        if (!isArmorSlot && item < 9 && hotbarValue.get() && !(mc.currentScreen instanceof GuiInventory)) {
            sendPacket(new C09PacketHeldItemChange(item));

            useItem(item + 36);

            if (!switchBackLegit.get()) {
                sendPacket(new C09PacketHeldItemChange(mc.thePlayer.inventory.currentItem));
            }

            // Change slot in the next tick. Prevents detection by anti-cheats.
            switchBack = switchBackLegit.get();

            delay = TimeUtils.INSTANCE.randomDelay(minTicksValue.get(), maxTicksValue.get());

            return true;
        } else if (!(noMoveValue.get() && MovementUtils.INSTANCE.isMoving()) && (!invOpenValue.get() || mc.currentScreen instanceof GuiInventory) && item != -1) {
            final boolean openInventory = simulateInventory.get() && !(mc.currentScreen instanceof GuiInventory);

            if (openInventory) sendPacket(new C16PacketClientStatus(OPEN_INVENTORY_ACHIEVEMENT));

            boolean full = isArmorSlot;

            if (full) {
                for (ItemStack iItemStack : mc.thePlayer.inventory.mainInventory) {
                    if (isEmpty(iItemStack)) {
                        full = false;
                        break;
                    }
                }
            }

            if (full) {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, item, 1, 4, mc.thePlayer);
            } else {
                mc.playerController.windowClick(mc.thePlayer.inventoryContainer.windowId, (isArmorSlot ? item : (item < 9 ? item + 36 : item)), 0, 1, mc.thePlayer);
            }

            delay = TimeUtils.INSTANCE.randomDelay(minTicksValue.get(), maxTicksValue.get());

            if (openInventory) sendPacket(new C0DPacketCloseWindow());

            return true;
        }

        return false;
    }

    // Useful when silently switching slots.
    private void useItem(int slot) {
        ItemStack stack = mc.thePlayer.inventoryContainer.getSlot(slot).getStack();
        sendPacket((new C08PacketPlayerBlockPlacement(stack)));
        int i = stack.stackSize;
        ItemStack itemstack = stack.useItemRightClick(mc.theWorld, mc.thePlayer);
        if (itemstack != stack || itemstack.stackSize != i) {
            mc.thePlayer.inventory.mainInventory[slot - 36] = itemstack;
            if (itemstack.stackSize <= 0) {
                mc.thePlayer.inventory.mainInventory[slot - 36] = null;
                ForgeEventFactory.onPlayerDestroyItem(mc.thePlayer, itemstack);
            }
        }
    }

}
