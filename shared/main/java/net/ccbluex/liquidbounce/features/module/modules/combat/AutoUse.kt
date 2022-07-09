/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.IClassProvider
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.ItemType
import net.ccbluex.liquidbounce.api.enums.WEnumHand
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.network.IINetHandlerPlayClient
import net.ccbluex.liquidbounce.api.minecraft.inventory.IContainer
import net.ccbluex.liquidbounce.api.minecraft.item.IItem
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.potion.IPotionEffect
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.player.FastUse
import net.ccbluex.liquidbounce.features.module.modules.player.Zoot
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.createOpenInventoryPacket
import net.ccbluex.liquidbounce.utils.createUseItemPacket
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
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
        endEating(mc.thePlayer ?: return, classProvider, mc.netHandler, glassBottleValue.get(), silentValue.get())

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

        val provider = classProvider

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
                if (useDelayTimer.hasTimePassed(useDelay) && (ignoreScreen.get() || provider.isGuiContainer(screen)))
                {
                    val foodInHotbar = if (food && foodLevel <= foodLevelValue.get()) inventoryContainer.findBestFood(foodLevel, itemDelay = itemDelay) else -1
                    val potionInHotbar = if (potion) findPotion(activePotionEffects, inventoryContainer, random = random, itemDelay = itemDelay) else -1
                    val gappleInHotbar = if (gapple && if (gappleHealth < 20) health <= gappleHealth else thePlayer.absorptionAmount <= 0) inventoryContainer.findItem(36, 45, provider.getItemEnum(ItemType.GOLDEN_APPLE), itemDelay, random) else -1
                    val milkInHotbar = if (milk && activePotionEffects.map(IPotionEffect::potionID).any(Zoot.badEffectsArray::contains)) inventoryContainer.findItem(36, 45, provider.getItemEnum(ItemType.MILK_BUCKET), itemDelay, random) else -1

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

                        if (isFirst) netHandler.addToSendQueue(createUseItemPacket(stack, WEnumHand.MAIN_HAND))

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
                else endEating(thePlayer, provider, netHandler)

                if (InventoryUtils.CLICK_TIMER.hasTimePassed(invDelay) && !(inventoryNoMoveValue.get() && thePlayer.isMoving) && !(openContainer != null && openContainer.windowId != 0))
                {
                    val glassBottle = provider.getItemEnum(ItemType.GLASS_BOTTLE)

                    // Move empty glass bottles to inventory
                    val glassBottleInHotbar = inventoryContainer.findItem(36, 45, glassBottle, itemDelay, random)

                    val isGuiInventory = provider.isGuiInventory(screen)
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
                    val gappleInInventory = if (gapple && if (gappleHealth < 20) health <= gappleHealth else thePlayer.absorptionAmount <= 0) inventoryContainer.findItem(9, 36, provider.getItemEnum(ItemType.GOLDEN_APPLE), itemDelay, random) else -1
                    val milkInInventory = if (milk && activePotionEffects.map(IPotionEffect::potionID).any(Zoot.badEffectsArray::contains)) inventoryContainer.findItem(9, 36, provider.getItemEnum(ItemType.MILK_BUCKET), itemDelay, random) else -1

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

                        if (openInventory) netHandler.addToSendQueue(provider.createCPacketCloseWindow())

                        invDelay = inventoryDelayValue.getRandomLong()
                        InventoryUtils.CLICK_TIMER.reset()
                    }
                }
            }

            EventState.POST -> if (slotToUse >= 0)
            {
                waitedTicks = (waitedTicks - 1).coerceAtLeast(-1)
                if (waitedTicks <= 0) endEating(thePlayer, provider, netHandler, handleGlassBottle, silent, wasSuccessful = true)
            }
        }
    }

    fun endEating(thePlayer: IEntityPlayer, provider: IClassProvider, netHandler: IINetHandlerPlayClient, handleGlassBottle: String = glassBottleValue.get(), silent: Boolean = silentValue.get(), wasSuccessful: Boolean = false)
    {
        if (slotToUse == -1) return

        val itemStack = thePlayer.inventoryContainer.getSlot(slotToUse).stack
        slotToUse = -1 // Reset slot

        if (itemStack != null && wasSuccessful)
        {
            if (handleGlassBottle.equals("Drop", true) && (provider.isItemGlassBottle(itemStack.item) || provider.isItemPotion(itemStack.item) && !itemStack.isSplash())) netHandler.addToSendQueue(provider.createCPacketPlayerDigging(ICPacketPlayerDigging.WAction.DROP_ITEM, WBlockPos.ORIGIN, provider.getEnumFacing(EnumFacingType.DOWN)))

            useDelay = delayValue.getRandomLong()
            useDelayTimer.reset()
        }

        val gameSettings = mc.gameSettings
        if (silent) InventoryUtils.resetSlot(thePlayer) else if (!gameSettings.isKeyDown(gameSettings.keyBindUseItem)) gameSettings.keyBindUseItem.unpressKey()
    }

    private fun performFastUse(thePlayer: IEntityPlayerSP, item: IItem?, itemUseTicks: Int): Int = (LiquidBounce.moduleManager[FastUse::class.java] as FastUse).perform(thePlayer, mc.timer, item, itemUseTicks)

    private fun findPotion(activePotionEffects: Collection<IPotionEffect>, inventoryContainer: IContainer, startSlot: Int = 36, endSlot: Int = 45, random: Boolean, itemDelay: Long): Int = (LiquidBounce.moduleManager[AutoPot::class.java] as AutoPot).findBuffPotion(activePotionEffects, startSlot, endSlot, inventoryContainer, random, false, itemDelay) // TODO: findHealPotion() support

    override val tag: String?
        get() = if (silentValue.get()) "Silent" else null
}
