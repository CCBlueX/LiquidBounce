/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.EntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.utils.extensions.isEmpty
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.timer.Cooldown
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ValueGroup
import java.lang.ref.SoftReference
import java.util.*

private const val CLICKINDICATION_SHIFT_LEFT = -2147462913
private const val CLICKINDICATION_RIGHT_ARMORSLOT = -2147450625
private const val CLICKINDICATION_RIGHT = -2147418113

@ModuleInfo(name = "AutoArmor", description = "Automatically equips the best armor in your inventory.", category = ModuleCategory.COMBAT)
class AutoArmor : Module()
{
    /**
     * Options
     */
    private val delayValue = IntegerRangeValue("Delay", 100, 200, 0, 1000, "MaxDelay" to "MinDelay")
    private val invOpenValue = BoolValue("InvOpen", false)
    private val simulateInventory = BoolValue("SimulateInventory", true)
    private val noMoveValue = BoolValue("NoMove", false)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)
    private val hotbarValue = BoolValue("Hotbar", true)

    // Visuals
    private val clickIndicationGroup = ValueGroup("ClickIndication")
    private val clickIndicationEnabledValue = BoolValue("Enabled", false, "ClickIndication")
    private val clickIndicationLengthValue = IntegerValue("Length", 100, 50, 1000, "ClickIndicationLength")

    init
    {
        clickIndicationGroup.addAll(clickIndicationEnabledValue, clickIndicationLengthValue)
    }

    /**
     * Variables
     */
    private var nextDelay: Long = 0
    private var locked = false

    private val infoUpdateCooldown = Cooldown.createCooldownInMillis(100)

    private var cachedDebug: SoftReference<String>? = null

    val debug: String
        get()
        {
            val cache = cachedDebug?.get()

            return if (cache == null || infoUpdateCooldown.attemptReset()) (if (!state) "AutoArmor disabled"
            else
            {
                val builder = StringJoiner(", ")

                builder.add("DELAY(${delayValue.getMin()}-${delayValue.getMax()}ms)")
                builder.add("ITEMDELAY(${itemDelayValue.get()}ms)")

                if (invOpenValue.get()) builder.add("INVOPEN")

                if (simulateInventory.get()) builder.add("SIMULATE")

                if (noMoveValue.get()) builder.add("NOMOVE")

                if (hotbarValue.get()) builder.add("HOTBAR")

                "AutoArmor ENABLED {$builder}"
            }).also { cachedDebug = SoftReference(it) }
            else cache
        }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val thePlayer = mc.thePlayer ?: return
        val netHandler = mc.netHandler
        val inventory = thePlayer.inventory
        val openContainer = thePlayer.openContainer

        if (!InventoryUtils.CLICK_TIMER.hasTimePassed(nextDelay) || openContainer != null && openContainer.windowId != 0) return

        val provider = classProvider

        val itemDelay = itemDelayValue.get()

        val currentTime = System.currentTimeMillis()

        // Find best armor
        val armorPieces = (0 until 36).mapNotNull { it to (inventory.getStackInSlot(it) ?: return@mapNotNull null) }.filter { (slot, stack) -> stack.item is ItemArmor && (slot < 9 || currentTime - stack.itemDelay >= itemDelay) }.map { (slot, stack) -> ArmorPiece(stack, slot) }.groupBy(ArmorPiece::armorType)

        val bestArmor = arrayOfNulls<ArmorPiece>(4)
        for ((armorType, candidates) in armorPieces) bestArmor[armorType] = candidates.maxWith(ARMOR_COMPARATOR)

        // Swap armor
        if ((0..3).mapNotNull { i ->
                val armorSlot = 3 - i
                Triple(bestArmor[i] ?: return@mapNotNull null, armorSlot, ArmorPiece(inventory.armorItemInSlot(armorSlot), -1))
            }.filter { (armorPiece, _, oldArmor) ->
                val oldArmorStack = oldArmor.itemStack
                oldArmorStack.isEmpty || oldArmorStack?.item !is ItemArmor || ARMOR_COMPARATOR.compare(oldArmor, armorPiece) < 0
            }.any { (armorPiece, armorSlot, oldArmor) -> if (oldArmor.itemStack.isEmpty) move(thePlayer, netHandler, armorPiece.slot, false) else move(thePlayer, netHandler, 8 - armorSlot, true) })
        {
            locked = true
            return
        }

        locked = false
    }

    val isLocked: Boolean
        get() = state && locked

    /**
     * Shift+Left clicks the specified item
     *
     * @param  item
     * Slot of the item to click
     * @param  isArmorSlot
     * @return             False if it is unable to move the item
     */
    private fun move(thePlayer: EntityPlayerSP, netHandler: IINetHandlerPlayClient, item: Int, isArmorSlot: Boolean): Boolean
    {
        val screen = mc.currentScreen
        val controller = mc.playerController

        val provider = classProvider

        if (!isArmorSlot && item < 9 && hotbarValue.get() && screen !is GuiInventory)
        {
            if (InventoryUtils.tryHoldSlot(thePlayer, item))
            {
                netHandler.addToSendQueue(createUseItemPacket(thePlayer.inventoryContainer.getSlot(item).stack, WEnumHand.MAIN_HAND))
                InventoryUtils.resetSlot(thePlayer)

                nextDelay = delayValue.getRandomLong()

                return true
            }
        }

        if (!(noMoveValue.get() && thePlayer.isMoving) && (!invOpenValue.get() || screen is GuiInventory) && item != -1)
        {
            val openInventory = simulateInventory.get() && screen !is GuiInventory

            if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

            var full = isArmorSlot

            if (full) full = thePlayer.inventory.mainInventory.none(IItemStack?::isEmpty)

            val slot = if (full || isArmorSlot) item else if (item < 9) item + 36 else item
            controller.windowClick(thePlayer.inventoryContainer.windowId, slot, if (full) 1 else 0, if (full) 4 else 1, thePlayer)
            if (clickIndicationEnabledValue.get() && screen != null && screen is GuiContainer) screen.asGuiContainer().highlight(slot, clickIndicationLengthValue.get().toLong(), if (full) CLICKINDICATION_SHIFT_LEFT else if (isArmorSlot) CLICKINDICATION_RIGHT_ARMORSLOT else CLICKINDICATION_RIGHT)

            nextDelay = delayValue.getRandomLong()

            if (openInventory) netHandler.addToSendQueue(CPacketCloseWindow())

            return true
        }
        return false
    }

    override val tag: String
        get() = "${delayValue.getMin()} ~ ${delayValue.getMax()}"

    companion object
    {
        val ARMOR_COMPARATOR: Comparator<ArmorPiece> = ArmorComparator()
    }
}
