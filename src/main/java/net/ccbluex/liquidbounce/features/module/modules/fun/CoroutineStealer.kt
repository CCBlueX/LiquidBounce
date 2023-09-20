/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.features.module.modules.`fun`

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.`fun`.CoroutineCleaner.canBeSortedTo
import net.ccbluex.liquidbounce.features.module.modules.`fun`.CoroutineCleaner.isStackUseful
import net.ccbluex.liquidbounce.features.module.modules.movement.InventoryMove
import net.ccbluex.liquidbounce.utils.InventoryUtils.hasSpaceInInventory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.utils.extensions.shuffled
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.timer.TimeUtils.randomDelay
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.init.Blocks
import net.minecraft.init.Items
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

    private val noMove by BoolValue("NoMoveClicks", false)
    private val noMoveAir by BoolValue("NoClicksInAir", false) { noMove }
    private val noMoveGround by BoolValue("NoClicksOnGround", true) { noMove }

    private val noCompass by BoolValue("NoCompass", true)

    private val chestTitle by BoolValue("ChestTitle", true)

    private val takeRandomized by BoolValue("TakeRandomized", true)

    // private val silent by BoolValue("Silent", false)

    private val progressBar by BoolValue("ProgressBar", true)

    private var progress = 0f
        set(value) {
            field = value.coerceIn(0f, 1f)
        }

    private var easingProgress = 0f

    private var receivedId = 0

    private var stacks = emptyList<ItemStack?>()

    private suspend fun shouldExecute(): Boolean {
        while (true) {
            if (mc.currentScreen !is GuiChest)
                return false

            if (mc.thePlayer?.openContainer?.windowId != receivedId)
                return false

            // Wait till NoMove check isn't violated
            if (InventoryMove.canClickInventory() && !(noMove && isMoving && if (mc.thePlayer.onGround) noMoveGround else noMoveAir))
                return true

            // If NoMove is violated, wait a tick and check again
            // If there is no delay, very weird things happen: https://www.guilded.gg/CCBlueX/groups/1dgpg8Jz/channels/034be45e-1b72-4d5a-bee7-d6ba52ba1657/chat?messageId=94d314cd-6dc4-41c7-84a7-212c8ea1cc2a
            delay(50)
        }
    }

    suspend fun execute() {
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

                            val hotbarStack = stacks[stacks.size - 9 + hotbarIndex]

                            if (!isStackUseful(hotbarStack, stacks)) {
                                sortableTo = hotbarIndex
                                sortBlacklist[hotbarIndex] = true
                                break
                            }
                        }
                    }

                    Triple(index, stack, sortableTo)
                }.shuffled(takeRandomized)

                // Prioritize items that can be sorted (so that as many items could be instantly sorted)
                // Explanation: If a non-sortable item goes before the sortable, it gets shift-clicked to some hotbar slot, which is then occupied with useful item that shouldn't get swapped by sorting...
                // (sortable items would have to be normally shift-clicked afterward)
                .sortedBy { it.third == null }

            run scheduler@ {
                usefulItems.forEachIndexed { index, (slot, stack, sortableTo) ->
                    if (!shouldExecute()) {
                        progress = 0f
                        easingProgress = 0f
                        return
                    }

                    // TODO: might schedule clicks that exceed inventory space at low delays, it will notice that it doesn't have space in inventory next tick, when the scheduled click gets executed
                    // When stealing items by instantly sorting them, you don't need any space in inventory, yay
                    if (sortableTo == null && !hasSpaceInInventory())
                        return@scheduler

                    TickScheduler.scheduleClick(slot, sortableTo ?: 0, if (sortableTo != null) 2 else 1) {
                        progress = index / usefulItems.lastIndex.toFloat()
                    }

                    delay(randomDelay(minDelay, maxDelay).toLong())
                }
            }

            // If no clicks were sent in the last loop
            if (TickScheduler.isEmpty()) {
                delay(closeDelay.toLong())

                break
            }

            // Wait till all scheduled clicks were sent
            while (!TickScheduler.isEmpty()) {}

            // Before closing the chest, check all items once more, whether server hadn't cancelled some of the actions.
            stacks = thePlayer.openContainer.inventory
        }

        progress = 1f

        TickScheduler += {
            mc.thePlayer.closeScreen()

            progress = 0f
            easingProgress = 0f
        }

        // Wait before the chest gets closed (if it gets closed out of tick loop it could throw npe)
        while (!TickScheduler.isEmpty()) {}
    }

    // Progress bar
    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        if (!progressBar || progress == 0f)
            return

        val (scaledWidth, scaledHeight) = ScaledResolution(mc)

        val minX = scaledWidth * 0.3f
        val maxX = scaledWidth * 0.7f
        val minY = scaledHeight * 0.75f
        val maxY = minY + 10f

        easingProgress += (progress - easingProgress) / 6f * event.partialTicks

        drawRect(minX - 2, minY - 2, maxX + 2, maxY + 2, Color(200, 200, 200).rgb)
        drawRect(minX, minY, maxX, maxY, Color(50, 50, 50).rgb)
        drawRect(
            minX,
            minY,
            minX + (maxX - minX) * easingProgress,
            maxY,
            Color.HSBtoRGB(easingProgress / 5, 1f, 1f) or 0xFF0000
        )
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        when (packet) {
            is C0DPacketCloseWindow, is S2DPacketOpenWindow, is S2EPacketCloseWindow -> receivedId = 0
            is S30PacketWindowItems -> {
                receivedId = packet.func_148911_c()

                stacks = packet.itemStacks.toList()
            }
        }
    }
}