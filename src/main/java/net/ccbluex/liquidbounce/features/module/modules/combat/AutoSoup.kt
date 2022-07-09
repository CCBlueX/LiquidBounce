/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.BlockPos
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.utils.extensions.findItem
import net.ccbluex.liquidbounce.utils.extensions.firstEmpty
import net.ccbluex.liquidbounce.utils.extensions.hasSpaceHotbar
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import kotlin.random.Random

@ModuleInfo(name = "AutoSoup", description = "Makes you automatically eat soup whenever your health is low.", category = ModuleCategory.COMBAT)
class AutoSoup : Module()
{
    private val healthValue = FloatValue("Health", 15f, 0f, 20f)

    private val delayValue = IntegerRangeValue("Delay", 100, 100, 0, 2000, "MaxSoupDelay" to "MinSoupDelay")
    private val silentValue = BoolValue("Silent", true)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)
    private val bowlValue = ListValue("Bowl", arrayOf("Drop", "Move", "Stay"), "Drop")
    private val ignoreScreen = BoolValue("IgnoreScreen", true)

    private val inventoryGroup = ValueGroup("Inventory")
    private val inventoryDelayValue = IntegerRangeValue("Delay", 100, 200, 0, 5000, "MaxInvDelay" to "MinInvDelay")
    private val inventoryOpenInventoryValue = BoolValue("OpenInventory", false, "OpenInv")
    private val inventorySimulateInventoryValue = BoolValue("SimulateInventory", true, "SimulateInventory")
    private val inventoryNoMoveValue = BoolValue("NoMove", false, "NoMove")
    private val inventoryRandomSlotValue = BoolValue("RandomSlot", false, "RandomSlot")

    private val inventoryMisclickGroup = ValueGroup("ClickMistakes")
    private val inventoryMisclickEnabledValue = BoolValue("Enabled", false, "ClickMistakes")
    private val inventoryMisclickRateValue = IntegerValue("Rate", 5, 0, 100, "ClickMistakeRate")

    private val soupDelayTimer = MSTimer()
    private var soupDelay = delayValue.getRandomLong()

    private var invDelay = inventoryDelayValue.getRandomLong()

    private var soup = -1

    override val tag: String
        get() = "${healthValue.get()}"

    init
    {
        inventoryMisclickGroup.addAll(inventoryMisclickEnabledValue, inventoryMisclickRateValue)
        inventoryGroup.addAll(inventoryDelayValue, inventoryOpenInventoryValue, inventorySimulateInventoryValue, inventoryNoMoveValue, inventoryRandomSlotValue, inventoryMisclickGroup)
    }

    @EventTarget
    fun onMotion(motionEvent: MotionEvent)
    {
        val thePlayer = mc.thePlayer ?: return
        val netHandler = mc.netHandler
        val controller = mc.playerController
        val screen = mc.currentScreen

        val openContainer = thePlayer.openContainer
        val inventory = thePlayer.inventory
        val inventoryContainer = thePlayer.inventoryContainer

        val provider = classProvider

        val itemDelay = itemDelayValue.get().toLong()
        val random = inventoryRandomSlotValue.get()
        val handleBowl = bowlValue.get()

        when (motionEvent.eventState)
        {
            EventState.PRE ->
            {
                if (soupDelayTimer.hasTimePassed(soupDelay) && (ignoreScreen.get() || screen is GuiContainer))
                {
                    val soupInHotbar = inventoryContainer.findItem(36, 45, provider.getItemEnum(ItemType.MUSHROOM_STEW), itemDelay, random)

                    if (thePlayer.health <= healthValue.get() && soupInHotbar != -1)
                    {
                        soup = soupInHotbar

                        val soupInHotbarIndex = soupInHotbar - 36

                        if (silentValue.get())
                        {
                            if (!InventoryUtils.tryHoldSlot(thePlayer, soupInHotbarIndex, -1, true)) return
                        }
                        else
                        {
                            inventory.currentItem = soupInHotbarIndex
                            mc.playerController.updateController()
                        }
                        return
                    }
                }

                if (InventoryUtils.CLICK_TIMER.hasTimePassed(invDelay) && !(inventoryNoMoveValue.get() && thePlayer.isMoving) && !(openContainer != null && openContainer.windowId != 0))
                {
                    val bowl = provider.getItemEnum(ItemType.BOWL)

                    // Move empty bowls to inventory
                    val bowlInHotbar = inventoryContainer.findItem(36, 45, bowl, itemDelay, random)

                    val isGuiInventory = screen is GuiInventory
                    val simulateInv = inventorySimulateInventoryValue.get()

                    if (handleBowl.equals("Move", true) && bowlInHotbar != -1)
                    {
                        if (inventoryOpenInventoryValue.get() && !isGuiInventory) return

                        if ((9..36).map(inventory::getStackInSlot).any { it == null || it.item == bowl && it.stackSize < 64 })
                        {
                            val openInventory = !isGuiInventory && simulateInv

                            if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

                            controller.windowClick(0, bowlInHotbar, 0, 1, thePlayer)

                            invDelay = inventoryDelayValue.getRandomLong()
                            InventoryUtils.CLICK_TIMER.reset()

                            return
                        }
                    }

                    // Move soups to hotbar
                    var soupInInventory = inventoryContainer.findItem(9, 36, provider.getItemEnum(ItemType.MUSHROOM_STEW), itemDelay, random)

                    if (soupInInventory != -1 && inventory.hasSpaceHotbar)
                    {

                        // OpenInventory Check
                        if (inventoryOpenInventoryValue.get() && !isGuiInventory) return

                        // Simulate Click Mistakes to bypass some anti-cheats
                        if (inventoryMisclickEnabledValue.get() && inventoryMisclickRateValue.get() > 0 && Random.nextInt(100) <= inventoryMisclickRateValue.get())
                        {
                            val firstEmpty = inventoryContainer.firstEmpty(9, 36, random)
                            if (firstEmpty != -1) soupInInventory = firstEmpty
                        }

                        val openInventory = !isGuiInventory && simulateInv
                        if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

                        controller.windowClick(0, soupInInventory, 0, 1, thePlayer)

                        if (openInventory) netHandler.addToSendQueue(CPacketCloseWindow())

                        invDelay = inventoryDelayValue.getRandomLong()
                        InventoryUtils.CLICK_TIMER.reset()
                    }
                }
            }

            EventState.POST ->
            {
                if (soup >= 0)
                {
                    val itemStack = thePlayer.inventoryContainer.getSlot(soup).stack

                    if (itemStack != null)
                    {
                        netHandler.addToSendQueue(createUseItemPacket(itemStack, WEnumHand.MAIN_HAND))

                        if (handleBowl.equals("Drop", true)) netHandler.addToSendQueue(CPacketPlayerDigging(ICPacketPlayerDigging.WAction.DROP_ITEM, BlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))

                        if (silentValue.get()) InventoryUtils.resetSlot(thePlayer)

                        soupDelay = delayValue.getRandomLong()
                        soupDelayTimer.reset()
                    }

                    soup = -1
                }
            }
        }
    }
}
