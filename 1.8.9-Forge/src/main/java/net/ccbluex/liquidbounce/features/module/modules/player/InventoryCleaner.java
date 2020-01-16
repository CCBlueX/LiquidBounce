package net.ccbluex.liquidbounce.features.module.modules.player;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.Render3DEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.injection.implementations.IItemStack;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.MovementUtils;
import net.ccbluex.liquidbounce.utils.item.Armor;
import net.ccbluex.liquidbounce.utils.item.ItemUtils;
import net.ccbluex.liquidbounce.utils.timer.MSTimer;
import net.ccbluex.liquidbounce.utils.timer.TimeUtils;
import net.ccbluex.liquidbounce.value.BoolValue;
import net.ccbluex.liquidbounce.value.IntegerValue;
import net.ccbluex.liquidbounce.value.ListValue;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.network.play.client.C0DPacketCloseWindow;
import net.minecraft.network.play.client.C16PacketClientStatus;

import java.util.*;
import java.util.stream.Collectors;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@ModuleInfo(name = "InventoryCleaner", description = "Automatically throws away useless items.", category = ModuleCategory.PLAYER)
public class InventoryCleaner extends Module {

    private final IntegerValue maxDelayValue = new IntegerValue("MaxDelay", 600, 0, 1000) {
        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int minCPS = minDelayValue.get();

            if(minCPS > newValue)
                set(minCPS);
        }
    };

    private final IntegerValue minDelayValue = new IntegerValue("MinDelay", 400, 0, 1000) {

        @Override
        protected void onChanged(final Integer oldValue, final Integer newValue) {
            final int maxDelay = maxDelayValue.get();

            if(maxDelay < newValue)
                set(maxDelay);
        }
    };

    private final BoolValue invOpenValue = new BoolValue("InvOpen", false);
    private final BoolValue simulateInventory = new BoolValue("SimulateInventory", true);
    private final BoolValue noMoveValue = new BoolValue("NoMove", false);
    private final BoolValue hotbarValue = new BoolValue("Hotbar", true);
    private final BoolValue randomSlotValue = new BoolValue("RandomSlot", false);
    private final BoolValue sortValue = new BoolValue("Sort", true);
    private final IntegerValue itemDelayValue = new IntegerValue("ItemDelay", 0, 0, 5000);

    private final String[] items = new String[] {"None", "Ignore", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water"};
    private final ListValue sortSlot1Value = new ListValue("SortSlot-1", items, "Sword");
    private final ListValue sortSlot2Value = new ListValue("SortSlot-2", items, "Bow");
    private final ListValue sortSlot3Value = new ListValue("SortSlot-3", items, "Pickaxe");
    private final ListValue sortSlot4Value = new ListValue("SortSlot-4", items, "Axe");
    private final ListValue sortSlot5Value = new ListValue("SortSlot-5", items, "None");
    private final ListValue sortSlot6Value = new ListValue("SortSlot-6", items, "None");
    private final ListValue sortSlot7Value = new ListValue("SortSlot-7", items, "Food");
    private final ListValue sortSlot8Value = new ListValue("SortSlot-8", items, "Block");
    private final ListValue sortSlot9Value = new ListValue("SortSlot-9", items, "Block");

    private final MSTimer clickTimer = new MSTimer();
    private long delay;

    @EventTarget
    public void onRender3D(final Render3DEvent event) {
        if(!clickTimer.hasTimePassed(delay) || (!(mc.currentScreen instanceof GuiInventory) && invOpenValue.get()) || (noMoveValue.get() && MovementUtils.isMoving()) || (mc.thePlayer.openContainer != null && mc.thePlayer.openContainer.windowId != 0))
            return;

        final Map.Entry<Integer, ItemStack>[] entries = getItems(9, hotbarValue.get() ? 45 : 36).entrySet()
                .stream()
                .filter(stackEntry -> !isUseful(stackEntry.getValue())).sorted((o1, o2) -> randomSlotValue.get() ? new Random().nextBoolean() ? 1 : -1 : 1)
                .toArray(Map.Entry[] ::new);

        if(entries.length == 0) {
            sortInventory();
            return;
        }

        for(final Map.Entry<Integer, ItemStack> stackEntry : entries) {
            final int slot = stackEntry.getKey();
            final ItemStack itemStack = stackEntry.getValue();

            if(!isUseful(itemStack)) {
                final boolean openInventory = !(mc.currentScreen instanceof GuiInventory) && simulateInventory.get();

                if(openInventory)
                    mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));

                mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, 4, 4, mc.thePlayer);

                if(openInventory)
                    mc.getNetHandler().addToSendQueue(new C0DPacketCloseWindow());

                clickTimer.reset();
                delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
                break;
            }
        }
    }

    public boolean isUseful(final ItemStack itemStack) {
        try {
            final Item item = itemStack.getItem();

            if(item instanceof ItemSword || item instanceof ItemTool) {
                for(final AttributeModifier attributeModifier : itemStack.getAttributeModifiers().get("generic.attackDamage")) {
                    final double damage = attributeModifier.getAmount() + (1.25D * ItemUtils.getEnchantment(itemStack, Enchantment.sharpness));

                    for(final ItemStack anotherStack : getItems(0, 45).values()) {
                        if(itemStack.equals(anotherStack) || item.getClass() != anotherStack.getItem().getClass())
                            continue;

                        for(final AttributeModifier anotherAttributeModifier : anotherStack.getAttributeModifiers().get("generic.attackDamage"))
                            if(damage <= anotherAttributeModifier.getAmount() + (1.25D * ItemUtils.getEnchantment(anotherStack, Enchantment.sharpness)))
                                return false;
                    }
                }

                return true;
            }else if(item instanceof ItemBow) {
                final int bowPower = ItemUtils.getEnchantment(itemStack, Enchantment.power);

                for(final ItemStack anotherStack : getItems(0, 45).values()) {
                    if(itemStack.equals(anotherStack) || !(anotherStack.getItem() instanceof ItemBow))
                        continue;

                    if(ItemUtils.getEnchantment(anotherStack, Enchantment.power) >= bowPower)
                        return false;
                }
                return true;
            }else if(item instanceof ItemArmor) {
                final ItemArmor itemArmor = (ItemArmor) item;
                final int itemID = Item.getIdFromItem(item);

                final int[] array = Armor.getArmorArray(itemArmor);

                if(array == null)
                    return true;

                final List<Integer> armorArray = Arrays.stream(array).boxed().collect(Collectors.toList());

                for(final ItemStack anotherStack : getItems(0, 45).values()) {
                    if(!(anotherStack.getItem() instanceof ItemArmor) || itemStack.equals(anotherStack) || itemArmor.armorType != ((ItemArmor) anotherStack.getItem()).armorType)
                        continue;

                    if(armorArray.indexOf(itemID) >= armorArray.indexOf(Item.getIdFromItem(anotherStack.getItem())) && ItemUtils.getEnchantment(itemStack, Enchantment.protection) <= ItemUtils.getEnchantment(anotherStack, Enchantment.protection))
                        return false;
                }

                return true;
            }else if(itemStack.getUnlocalizedName().equals("item.compass")) {
                for(final ItemStack anotherStack : getItems(0, 45).values())
                    if(!itemStack.equals(anotherStack) && anotherStack.getUnlocalizedName().equals("item.compass"))
                        return false;

                return true;
            }else
                return item instanceof ItemFood || itemStack.getUnlocalizedName().equals("item.arrow") || item instanceof ItemBlock && !itemStack.getUnlocalizedName().contains("flower") || item instanceof ItemBed || itemStack.getUnlocalizedName().equals("item.diamond") || itemStack.getUnlocalizedName().equals("item.ingotIron") || item instanceof ItemPotion || item instanceof ItemEnderPearl || item instanceof ItemEnchantedBook || item instanceof ItemBucket || itemStack.getUnlocalizedName().equals("item.stick");
        }catch(final Throwable t) {
            ClientUtils.getLogger().error("(InventoryCleaner) Failed to check item: " + itemStack.getUnlocalizedName() + ".", t);
            return true;
        }
    }

    private void sortInventory() {
        if(!sortValue.get())
            return;

        for(int targetSlot = 0; targetSlot < 9; targetSlot++) {
            final ItemStack slotStack = mc.thePlayer.inventory.getStackInSlot(targetSlot);
            final SortCallback sortCallback = searchSortItem(targetSlot, slotStack);

            if(sortCallback != null && sortCallback.getItemSlot() != targetSlot && (sortCallback.isReplaceCurrentItem() || slotStack == null)) {
                final boolean openInventory = !(mc.currentScreen instanceof GuiInventory) && simulateInventory.get();

                if (openInventory)
                    mc.getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));

                mc.playerController.windowClick(0, sortCallback.getItemSlot() < 9 ? sortCallback.getItemSlot() + 36 : sortCallback.getItemSlot(), targetSlot, 2, mc.thePlayer);

                if (openInventory)
                    mc.getNetHandler().addToSendQueue(new C0DPacketCloseWindow());

                clickTimer.reset();
                delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get());
                break;
            }
        }
    }

    private String getType(final int targetSlot) {
        switch(targetSlot) {
            case 0:
                return sortSlot1Value.get();
            case 1:
                return sortSlot2Value.get();
            case 2:
                return sortSlot3Value.get();
            case 3:
                return sortSlot4Value.get();
            case 4:
                return sortSlot5Value.get();
            case 5:
                return sortSlot6Value.get();
            case 6:
                return sortSlot7Value.get();
            case 7:
                return sortSlot8Value.get();
            case 8:
                return sortSlot9Value.get();
        }

        return "";
    }


    private SortCallback searchSortItem(final int targetSlot, final ItemStack slotStack) {
        final String targetType = getType(targetSlot);

        switch(targetType.toLowerCase()) {
            case "sword":
            case "pickaxe":
            case "axe": {
                final Class<? extends Item> currentType = targetType.equalsIgnoreCase("Sword") ? ItemSword.class : targetType.equalsIgnoreCase("Pickaxe") ? ItemPickaxe.class : targetType.equalsIgnoreCase("Axe") ? ItemAxe.class : null;
                int bestWeapon = slotStack != null && slotStack.getItem().getClass() == currentType ? targetSlot : -1;

                for(int sourceSlot = 0; sourceSlot < mc.thePlayer.inventory.mainInventory.length; sourceSlot++) {
                    final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(sourceSlot);

                    if(itemStack != null && itemStack.getItem().getClass() == currentType &&
                            !getType(sourceSlot).equalsIgnoreCase(targetType)) {

                        if(bestWeapon == -1) {
                            bestWeapon = sourceSlot;
                            continue;
                        }

                        for(final AttributeModifier attributeModifier : itemStack.getAttributeModifiers().get("generic.attackDamage")) {
                            final double damage = attributeModifier.getAmount() + (1.25D * ItemUtils.getEnchantment(itemStack, Enchantment.sharpness));

                            final ItemStack anotherStack = mc.thePlayer.inventory.getStackInSlot(bestWeapon);

                            if(anotherStack == null)
                                continue;

                            for(final AttributeModifier anotherAttributeModifier : anotherStack.getAttributeModifiers().get("generic.attackDamage"))
                                if(damage < anotherAttributeModifier.getAmount() + (1.25D * ItemUtils.getEnchantment(anotherStack, Enchantment.sharpness)))
                                    bestWeapon = sourceSlot;
                        }
                    }
                }

                if(bestWeapon == -1)
                    return null;

                return new SortCallback(bestWeapon, true);
            }
            case "bow": {
                int bestWeapon = -1;

                for(int sourceSlot = 0; sourceSlot < mc.thePlayer.inventory.mainInventory.length; sourceSlot++) {
                    final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(sourceSlot);

                    if(itemStack != null && itemStack.getItem() instanceof ItemBow &&
                            !getType(sourceSlot).equalsIgnoreCase(targetType)) {
                        if(bestWeapon == -1) {
                            bestWeapon = sourceSlot;
                            continue;
                        }

                        final ItemStack anotherStack = mc.thePlayer.inventory.getStackInSlot(bestWeapon);

                        if(ItemUtils.getEnchantment(itemStack, Enchantment.power) > ItemUtils.getEnchantment(anotherStack, Enchantment.power))
                            bestWeapon = sourceSlot;
                    }
                }

                if(bestWeapon == -1)
                    return null;

                return new SortCallback(bestWeapon, true);
            }
            case "food": {
                for(int sourceSlot = 0; sourceSlot < mc.thePlayer.inventory.mainInventory.length; sourceSlot++) {
                    final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(sourceSlot);

                    if(itemStack != null && itemStack.getItem() instanceof ItemFood && !getType(sourceSlot).equalsIgnoreCase("Food"))
                        return new SortCallback(sourceSlot, slotStack == null || !(slotStack.getItem() instanceof ItemFood));
                }
                break;
            }
            case "block": {
                for(int sourceSlot = 0; sourceSlot < mc.thePlayer.inventory.mainInventory.length; sourceSlot++) {
                    final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(sourceSlot);

                    if(itemStack != null && itemStack.getItem() instanceof ItemBlock && !getType(sourceSlot).equalsIgnoreCase("Block"))
                        return new SortCallback(sourceSlot, slotStack == null || !(slotStack.getItem() instanceof ItemBlock));
                }
                break;
            }
            case "water":
                for(int sourceSlot = 0; sourceSlot < mc.thePlayer.inventory.mainInventory.length; sourceSlot++) {
                    final ItemStack itemStack = mc.thePlayer.inventory.getStackInSlot(sourceSlot);

                    if(itemStack != null && itemStack.getItem() instanceof ItemBucket && ((ItemBucket) itemStack.getItem()).isFull == Blocks.flowing_water && !getType(sourceSlot).equalsIgnoreCase("Water"))
                        return new SortCallback(sourceSlot, slotStack == null || !(slotStack.getItem() instanceof ItemBucket && ((ItemBucket) slotStack.getItem()).isFull == Blocks.water));
                }
                break;
        }

        return null;
    }

    private Map<Integer, ItemStack> getItems(final int start, final int end) {
        final Map<Integer, ItemStack> itemsMap = new HashMap<>();

        for(int i = end - 1; i >= start; i--) {
            final ItemStack itemStack = mc.thePlayer.inventoryContainer.getSlot(i).getStack();

            if(i >= 36 && i <= 44 && getType(i).equalsIgnoreCase("Ignore"))
                continue;

            if(itemStack != null && itemStack.getItem() != null && System.currentTimeMillis() - ((IItemStack) (Object) itemStack).getItemDelay() >= itemDelayValue.get())
                itemsMap.put(i, itemStack);

        }

        return itemsMap;
    }

    public class SortCallback {

        private final int itemSlot;
        private final boolean replaceCurrentItem;

        public SortCallback(final int itemSlot, final boolean replaceCurrentItem) {
            this.itemSlot = itemSlot;
            this.replaceCurrentItem = replaceCurrentItem;
        }

        public int getItemSlot() {
            return itemSlot;
        }

        public boolean isReplaceCurrentItem() {
            return replaceCurrentItem;
        }
    }
}