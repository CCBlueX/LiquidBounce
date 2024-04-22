/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoArmor
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner.canBeSortedTo
import net.ccbluex.liquidbounce.features.module.modules.player.InventoryCleaner.isStackUseful
import net.ccbluex.liquidbounce.utils.CoroutineUtils.waitUntil
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.shuffled
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.canClickInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.countSpaceInInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.hasSpaceInInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRectNew
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.entity.EntityLiving.getArmorPosition
import net.minecraft.init.Blocks
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S30PacketWindowItems
import java.awt.Color

object ChestStealer : Module("ChestStealer", ModuleCategory.WORLD, hideModule = false) {

    private val maxDelay: Int by object : IntegerValue("MaxDelay", 50, 0..500) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minDelay)
    }
    private val minDelay by object : IntegerValue("MinDelay", 50, 0..500) {
        override fun isSupported() = maxDelay > 0

        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxDelay)
    }

    private val startDelay by IntegerValue("StartDelay", 50, 0..500)
    private val closeDelay by IntegerValue("CloseDelay", 50, 0..500)

    private val noMove by InventoryManager.noMoveValue
    private val noMoveAir by InventoryManager.noMoveAirValue
    private val noMoveGround by InventoryManager.noMoveGroundValue

    private val chestTitle by BoolValue("ChestTitle", true)

    private val randomSlot by BoolValue("RandomSlot", true)

    private val progressBar by BoolValue("ProgressBar", true, subjective = true)

    private var progress: Float? = null
        set(value) {
            field = value?.coerceIn(0f, 1f)

            if (field == null)
                easingProgress = 0f
        }

    private var easingProgress = 0f

    private var receivedId: Int? = null

    private var stacks = emptyList<ItemStack?>()

    private suspend fun shouldOperate(): Boolean {
        while (true) {
            if (!handleEvents())
                return false

            if (mc.playerController?.currentGameType?.isSurvivalOrAdventure != true)
                return false

            if (mc.currentScreen !is GuiChest)
                return false

            if (mc.thePlayer?.openContainer?.windowId != receivedId)
                return false

            // Wait till NoMove check isn't violated
            if (canClickInventory())
                return true

            // If NoMove is violated, wait a tick and check again
            // If there is no delay, very weird things happen: https://www.guilded.gg/CCBlueX/groups/1dgpg8Jz/channels/034be45e-1b72-4d5a-bee7-d6ba52ba1657/chat?messageId=94d314cd-6dc4-41c7-84a7-212c8ea1cc2a
            delay(50)
        }
    }

    suspend fun stealFromChest() {
        if (!handleEvents())
            return

        val thePlayer = mc.thePlayer ?: return

        val screen = mc.currentScreen ?: return

        if (screen !is GuiChest || !shouldOperate())
            return

        // Check if chest isn't a custom gui
        if (chestTitle && Blocks.chest.localizedName !in (screen.lowerChestInventory ?: return).name)
            return

        progress = 0f

        delay(startDelay.toLong())

        // Go through the chest multiple times, till there are no useful items anymore
        while (true) {
            if (!shouldOperate())
                return

            if (!hasSpaceInInventory())
                return

            var hasTaken = false

            val itemsToSteal = getItemsToSteal()

            run scheduler@ {
                itemsToSteal.forEachIndexed { index, (slot, stack, sortableTo) ->
                    // Wait for NoMove or cancel click
                    if (!shouldOperate()) {
                        TickScheduler += { serverSlot = thePlayer.inventory.currentItem }
                        return
                    }

                    if (!hasSpaceInInventory())
                        return@scheduler

                    hasTaken = true

                    // If target is sortable to a hotbar slot, steal and sort it at the same time, else shift + left-click
                    TickScheduler.scheduleClick(slot, sortableTo ?: 0, if (sortableTo != null) 2 else 1) {
                        progress = (index + 1) / itemsToSteal.size.toFloat()

                        if (!AutoArmor.canEquipFromChest())
                            return@scheduleClick

                        val item = stack.item

                        if (item !is ItemArmor || thePlayer.inventory.armorInventory[getArmorPosition(stack) - 1] != null)
                            return@scheduleClick

                        // TODO: should the stealing be suspended until the armor gets equipped and some delay on top of that, maybe toggleable?
                        // Try to equip armor piece from hotbar 1 tick after stealing it
                        TickScheduler += {
                            val hotbarStacks = thePlayer.inventory.mainInventory.take(9)

                            // Can't get index of stack instance, because it is different even from the one returned from windowClick()
                            val newIndex = hotbarStacks.indexOfFirst { it?.getIsItemStackEqual(stack) ?: false }

                            if (newIndex != -1)
                                AutoArmor.equipFromHotbarInChest(newIndex, stack)
                        }
                    }

                    delay(randomDelay(minDelay, maxDelay).toLong())
                }
            }

            // If no clicks were sent in the last loop stop searching
            if (!hasTaken) {
                progress = 1f
                delay(closeDelay.toLong())

                TickScheduler += { serverSlot = thePlayer.inventory.currentItem }
                break
            }

            // Wait till all scheduled clicks were sent
            waitUntil(TickScheduler::isEmpty)

            // Before closing the chest, check all items once more, whether server hadn't cancelled some of the actions.
            stacks = thePlayer.openContainer.inventory
        }

        // Wait before the chest gets closed (if it gets closed out of tick loop it could throw npe)
        TickScheduler.scheduleAndSuspend {
            thePlayer.closeScreen()
            progress = null
        }
    }

    private fun getItemsToSteal(): MutableList<Triple<Int, ItemStack, Int?>> {
        val sortBlacklist = BooleanArray(9)

        var spaceInInventory = countSpaceInInventory()

        return stacks.dropLast(36)
            .mapIndexedNotNull { index, stack ->
                stack ?: return@mapIndexedNotNull null

                if (index in TickScheduler) return@mapIndexedNotNull null

                val mergeableCount = mc.thePlayer.inventory.mainInventory.sumOf { otherStack ->
                    otherStack ?: return@sumOf 0

                    if (otherStack.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(stack, otherStack))
                        otherStack.maxStackSize - otherStack.stackSize
                    else 0
                }

                val canMerge = mergeableCount > 0
                val canFullyMerge = mergeableCount >= stack.stackSize

                // Clicking this item wouldn't take it from chest or merge it
                if (!canMerge && spaceInInventory <= 0) return@mapIndexedNotNull null

                // If stack can be merged without occupying any additional slot, do not take stack limits into account
                // TODO: player could theoretically already have too many stacks in inventory before opening the chest so no more should even get merged
                // TODO: if it can get merged but would also need another slot, it could simulate 2 clicks, one which maxes out the stack in inventory and second that puts excess items back
                if (InventoryCleaner.handleEvents() && !isStackUseful(stack, stacks, noLimits = canFullyMerge))
                    return@mapIndexedNotNull null

                var sortableTo: Int? = null

                // If stack can get merged, do not try to sort it, normal shift + left-click will merge it
                if (!canMerge && InventoryCleaner.handleEvents() && InventoryCleaner.sort) {
                    for (hotbarIndex in 0..8) {
                        if (sortBlacklist[hotbarIndex])
                            continue

                        if (!canBeSortedTo(hotbarIndex, stack.item))
                            continue

                        val hotbarStack = stacks.getOrNull(stacks.size - 9 + hotbarIndex)

                        // If occupied hotbar slot isn't already sorted or isn't strictly best, sort to it
                        if (!canBeSortedTo(hotbarIndex, hotbarStack?.item) || !isStackUseful(hotbarStack, stacks, strictlyBest = true)) {
                            sortableTo = hotbarIndex
                            sortBlacklist[hotbarIndex] = true
                            break
                        }
                    }
                }

                // If stack gets fully merged, no slot in inventory gets occupied
                if (!canFullyMerge) spaceInInventory--

                Triple(index, stack, sortableTo)
            }.shuffled(randomSlot)

            // Prioritise armor pieces with lower priority, so that as many pieces can get equipped from hotbar after chest gets closed
            .sortedByDescending { it.second.item is ItemArmor }

            // Prioritize items that can be sorted
            .sortedByDescending { it.third != null }

            .toMutableList()
            .also {
                // Fully prioritise armor pieces when it is possible to equip armor while in chest
                if (AutoArmor.canEquipFromChest())
                    it.sortByDescending { it.second.item is ItemArmor }
            }
    }


    // Progress bar
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!progressBar || mc.currentScreen !is GuiChest)
            return

        val progress = progress ?: return

        val (scaledWidth, scaledHeight) = ScaledResolution(mc)

        val minX = scaledWidth * 0.3f
        val maxX = scaledWidth * 0.7f
        val minY = scaledHeight * 0.75f
        val maxY = minY + 10f

        easingProgress += (progress - easingProgress) / 6f * event.partialTicks

        drawRectNew(minX - 2, minY - 2, maxX + 2, maxY + 2, Color(200, 200, 200).rgb)
        drawRectNew(minX, minY, maxX, maxY, Color(50, 50, 50).rgb)
        drawRectNew(minX, minY, minX + (maxX - minX) * easingProgress, maxY, Color.HSBtoRGB(easingProgress / 5, 1f, 1f) or 0xFF0000)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        when (val packet = event.packet) {
            is C0DPacketCloseWindow, is S2DPacketOpenWindow, is S2EPacketCloseWindow -> {
                receivedId = null
                progress = null
            }
            is S30PacketWindowItems -> {
                // Chests never have windowId 0
                if (packet.func_148911_c() == 0)
                    return

                receivedId = packet.func_148911_c()

                stacks = packet.itemStacks.toList()
            }
        }
    }
}