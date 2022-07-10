package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.getEnchantmentLevel
import net.ccbluex.liquidbounce.utils.extensions.highlight
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.client.gui.GuiRepair
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.ai.attributes.AttributeModifier
import net.minecraft.inventory.Container
import net.minecraft.item.Item
import net.minecraft.item.ItemEnchantedBook
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.util.*

private const val CLICKINDICATION_BOOK2ITEM_ITEM = -6270721
private const val CLICKINDICATION_BOOK2ITEM_BOOK = -8388528
private const val CLICKINDICATION_ITEM2ITEM_FIRST = -8367903
private const val CLICKINDICATION_ITEM2ITEM_SECOND = -8355776

/**
 * LiquidBounce Hacked Client A minecraft forge injection client using Mixin
 *
 *  TODO
 * * Book-To-Armor support
 * * Book-To-FishingRod support
 * * Book-To-Bow support
 * * Book-To-Tool support
 * * Armor-To-Armor support
 * * FishingRod-To-FishingRod support
 * * Bow-To-Bow support
 * * Tool-To-Tool support
 *
 * @author CCBlueX
 * @game   Minecraft
 */
@ModuleInfo(name = "AutoEnchant", description = "Automatically enchant your weapon when you opened anvil.", category = ModuleCategory.PLAYER)
class AutoEnchant : Module()
{
    val delayValue = IntegerRangeValue("Delay", 100, 100, 0, 500)
    private val firstDelayValue = IntegerRangeValue("FirstDelay", 500, 500, 0, 1000)
    private val itemToItemValue = BoolValue("Item-To-Item", true, "SwordToSword")

    private val clickIndicationGroup = ValueGroup("ClickIndication")
    private val clickIndicationEnabledValue = BoolValue("Enabled", false, "ClickIndication")
    private val clickIndicationLengthValue = IntegerValue("Length", 100, 50, 1000, "ClickIndicationLength")

    init
    {
        clickIndicationGroup.addAll(clickIndicationEnabledValue, clickIndicationLengthValue)
    }

    private var delay = firstDelayValue.getRandomLong()
    private val delayTimer = MSTimer()

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val thePlayer = mc.thePlayer ?: return
        val screen = mc.currentScreen ?: return
        val playerController = mc.playerController

        if (screen is GuiRepair)
        {
            val container = screen.inventorySlots ?: return

            if (!delayTimer.hasTimePassed(delay)) return

            // Find a item for enchant. If there is no items for enchant found, close the gui automatically if option is present.

            // Find a best sword
            val triedEnchantSwordSlots = ArrayList<Int>()

            // For Anvil Inventory
            val firstInputSlot = container.getSlot(0)
            val secondInputSlot = container.getSlot(1)
            val outputSlot = container.getSlot(2)
            if (outputSlot.stack != null) // I'm sure there is no anticheat detecting autoenchant with output-slot item spoofing.
            {
                playerController.windowClick(container.windowId, outputSlot.slotNumber, 0, 1, thePlayer)
                delay = delayValue.getRandomLong()
                delayTimer.reset()
                return
            }

            val bestSwordSlot = findBestWeapon(container, 3, 39)

            val bestEnchantedBookSlots = findBestEnchantedBooks(container, 3, 39, listOf(Enchantment.sharpness, Enchantment.knockback, Enchantment.fireAspect, Enchantment.unbreaking))
            if (bestEnchantedBookSlots.isNotEmpty())
            {
                // Item-To-Book
                if (firstInputSlot.stack == null && bestSwordSlot != -1)
                {
                    playerController.windowClick(container.windowId, bestSwordSlot, 0, 1, thePlayer)

                    if (clickIndicationEnabledValue.get()) screen.highlight(bestSwordSlot, clickIndicationLengthValue.get().toLong(), CLICKINDICATION_BOOK2ITEM_ITEM)

                    delay = delayValue.getRandomLong()
                    delayTimer.reset()
                }
                else if (secondInputSlot.stack == null)
                {
                    playerController.windowClick(container.windowId, bestEnchantedBookSlots[0], 0, 1, thePlayer)

                    if (clickIndicationEnabledValue.get()) screen.highlight(bestSwordSlot, clickIndicationLengthValue.get().toLong(), CLICKINDICATION_BOOK2ITEM_BOOK)

                    delay = delayValue.getRandomLong()
                    delayTimer.reset()
                }
            }
            else if (itemToItemValue.get())
            {
                // Item-To-Item
                var secondSwordSlot = -1
                if (bestSwordSlot != -1)
                {
                    var whiteID: Int? = null
                    (if (firstInputSlot.stack == null) bestSwordSlot else firstInputSlot.slotNumber).let { slot ->
                        triedEnchantSwordSlots.add(slot)
                        container.getSlot(slot).stack?.item?.let { whiteID = Item.getIdFromItem(it) }
                    }
                    triedEnchantSwordSlots.add(outputSlot.slotNumber)

                    do
                    {
                        secondSwordSlot = findBestWeapon(container, 0, 39, whiteID, triedEnchantSwordSlots)
                        triedEnchantSwordSlots.add(secondSwordSlot)
                    } while (run {
                            if (secondSwordSlot == -1) return@run false
                            val bestSwordStack = container.getSlot(bestSwordSlot).stack ?: return@run false
                            val secondSwordStack = container.getSlot(secondSwordSlot).stack ?: return@run false

                            secondSwordStack.item?.let(Item::getIdFromItem)?.equals(bestSwordStack.item?.let(Item::getIdFromItem)) == true || !secondSwordStack.isItemEnchanted
                        })
                }

                if (firstInputSlot.stack == null)
                {
                    // First
                    if (bestSwordSlot != -1 && secondSwordSlot != -1)
                    {
                        playerController.windowClick(container.windowId, bestSwordSlot, 0, 1, thePlayer)

                        if (clickIndicationEnabledValue.get()) screen.highlight(bestSwordSlot, clickIndicationLengthValue.get().toLong(), CLICKINDICATION_ITEM2ITEM_FIRST)

                        delay = delayValue.getRandomLong()
                        delayTimer.reset()
                    }
                }
                else if (secondInputSlot.stack == null && secondSwordSlot != -1)
                {
                    // Second
                    playerController.windowClick(container.windowId, secondSwordSlot, 0, 1, thePlayer)

                    if (clickIndicationEnabledValue.get()) screen.highlight(bestSwordSlot, clickIndicationLengthValue.get().toLong(), CLICKINDICATION_ITEM2ITEM_SECOND)

                    delay = delayValue.getRandomLong()
                    delayTimer.reset()
                }
            }
        }
        else
        {
            delay = firstDelayValue.getRandomLong()
            delayTimer.reset()
        }
    }

    private fun findBestWeapon(container: Container, start: Int, end: Int, whitelistedID: Int? = null, blacklistedSlots: List<Int>? = null): Int = (start until end).filter { blacklistedSlots?.contains(it) != true }.mapNotNull { it to (container.getSlot(it).stack ?: return@mapNotNull null) }.filter { (_, stack) -> stack.item is ItemSword || stack.item is ItemTool }.filter { (_, stack) -> whitelistedID?.let { Item.getIdFromItem(stack.item ?: return@filter true) == it } != false }.maxByOrNull { (_, stack) ->
        stack.attributeModifiers["generic.attackDamage"].map(AttributeModifier::getAmount).sum() + 1.25 * stack.getEnchantmentLevel(Enchantment.sharpness)
    }?.first ?: -1

    private fun findBestEnchantedBooks(container: Container, start: Int, end: Int, enchantments: Collection<Enchantment>): List<Int>
    {
        val bestSlots: MutableList<Int> = ArrayList()

        for (enchantment in enchantments)
        {
            // Find the best enchanted book for each enchantment (#Kotlin style)
            val enchantID = enchantment.effectId
            val bestSlot = (start until end).mapNotNull { it to (container.getSlot(it).stack ?: return@mapNotNull null) }.filter { (_, stack) -> stack.item is ItemEnchantedBook }.maxByOrNull { (_, stack) ->
                EnchantmentHelper.getEnchantments(stack).filter { it.key == enchantID }.maxByOrNull(Map.Entry<Int, Int>::value)?.value ?: -1
            }?.first ?: -1

            if (bestSlot != -1 && !bestSlots.contains(bestSlot)) bestSlots.add(bestSlot)
        }

        return bestSlots
    }
}
