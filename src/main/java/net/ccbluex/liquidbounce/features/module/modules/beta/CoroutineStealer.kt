/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.features.module.modules.beta

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.beta.CoroutineCleaner.canBeSortedTo
import net.ccbluex.liquidbounce.features.module.modules.beta.CoroutineCleaner.isStackUseful
import net.ccbluex.liquidbounce.features.module.modules.beta.CoroutineCleaner.toHotbarIndex
import net.ccbluex.liquidbounce.utils.CoroutineUtils.waitUntil
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.shuffled
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager.canClickInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.hasSpaceInInventory
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.ItemArmor
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0DPacketCloseWindow
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraft.network.play.server.S2EPacketCloseWindow
import net.minecraft.network.play.server.S30PacketWindowItems
import java.awt.Color

object CoroutineStealer : Module("CoroutineStealer", ModuleCategory.BETA) {

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

    private val noCompass by BoolValue("NoCompass", true)

    private val chestTitle by BoolValue("ChestTitle", true)

    private val randomSlot by BoolValue("RandomSlot", true)

    private val progressBar by BoolValue("ProgressBar", true)

    private var progress: Float? = null
        set(value) {
            field = value?.coerceIn(0f, 1f)

            if (field == null)
                easingProgress = 0f
        }

    private var easingProgress = 0f

    private var receivedId: Int? = null

    private var stacks = emptyList<ItemStack?>()

    private suspend fun shouldExecute(): Boolean {
        while (true) {
            if (!state)
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
        if (!state)
            return

        val thePlayer = mc.thePlayer ?: return

        val screen = mc.currentScreen ?: return

        if (screen !is GuiChest || !shouldExecute())
            return

        // Check if player isn't holding a compass and browsing navigation gui
        if (noCompass && thePlayer.heldItem?.item == Items.compass)
            return

        // Check if chest isn't a custom gui
        if (chestTitle && Blocks.chest.localizedName !in (screen.lowerChestInventory ?: return).name)
            return

        progress = 0f

        delay(startDelay.toLong())

        // Go through the chest multiple times, till there are no useful items anymore
        while (true) {
            if (!shouldExecute())
                return

            val sortBlacklist = BooleanArray(9)

            val usefulItems = stacks.dropLast(36)
                .mapIndexedNotNull { index, stack ->
                    stack ?: return@mapIndexedNotNull null

                    if (index in TickScheduler || (CoroutineCleaner.state && !isStackUseful(stack, stacks)))
                        return@mapIndexedNotNull null

                    var sortableTo: Int? = null

                    if (CoroutineCleaner.state && CoroutineCleaner.sort) {
                        for (hotbarIndex in 0..8) {
                            if (sortBlacklist[hotbarIndex])
                                continue

                            if (!canBeSortedTo(hotbarIndex, stack.item))
                                continue

                            val hotbarStack = stacks.getOrNull(stacks.size - 9 + hotbarIndex)

                            if (!isStackUseful(hotbarStack, stacks)) {
                                sortableTo = hotbarIndex
                                sortBlacklist[hotbarIndex] = true
                                break
                            }
                        }
                    }

                    Triple(index, stack, sortableTo)
                }.shuffled(randomSlot)

                // Prioritize items that can be sorted (so that as many items could be instantly sorted)
                // Explanation: If a non-sortable item goes before the sortable, it gets shift-clicked to some hotbar slot, which is then occupied with useful item that shouldn't get swapped by sorting...
                // (sortable items would have to be normally shift-clicked afterward)
                .sortedByDescending { it.third != null }
                .toMutableList()

            // TODO: Might block sortable items from getting instantly sorted, armor having lower priority might be better afterwards...
            // TODO: Or it could wait for armor to get equipped... Or it could expect which slot the armor would go to...
            // Also prioritize armor pieces so that they could be equipped while in chest, from hotbar.
            if (CoroutineArmorer.state && CoroutineArmorer.hotbar && !CoroutineArmorer.onlyWhenNoScreen)
                usefulItems.sortByDescending { it.second.item is ItemArmor }

            var hasTaken = false

            run scheduler@ {
                usefulItems.forEachIndexed { index, (slot, stack, sortableTo) ->
                    if (!shouldExecute()) {
                        TickScheduler += { serverSlot = thePlayer.inventory.currentItem }
                        return
                    }

                    if (!hasSpaceInInventory()) {
                        // Check if the chest has any empty slot
                        val hasChestEmptySlot = stacks.dropLast(36).any { it == null }

                        if (!hasChestEmptySlot)
                            return@scheduler

                        // If the item is supposed to be sorted, put the stack that occupies its slot into chest, else find first garbage item
                        var indexToMoveToChest = sortableTo?.plus(stacks.size - 9)

                        if (indexToMoveToChest == null) {
                            val garbageInventoryIndex = stacks.takeLast(36)
                                .indexOfLast { CoroutineCleaner.state && !isStackUseful(it, stacks) }

                            if (garbageInventoryIndex != -1)
                                indexToMoveToChest = stacks.size - 36 + garbageInventoryIndex
                        }

                        indexToMoveToChest ?: return@scheduler

                        // Shift + left-click bad item from inventory into chest to free up space
                        TickScheduler.scheduleClick(indexToMoveToChest, 0, 1)

                        delay(randomDelay(minDelay, maxDelay).toLong())
                    }

                    hasTaken = true

                    // TODO: If armor can be equipped from hotbar, but all slots are occupied, sort it to slot with useless item or not sorted item and equip (if the item was useful or sorted, sort it back in the end)

                    // TODO: might schedule clicks that exceed inventory space at low delays, it will notice that it doesn't have space in inventory next tick, when the scheduled click gets executed
                    // If target is sortable to a hotbar slot, steal and sort it at the same time, else shift + left-click
                    TickScheduler.scheduleClick(slot, sortableTo ?: 0, if (sortableTo != null) 2 else 1) {
                        progress = (index + 1) / usefulItems.size.toFloat()

                        // Try to equip armor piece from hotbar 1 tick after stealing it
                        if (stack.item is ItemArmor) {
                            TickScheduler += {
                                val stacks = thePlayer.openContainer.inventory

                                // Can't get index of stack instance, because it is different even from the one returned from windowClick()
                                stacks.indexOfLast { it?.getIsItemStackEqual(stack) ?: false }.toHotbarIndex(stacks.size)?.let {
                                    CoroutineArmorer.equipFromHotbarInChest(it, stack)
                                }
                            }
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
            waitUntil { TickScheduler.isEmpty() }

            // Before closing the chest, check all items once more, whether server hadn't cancelled some of the actions.
            stacks = thePlayer.openContainer.inventory
        }

        // Wait before the chest gets closed (if it gets closed out of tick loop it could throw npe)
        TickScheduler.scheduleAndSuspend({
            thePlayer.closeScreen()
            progress = null
        })
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

        drawRect(minX - 2, minY - 2, maxX + 2, maxY + 2, Color(200, 200, 200).rgb)
        drawRect(minX, minY, maxX, maxY, Color(50, 50, 50).rgb)
        drawRect(minX, minY, minX + (maxX - minX) * easingProgress, maxY, Color.HSBtoRGB(easingProgress / 5, 1f, 1f) or 0xFF0000)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        when (packet) {
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