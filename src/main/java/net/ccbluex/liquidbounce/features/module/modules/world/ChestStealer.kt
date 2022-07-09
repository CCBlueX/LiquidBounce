/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.gui.inventory.IGuiChest
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.inventory.ISlot
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner
import net.ccbluex.liquidbounce.utils.extensions.isEmpty
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ValueGroup
import java.lang.ref.SoftReference
import java.util.*
import kotlin.math.max
import kotlin.random.Random

private const val CLICKINDICATION_MISCLICK = -2130771968 /* 0x80FF0000 */
private const val CLICKINDICATION_TAKE = -2147418368 /* 0x8000FF00 */

// TODO: Silent option (Steal without opening GUI)
@ModuleInfo(name = "ChestStealer", description = "Automatically steals items from a chest.", category = ModuleCategory.WORLD)
class ChestStealer : Module()
{
    /**
     * OPTIONS
     */

    private val delayValue = IntegerRangeValue("Delay", 150, 200, 0, 2000, "MaxDelay" to "MinDelay")
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

    private val takeRandomizedValue = BoolValue("TakeRandomized", false)
    private val onlyItemsValue = BoolValue("OnlyItems", false)
    private val noCompassValue = BoolValue("NoCompass", false)
    // private val invCleanBeforeSteal = BoolValue("PerformInvCleanBeforeSteal", true) // Disabled due bug

    private val chestTitleValue = BoolValue("ChestTitle", false)

    private val delayOnFirstGroup = ValueGroup("DelayOnFirst")
    private val delayOnFirstEnabledValue = BoolValue("Enabled", false, "DelayOnFirst")
    private val delayOnFirstDelayValue: IntegerRangeValue = object : IntegerRangeValue("Delay", 0, 0, 0, 2000, "MaxFirstDelay" to "MinFirstDelay")
    {
        override fun onMaxValueChanged(oldValue: Int, newValue: Int)
        {
            nextDelay = TimeUtils.randomDelay(getMin(), newValue)
        }

        override fun onMinValueChanged(oldValue: Int, newValue: Int)
        {
            nextDelay = TimeUtils.randomDelay(newValue, getMax())
        }
    }

    private val misclickGroup = ValueGroup("ClickMistakes")
    private val misclickEnabledValue = BoolValue("Enabled", false, "ClickMistakes")
    private val misclickChanceValue = IntegerValue("Chance", 5, 1, 100, "ClickMistakeRate")
    private val misclickMaxPerChestValue = IntegerValue("MaxPerChest", 5, 1, 10, "MaxClickMistakesPerChest")

    private val autoCloseGroup = ValueGroup("AutoClose")
    private val autoCloseEnabledValue = BoolValue("Enabled", true, "AutoClose")
    private val autoCloseOnFullValue = BoolValue("CloseOnFull", true, "CloseOnFull")
    private val autoCloseDelayValue: IntegerRangeValue = object : IntegerRangeValue("Delay", 0, 0, 0, 2000, "AutoCloseMaxDelay" to "AutoCloseMinDelay")
    {
        override fun onMaxValueChanged(oldValue: Int, newValue: Int)
        {
            nextCloseDelay = TimeUtils.randomDelay(getMin(), newValue)
        }

        override fun onMinValueChanged(oldValue: Int, newValue: Int)
        {
            nextCloseDelay = TimeUtils.randomDelay(newValue, getMax())
        }
    }

    private val clickIndicationGroup = ValueGroup("ClickIndication")
    private val clickIndicationEnabledValue = BoolValue("Enabled", true, "ClickIndication")
    private val clickIndicationLengthValue = IntegerValue("Length", 100, 50, 1000, "ClickIndicationLength")

    /**
     * VALUES
     */

    private val delayTimer = MSTimer()
    private var nextDelay = delayValue.getRandomLong()

    private val autoCloseTimer = MSTimer()
    private var nextCloseDelay = autoCloseDelayValue.getRandomLong()

    private var contentReceived = 0

    // Remaining Misclicks count
    private var remainingMisclickCount = misclickMaxPerChestValue.get()

    private val infoUpdateCooldown = Cooldown.createCooldownInMillis(100)

    private var cachedDebug: SoftReference<String>? = null

    val debug: String
        get()
        {
            val cache = cachedDebug?.get()

            return if (cache == null || infoUpdateCooldown.attemptReset()) (if (!state) "ChestStealer disabled"
            else
            {
                val builder = StringJoiner(", ")

                builder.add("FIRSTDELAY(${delayOnFirstDelayValue.getMin()}-${delayOnFirstDelayValue.getMax()}ms)")
                builder.add("DELAY(${delayValue.getMin()}-${delayValue.getMax()}ms)")
                builder.add("ITEMDELAY(${itemDelayValue.get()}ms)")

                if (autoCloseEnabledValue.get()) builder.add("AUTOCLOSE[DELAY(${autoCloseDelayValue.getMin()}-${autoCloseDelayValue.getMax()}ms)${if (autoCloseOnFullValue.get()) ", ON_FULL" else ""}]")

                if (misclickEnabledValue.get()) builder.add("MISCLICK[CHANCE(${misclickChanceValue.get()}%), LIMIT(${misclickMaxPerChestValue.get()}/chest)]")

                if (LiquidBounce.moduleManager[InventoryCleaner::class.java].state) builder.add("ONLYUSEFUL")

                if (takeRandomizedValue.get()) builder.add("RANDOM")

                if (chestTitleValue.get()) builder.add("CHESTTITLE")

                if (onlyItemsValue.get()) builder.add("ONLYITEMS")

                "ChestStealer ENABLED {$builder}"
            }).also { cachedDebug = SoftReference(it) }
            else cache
        }

    init
    {
        delayOnFirstGroup.addAll(delayOnFirstEnabledValue, delayOnFirstDelayValue)
        misclickGroup.addAll(misclickEnabledValue, misclickChanceValue, misclickMaxPerChestValue)
        autoCloseGroup.addAll(autoCloseEnabledValue, autoCloseEnabledValue, autoCloseOnFullValue, autoCloseDelayValue)
        clickIndicationGroup.addAll(clickIndicationEnabledValue, clickIndicationLengthValue)
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        val provider = classProvider

        val itemDelay = itemDelayValue.get().toLong()

        if (mc.currentScreen !is GuiChest)
        {
            if (delayOnFirstEnabledValue.get() || itemDelay > 0L)
            {
                delayTimer.reset()
                if (nextDelay < delayOnFirstDelayValue.getMin()) nextDelay = max(delayOnFirstDelayValue.getRandomLong(), itemDelay)
            }
            autoCloseTimer.reset()
            return
        }

        if (!delayTimer.hasTimePassed(nextDelay))
        {
            autoCloseTimer.reset()
            return
        }

        val screen = (mc.currentScreen ?: return).asGuiChest()
        val lowerChestInventory = screen.lowerChestInventory

        // No Compass
        if (noCompassValue.get() && thePlayer.inventory.getCurrentItemInHand()?.item?.unlocalizedName == "item.compass") return

        // Chest title
        if (chestTitleValue.get() && (lowerChestInventory == null || !lowerChestInventory.name.contains(functions.getObjectFromItemRegistry(ResourceLocation("minecraft:chest"))?.let { ItemStack(it).displayName } ?: "Chest"))) return

        // inventory cleaner
        val inventoryCleaner = LiquidBounce.moduleManager[InventoryCleaner::class.java] as InventoryCleaner

        // Is empty?
        val notEmpty = !this.isEmpty(thePlayer, screen, itemDelay)

        val container = screen.inventorySlots ?: return
        val end = screen.inventoryRows * 9

        if (container.windowId != contentReceived)
        {
            autoCloseTimer.reset()
            return
        }

        // Disabled due bug
        // // Perform the InventoryCleaner before start stealing if option is present and InventoryCleaner is enabled. This will be helpful if player's inventory is nearly fucked up with tons of garbage. The settings of InventoryCleaner is depends on InventoryCleaner's official settings.
        // if (notEmpty && invCleanBeforeSteal.get() && inventoryCleaner.state && !inventoryCleaner.cleanInventory(start = end, end = end + if (inventoryCleaner.hotbarValue.get()) 36 else 27, timer = InventoryUtils.CLICK_TIMER, container = container, delayResetFunc = Runnable { nextDelay = TimeUtils.randomDelay(inventoryCleaner.minDelayValue.get(), inventoryCleaner.maxDelayValue.get()) })) return

        if (notEmpty && (!autoCloseOnFullValue.get() || !getFullInventory(thePlayer)))
        {
            autoCloseTimer.reset()

            // Pick Randomized
            if (takeRandomizedValue.get())
            {
                do
                {
                    val items = (0 until end).map(container::getSlot).filter { shouldTake(thePlayer, it.stack, it.slotNumber, inventoryCleaner, end, container, itemDelay) }.toList()

                    val randomSlot = Random.nextInt(items.size)
                    var slot = items[randomSlot]

                    var misclick = false

                    // Simulate Click Mistakes to bypass some anti-cheats
                    if (misclickEnabledValue.get() && remainingMisclickCount > 0 && misclickChanceValue.get() > 0 && Random.nextInt(100) <= misclickChanceValue.get())
                    {
                        val firstEmpty: ISlot? = firstEmpty(container.inventorySlots, end, true)
                        if (firstEmpty != null)
                        {
                            slot = firstEmpty
                            remainingMisclickCount--
                            misclick = true
                        }
                    }

                    move(screen, slot, misclick)
                } while (delayTimer.hasTimePassed(nextDelay) && items.isNotEmpty())
                return
            }

            // Non randomized
            for (slotIndex in end - 1 downTo 0) // Reversed-direction
            {
                var slot = container.getSlot(slotIndex)
                val stack = slot.stack

                if (delayTimer.hasTimePassed(nextDelay) && shouldTake(thePlayer, stack, slot.slotNumber, inventoryCleaner, end, container, itemDelay))
                {
                    var misclick = false

                    if (misclickEnabledValue.get() && remainingMisclickCount > 0 && misclickChanceValue.get() > 0 && Random.nextInt(100) <= misclickChanceValue.get())
                    {
                        val firstEmpty: ISlot? = firstEmpty(container.inventorySlots, end, false)
                        if (firstEmpty != null)
                        {
                            slot = firstEmpty
                            remainingMisclickCount--
                            misclick = true
                        }
                    }

                    move(screen, slot, misclick)
                }
            }
        }
        else if (autoCloseEnabledValue.get() && autoCloseTimer.hasTimePassed(nextCloseDelay))
        {
            thePlayer.closeScreen()
            nextCloseDelay = autoCloseDelayValue.getRandomLong()
        }
    }

    @EventTarget
    private fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (packet is SPacketWindowItems) contentReceived = packet.asSPacketWindowItems().windowId
    }

    private fun shouldTake(thePlayer: EntityPlayer, stack: IItemStack?, slot: Int, inventoryCleaner: InventoryCleaner, end: Int, container: IContainer, itemDelay: Long): Boolean = stack != null && (!onlyItemsValue.get() || !stack.item is ItemBlock) && (System.currentTimeMillis() - stack.itemDelay >= itemDelay && (!inventoryCleaner.state || inventoryCleaner.isUseful(thePlayer, slot, stack, end = end, container = container) && inventoryCleaner.isUseful(thePlayer, -1, stack, container = thePlayer.inventoryContainer) /* 상자 안에서 가장 좋은 템이랑 인벤 안의 가장 좋은 템이랑 비교한 후, 상자 안의 것이 더 좋을 경우에만 가져가기 */))

    private fun move(screen: IGuiChest, slot: ISlot, misclick: Boolean)
    {
        screen.handleMouseClick(slot, slot.slotNumber, 0, 1)
        if (clickIndicationEnabledValue.get()) screen.asGuiContainer().highlight(slot.slotNumber, clickIndicationLengthValue.get().toLong(), if (misclick) CLICKINDICATION_MISCLICK else CLICKINDICATION_TAKE)
        delayTimer.reset()
        nextDelay = delayValue.getRandomLong()
    }

    private fun isEmpty(thePlayer: EntityPlayer, chest: IGuiChest, itemDelay: Long): Boolean
    {
        val inventoryCleaner = LiquidBounce.moduleManager[InventoryCleaner::class.java] as InventoryCleaner
        val container = chest.inventorySlots ?: return false
        val end = chest.inventoryRows * 9

        return (0 until end).map(container::getSlot).none { shouldTake(thePlayer, it.stack, it.slotNumber, inventoryCleaner, end, container, itemDelay) }
    }

    private fun firstEmpty(slots: List<ISlot>?, length: Int, random: Boolean): ISlot?
    {
        slots ?: return null
        val emptySlots = (0 until length).map { slots[it] }.filter { it.stack == null }

        if (emptySlots.isEmpty()) return null

        return if (random) emptySlots[Random.nextInt(emptySlots.size)] else emptySlots.first()
    }

    private fun getFullInventory(thePlayer: EntityPlayer): Boolean = thePlayer.inventory.mainInventory.none(IItemStack?::isEmpty)

    override val tag: String
        get() = "${delayValue.getMin()} ~ ${delayValue.getMax()}"
}
