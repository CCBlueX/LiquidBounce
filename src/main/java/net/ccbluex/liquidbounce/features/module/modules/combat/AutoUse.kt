/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.FastUse
import net.ccbluex.liquidbounce.features.module.modules.player.Zoot
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.inventory.Container
import net.minecraft.item.Item
import net.minecraft.item.ItemGlassBottle
import net.minecraft.item.ItemPotion
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.potion.PotionEffect
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import kotlin.random.Random

@ModuleInfo(name = "AutoUse", description = "Automatically eat/drink foods/potions in your hotbar (WARNING: WIP)", category = ModuleCategory.COMBAT)
class AutoUse : Module()
{
    private val foodGroup = ValueGroup("Food")
    private val foodEnabledValue = BoolValue("Enabled", true, "Food")
    private val foodLevelValue = IntegerValue("Level", 18, 1, 20, "FoodLevel")

    private val potionValue = BoolValue("Potion", true)

    private val gappleGroup = ValueGroup("GApple")
    private val gappleEnabledValue = BoolValue("Enabled", true, "Gapple")
    private val gappleHealthValue = FloatValue("Health", 12F, 1F, 20F, "Gapple-Health")

    private val milkValue = BoolValue("Milk", true)

    private val silentValue = BoolValue("Silent", false)

    private val offset = IntegerValue("Offset", 1, 0, 10)

    private val delayValue = IntegerRangeValue("Delay", 2000, 2000, 0, 2000, "MaxUseDelay" to "MinUseDelay")
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

    private val glassBottleValue = ListValue("GlassBottle", arrayOf("Drop", "Move", "Stay"), "Drop")

    private val ignoreScreen = BoolValue("IgnoreScreen", true)

    private val inventoryGroup = ValueGroup("Inventory")
    private val inventoryDelayValue = IntegerRangeValue("Delay", 100, 200, 0, 2000, "MaxInvDelay" to "MinInvDelay")
    private val inventoryOpenInventoryValue = BoolValue("OpenInventory", false, "OpenInv")
    private val inventorySimulateInventoryValue = BoolValue("SimulateInventory", true, "SimulateInventory")
    private val inventoryNoMoveValue = BoolValue("NoMove", false, "NoMove")
    private val inventoryRandomSlotValue = BoolValue("RandomSlot", false, "RandomSlot")

    private val inventoryMisclickGroup = ValueGroup("ClickMistakes")
    private val inventoryMisclickEnabledValue = BoolValue("Enabled", false, "ClickMistakes")
    private val inventoryMisclickRateValue = IntegerValue("Rate", 5, 0, 100, "ClickMistakeRate")

    private val killAuraBypassGroup = ValueGroup("KillAuraBypass")
    private val killauraBypassModeValue = ListValue("Mode", arrayOf("None", "SuspendKillAura", "WaitForKillAuraEnd"), "SuspendKillAura", "KillAuraBypassMode")
    private val killAuraBypassKillAuraSuspendDurationValue = object : IntegerValue("Duration", 300, 100, 1000, "SuspendKillAuraDuration")
    {
        override fun showCondition() = killauraBypassModeValue.get().equals("SuspendKillAura", ignoreCase = true)
    }

    private val useDelayTimer = MSTimer()
    private var useDelay = delayValue.getRandomLong()

    private var invDelay = inventoryDelayValue.getRandomLong()

    private var slotToUse = -1

    private var lastRequiredTicks: Int? = null
    private var waitedTicks = -1

    init
    {
        foodGroup.addAll(foodEnabledValue, foodLevelValue)

        gappleGroup.addAll(gappleEnabledValue, gappleHealthValue)

        inventoryMisclickGroup.addAll(inventoryMisclickEnabledValue, inventoryMisclickRateValue)
        inventoryGroup.addAll(inventoryDelayValue, inventoryOpenInventoryValue, inventorySimulateInventoryValue, inventoryNoMoveValue, inventoryRandomSlotValue, inventoryMisclickGroup)

        killAuraBypassGroup.addAll(killauraBypassModeValue, killAuraBypassKillAuraSuspendDurationValue)
    }

    override fun onDisable()
    {
        endEating(mc.thePlayer ?: return, glassBottleValue.get(), silentValue.get())

        slotToUse = -1
        waitedTicks = -1
    }

    @EventTarget
    fun onMotion(motionEvent: MotionEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        val controller = mc.playerController
        if (controller.isInCreativeMode) return

        val killAura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura
        if (killauraBypassModeValue.get().equals("WaitForKillAuraEnd", true) && killAura.state && killAura.target != null) return

        val netHandler = mc.netHandler
        val screen = mc.currentScreen

        val openContainer = thePlayer.openContainer
        val inventory = thePlayer.inventory
        val inventoryContainer = thePlayer.inventoryContainer
        val activePotionEffects = thePlayer.activePotionEffects
        val health = thePlayer.health
        val foodLevel = thePlayer.foodStats.foodLevel

        val food = foodEnabledValue.get()
        val potion = potionValue.get()
        val gapple = gappleEnabledValue.get()
        val milk = milkValue.get()

        val gappleHealth = gappleHealthValue.get()

        val itemDelay = itemDelayValue.get().toLong()
        val random = inventoryRandomSlotValue.get()
        val handleGlassBottle = glassBottleValue.get()
        val silent = silentValue.get()

        when (motionEvent.eventState)
        {
            EventState.PRE ->
            {
                if (useDelayTimer.hasTimePassed(useDelay) && (ignoreScreen.get() || screen is GuiContainer))
                {
                    val foodInHotbar = if (food && foodLevel <= foodLevelValue.get()) inventoryContainer.findBestFood(foodLevel, itemDelay = itemDelay) else -1
                    val potionInHotbar = if (potion) findPotion(activePotionEffects, inventoryContainer, random = random, itemDelay = itemDelay) else -1
                    val gappleInHotbar = if (gapple && if (gappleHealth < 20) health <= gappleHealth else thePlayer.absorptionAmount <= 0) inventoryContainer.findItem(36, 45, Items.golden_apple, itemDelay, random) else -1
                    val milkInHotbar = if (milk && activePotionEffects.map(PotionEffect::getPotionID).any(Zoot.badEffectsArray::contains)) inventoryContainer.findItem(36, 45, Items.milk_bucket, itemDelay, random) else -1

                    val slot = arrayOf(foodInHotbar, potionInHotbar, gappleInHotbar, milkInHotbar).firstOrNull { it != -1 }

                    if (slot != null)
                    {
                        slotToUse = slot

                        val slotIndex = slotToUse - 36

                        if (!silent) inventory.currentItem = slotIndex

                        val isFirst = waitedTicks <= 0

                        if (isFirst) if (silent)
                        {
                            if (!InventoryUtils.tryHoldSlot(thePlayer, slotIndex, -1, slot == gappleInHotbar)) return
                        }
                        else mc.playerController.updateController()

                        val stack = inventoryContainer.getSlot(slotToUse).stack

                        if (isFirst) netHandler.addToSendQueue(createUseItemPacket(stack))

                        // Suspend killaura if option is present
                        if (killauraBypassModeValue.get().equals("SuspendKillAura", true)) killAura.suspend(killAuraBypassKillAuraSuspendDurationValue.get().toLong())

                        if (silent)
                        {
                            val itemUseTicks = if (isFirst) 0 else (lastRequiredTicks?.minus(waitedTicks)) ?: 0
                            lastRequiredTicks = performFastUse(thePlayer, stack?.item, itemUseTicks) + offset.get()

                            if (isFirst) waitedTicks = lastRequiredTicks!!
                        }
                        else
                        {
                            lastRequiredTicks = 32 // FIXME

                            mc.gameSettings.keyBindUseItem.pressed = true // FIXME: Change to better solution

                            if (isFirst) waitedTicks = lastRequiredTicks!!
                        }

                        return
                    }
                }
                else endEating(thePlayer)

                if (InventoryUtils.CLICK_TIMER.hasTimePassed(invDelay) && !(inventoryNoMoveValue.get() && thePlayer.isMoving) && !(openContainer != null && openContainer.windowId != 0))
                {
                    val glassBottle = Items.glass_bottle

                    // Move empty glass bottles to inventory
                    val glassBottleInHotbar = inventoryContainer.findItem(36, 45, glassBottle, itemDelay, random)

                    val isGuiInventory = screen is GuiInventory
                    val simulateInv = inventorySimulateInventoryValue.get()

                    if (handleGlassBottle.equals("Move", true) && glassBottleInHotbar != -1)
                    {
                        if (inventoryOpenInventoryValue.get() && !isGuiInventory) return

                        if ((9..36).map(inventory::getStackInSlot).any { it == null || it.item == glassBottle && it.stackSize < 16 })
                        {
                            val openInventory = !isGuiInventory && simulateInv

                            if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

                            controller.windowClick(0, glassBottleInHotbar, 0, 1, thePlayer)

                            invDelay = inventoryDelayValue.getRandomLong()
                            InventoryUtils.CLICK_TIMER.reset()

                            return
                        }
                    }

                    // Move foods to hotbar
                    val foodInInventory = if (food && foodLevel <= foodLevelValue.get()) inventoryContainer.findBestFood(foodLevel, startSlot = 9, endSlot = 36, itemDelay = itemDelay) else -1
                    val potionInInventory = if (potion) findPotion(activePotionEffects, inventoryContainer, startSlot = 9, endSlot = 36, random = random, itemDelay = itemDelay) else -1
                    // TODO: Enchanted golden apple preference
                    val gappleInInventory = if (gapple && if (gappleHealth < 20) health <= gappleHealth else thePlayer.absorptionAmount <= 0) inventoryContainer.findItem(9, 36, Items.golden_apple, itemDelay, random) else -1
                    val milkInInventory = if (milk && activePotionEffects.map(PotionEffect::getPotionID).any(Zoot.badEffectsArray::contains)) inventoryContainer.findItem(9, 36, Items.milk_bucket, itemDelay, random) else -1

                    var slot = arrayOf(foodInInventory, potionInInventory, gappleInInventory, milkInInventory).firstOrNull { it != -1 }

                    if (slot != null && inventory.hasSpaceHotbar)
                    {
                        // OpenInventory Check
                        if (inventoryOpenInventoryValue.get() && !isGuiInventory) return

                        // Simulate Click Mistakes to bypass some anti-cheats
                        if (inventoryMisclickEnabledValue.get() && inventoryMisclickRateValue.get() > 0 && Random.nextInt(100) <= inventoryMisclickRateValue.get())
                        {
                            val firstEmpty = inventoryContainer.firstEmpty(9, 36, random)
                            if (firstEmpty != -1) slot = firstEmpty
                        }

                        val openInventory = !isGuiInventory && simulateInv
                        if (openInventory) netHandler.addToSendQueue(createOpenInventoryPacket())

                        controller.windowClick(0, slot, 0, 1, thePlayer)

                        if (openInventory) netHandler.addToSendQueue(C0DPacketCloseWindow())

                        invDelay = inventoryDelayValue.getRandomLong()
                        InventoryUtils.CLICK_TIMER.reset()
                    }
                }
            }

            EventState.POST -> if (slotToUse >= 0)
            {
                waitedTicks = (waitedTicks - 1).coerceAtLeast(-1)
                if (waitedTicks <= 0) endEating(thePlayer, handleGlassBottle, silent, wasSuccessful = true)
            }
        }
    }

    fun endEating(thePlayer: EntityPlayer, handleGlassBottle: String = glassBottleValue.get(), silent: Boolean = silentValue.get(), wasSuccessful: Boolean = false)
    {
        if (slotToUse == -1) return

        val itemStack = thePlayer.inventoryContainer.getSlot(slotToUse).stack
        slotToUse = -1 // Reset slot

        if (itemStack != null && wasSuccessful)
        {
            if (handleGlassBottle.equals("Drop", true) && (itemStack.item is ItemGlassBottle || itemStack.item is ItemPotion && !ItemPotion.isSplash(itemStack.metadata))) mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.DROP_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))

            useDelay = delayValue.getRandomLong()
            useDelayTimer.reset()
        }

        val gameSettings = mc.gameSettings
        if (silent) InventoryUtils.resetSlot(thePlayer) else if (!GameSettings.isKeyDown(gameSettings.keyBindUseItem)) gameSettings.keyBindUseItem.unpressKey()
    }

    private fun performFastUse(thePlayer: EntityPlayerSP, item: Item?, itemUseTicks: Int): Int = (LiquidBounce.moduleManager[FastUse::class.java] as FastUse).perform(thePlayer, mc.timer, item, itemUseTicks)

    private fun findPotion(activePotionEffects: Collection<PotionEffect>, inventoryContainer: Container, startSlot: Int = 36, endSlot: Int = 45, random: Boolean, itemDelay: Long): Int = (LiquidBounce.moduleManager[AutoPot::class.java] as AutoPot).findBuffPotion(activePotionEffects, startSlot, endSlot, inventoryContainer, random, false, itemDelay) // TODO: findHealPotion() support

    override val tag: String?
        get() = if (silentValue.get()) "Silent" else null
}
