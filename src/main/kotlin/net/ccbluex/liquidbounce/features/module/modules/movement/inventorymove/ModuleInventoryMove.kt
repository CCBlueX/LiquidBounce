/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove

import it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveBlinkFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveSneakControlFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveSprintControlFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveTimerFeature
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.closeInventorySilently
import net.ccbluex.liquidbounce.utils.inventory.isInInventoryScreen
import net.minecraft.client.gui.screen.ChatScreen
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.option.KeyBinding
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.item.ItemGroups
import org.lwjgl.glfw.GLFW

/**
 * InventoryMove module
 *
 * Allows you to walk while an inventory is opened.
 */

object ModuleInventoryMove : ClientModule("InventoryMove", Category.MOVEMENT) {

    private val behavior by enumChoice("Behavior", Behaviour.NORMAL)

    @Suppress("unused")
    enum class Behaviour(override val choiceName: String) : NamedChoice {
        NORMAL("Normal"),
        SAFE("Safe"), // disable clicks while moving
        UNDETECTABLE("Undetectable"), // stop in inventory
    }

    private val passthroughSneak by boolean("PassthroughSneak", false)
    private val saveC0E by boolean("SaveC0E", false)
    private val noSprintWhenClosed by boolean("NoSprintWhenClosed", false)
    private val clickWindowList = ArrayDeque<ClickSlotC2SPacket>()
    private var isHandlingClosePacket = false
    // states of movement keys, using mc.options.<key>.isPressed doesn't work for some reason
    private val movementKeys = Reference2BooleanArrayMap<KeyBinding>(
        mc.options.run {
            arrayOf(forwardKey, leftKey, backKey, rightKey, jumpKey, sneakKey)
        },
        BooleanArray(6),
        6
    )

    /**
     * Restricts user from clicking while moving in inventory.
     */
    val doNotAllowClicking
        get() = behavior == Behaviour.SAFE && movementKeys.any { (key, pressed) ->
            pressed && shouldHandleInputs(key)
        }

    init {
        tree(InventoryMoveSprintControlFeature)
        tree(InventoryMoveSneakControlFeature)
        tree(InventoryMoveTimerFeature)
        tree(InventoryMoveBlinkFeature)
    }

    fun shouldHandleInputs(keyBinding: KeyBinding): Boolean {
        val screen = mc.currentScreen ?: return true

        if (!running || screen is ChatScreen || screen.isInCreativeSearchField() || ModuleClickGui.isInSearchBar) {
            return false
        }

        if (keyBinding == mc.options.sneakKey && !passthroughSneak) {
            return false
        }

        // If we are in a handled screen, we should handle the inputs only if the undetectable option is not enabled
        return behavior == Behaviour.NORMAL || screen !is HandledScreen<*>
            || behavior == Behaviour.SAFE && screen is InventoryScreen
    }
    @Suppress("unused")
    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        val player = mc.player ?: return@handler
        val screen = mc.currentScreen ?: return@handler

        if (!saveC0E) return@handler

        if (noSprintWhenClosed) {
            if (clickWindowList.isNotEmpty() && screen !is InventoryScreen) {
                player.isSprinting = false
            }
            if (packet is CloseHandledScreenC2SPacket) {
                if (isHandlingClosePacket) return@handler
                isHandlingClosePacket = true
                event.cancelEvent()
                player.isSprinting = false
                mc.networkHandler?.sendPacket(CloseHandledScreenC2SPacket(packet.syncId))
                isHandlingClosePacket = false
            }
        }

        if (screen is InventoryScreen) {
            if (packet is ClickSlotC2SPacket) {
                clickWindowList.add(packet)
                event.cancelEvent()
            }
        } else {

            if (clickWindowList.isNotEmpty()) {
                val packetsToSend = clickWindowList.toList()
                clickWindowList.clear()

                packetsToSend.forEach { pkt ->
                    mc.networkHandler?.sendPacket(pkt)
                }
            }
        }
    }
    @Suppress("unused")
    private val keyHandler = handler<KeyboardKeyEvent> { event ->
        val key = movementKeys.keys.find { it.matchesKey(event.keyCode, event.scanCode) }
            ?: return@handler
        val pressed = shouldHandleInputs(key) && event.action != GLFW.GLFW_RELEASE
        movementKeys.put(key, pressed)

        if (behavior == Behaviour.SAFE && isInInventoryScreen && InventoryManager.isInventoryOpenServerSide
            && pressed
        ) {
            closeInventorySilently()
        }
    }

    /**
     * Checks if the player is in the creative search field
     */
    private fun Screen.isInCreativeSearchField() =
        this is CreativeInventoryScreen &&
            CreativeInventoryScreen.selectedTab == ItemGroups.getSearchGroup()

}
