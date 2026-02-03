/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.fastutil.fastIterable
import net.ccbluex.fastutil.referenceBooleanArrayMapOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.KeyboardKeyEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveBlinkFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveSneakControlFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveSprintControlFeature
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features.InventoryMoveTimerFeature
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleClickGui
import net.ccbluex.liquidbounce.utils.client.sendCloseInventory
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.entity.any
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.isInInventoryScreen
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FINAL_DECISION
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.network.isC2SContainerPacket
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.ChatScreen
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.protocol.Packet
import net.minecraft.world.item.CreativeModeTabs
import org.lwjgl.glfw.GLFW

/**
 * InventoryMove module
 *
 * Allows you to walk while an inventory is opened.
 */

object ModuleInventoryMove : ClientModule("InventoryMove", ModuleCategories.MOVEMENT) {

    private val behavior by enumChoice("Behavior", Behaviour.NORMAL).also(::tagBy)

    @Suppress("unused")
    enum class Behaviour(override val tag: String) : Tagged {
        NORMAL("Normal"),
        SAFE("Safe"), // disable clicks while moving
        UNDETECTABLE("Undetectable"), // stop in inventory
        STOP_ON_ACTION("StopOnAction"), // stop input on inventory action
    }

    private val passthroughSneak by boolean("PassthroughSneak", false)

    // states of movement keys, using mc.options.<key>.isPressed doesn't work for some reason
    private val movementKeys =
        referenceBooleanArrayMapOf(
            mc.options.keyUp, false,
            mc.options.keyLeft, false,
            mc.options.keyDown, false,
            mc.options.keyRight, false,
            mc.options.keyJump, false,
            mc.options.keyShift, false,
        )

    /**
     * Restricts user from clicking while moving in inventory.
     */
    val doNotAllowClicking
        get() = behavior === Behaviour.SAFE && movementKeys.fastIterable().any {
            it.booleanValue && shouldHandleInputs(it.key)
        }

    init {
        tree(InventoryMoveSprintControlFeature)
        tree(InventoryMoveSneakControlFeature)
        tree(InventoryMoveTimerFeature)
        tree(InventoryMoveBlinkFeature)
    }

    fun shouldHandleInputs(keyBinding: KeyMapping): Boolean {
        val screen = mc.screen ?: return true

        if (!running || screen is ChatScreen || screen.isInCreativeSearchField() || ModuleClickGui.isInSearchBar) {
            return false
        }

        if (keyBinding == mc.options.keyShift && !passthroughSneak) {
            return false
        }

        // If we are in a handled screen, we should handle the inputs only if the undetectable option is not enabled
        return behavior === Behaviour.NORMAL || screen !is AbstractContainerScreen<*>
            || behavior === Behaviour.SAFE && screen is InventoryScreen
            || behavior === Behaviour.STOP_ON_ACTION
    }

    private val delayedContainerPackets = mutableListOf<Packet<*>>()

    override fun onDisabled() {
        delayedContainerPackets.clear()
        super.onDisabled()
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(FINAL_DECISION) {
        if (delayedContainerPackets.isEmpty() ||
            behavior !== Behaviour.STOP_ON_ACTION || !InventoryManager.isHandledScreenOpen) {
            return@handler
        }

        val packetsSnapshot = delayedContainerPackets.toTypedArray()
        delayedContainerPackets.clear()
        it.sneak = false
        it.jump = false
        it.directionalInput = DirectionalInput.NONE
        // `schedule` will force the Runnable to be run in next loop
        mc.schedule { packetsSnapshot.forEach(::sendPacketSilently) }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(FIRST_PRIORITY) { event ->
        if (behavior !== Behaviour.STOP_ON_ACTION || !InventoryManager.isHandledScreenOpen) {
            return@handler
        }

        val packet = event.packet

        if (packet.isC2SContainerPacket() && player.input.keyPresses.any) {
            event.cancelEvent()
            // Here only be called from render thread because [packet] is c2s
            delayedContainerPackets += packet
        }
    }

    @Suppress("unused")
    private val keyHandler = handler<KeyboardKeyEvent> { event ->
        val key = movementKeys.keys.find { it.matches(KeyEvent(event.keyCode, event.scanCode, event.mods)) }
            ?: return@handler
        val pressed = shouldHandleInputs(key) && event.action != GLFW.GLFW_RELEASE
        movementKeys.put(key, pressed)

        if (behavior === Behaviour.SAFE && isInInventoryScreen && InventoryManager.isInventoryOpenServerSide
            && pressed) {
            network.sendCloseInventory()
        }
    }

    /**
     * Checks if the player is in the creative search field
     */
    private fun Screen.isInCreativeSearchField() =
        this is CreativeModeInventoryScreen &&
            CreativeModeInventoryScreen.selectedTab == CreativeModeTabs.searchTab()

}
