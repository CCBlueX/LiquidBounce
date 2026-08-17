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

import com.mojang.blaze3d.platform.InputConstants
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
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.isInInventoryScreen
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FINAL_DECISION
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.network.isC2SContainerPacket
import net.ccbluex.liquidbounce.utils.network.sendCloseInventory
import net.ccbluex.liquidbounce.utils.network.sendPacketSilently
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.components.EditBox
import net.minecraft.client.gui.components.MultiLineEditBox
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.input.KeyEvent
import net.minecraft.network.protocol.Packet
import net.minecraft.world.entity.player.Input

/**
 * InventoryMove module
 *
 * Allows you to walk while an inventory is opened.
 */

object ModuleInventoryMove : ClientModule("InventoryMove", ModuleCategories.MOVEMENT) {

    private val behavior by enumChoice("Behavior", Behaviour.NORMAL).also(::tagBy)

    @Suppress("unused")
    enum class Behaviour(override val tag: String, val handleScreens: (Screen) -> Boolean) : Tagged {
        NORMAL("Normal", { !it.isInEditBox() && !ModuleClickGui.isInSearchBar }),
        // disable clicks while moving
        SAFE("Safe", { NORMAL.handleScreens(it) && (it !is AbstractContainerScreen<*> || it is InventoryScreen) }),
        // stop in inventory
        UNDETECTABLE("Undetectable", { NORMAL.handleScreens(it) && it !is AbstractContainerScreen<*> }),
        // stop input on inventory action
        STOP_ON_ACTION("StopOnAction", { NORMAL.handleScreens(it) })
    }

    private fun Screen.isInEditBox() = when (this.focused) {
        is EditBox, is MultiLineEditBox -> true
        else -> false
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
     * Restricts user from clicking while moving or sprinting in inventory.
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

    @JvmStatic
    fun shouldHandleInputs(key: KeyMapping, screen: Screen? = mc.gui.screen()): Boolean {
        if (!running) return false
        val screen = screen ?: return true

        when (key) {
            mc.options.keyShift -> passthroughSneak
            mc.options.keyJump, mc.options.keyUp, mc.options.keyDown, mc.options.keyLeft, mc.options.keyRight,
            mc.options.keySprint -> true
            else -> false
        }.also { if (!it) return false }

        // If we are in a handled screen, we should handle the inputs only if the undetectable option is not enabled
        return behavior.handleScreens(screen)
    }

    @JvmStatic
    fun shouldHandleInputs(event: KeyEvent) = KeyMapping.MAP[InputConstants.getKey(event)]
        ?.any(::shouldHandleInputs)
        ?: false

    @Suppress("unused")
    private val keyHandler = handler<KeyboardKeyEvent> { event ->
        if (behavior !== Behaviour.SAFE) return@handler

        val key = movementKeys.keys.find { it.matches(KeyEvent(event.keyCode, event.scanCode, event.mods)) }
            ?: return@handler
        val pressed = shouldHandleInputs(key) && !event.isReleased
        movementKeys.put(key, pressed)

        if (isInInventoryScreen && InventoryManager.isInventoryOpenServerSide && pressed) {
            network.sendCloseInventory()
        }
    }

    private val delayedContainerPackets = mutableListOf<Packet<*>>()

    override fun onDisabled() {
        delayedContainerPackets.clear()
        super.onDisabled()
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(FINAL_DECISION) {
        if (delayedContainerPackets.isEmpty()) return@handler

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
        if (behavior !== Behaviour.STOP_ON_ACTION) return@handler

        val packet = event.packet
        if (packet.isC2SContainerPacket() && player.input.keyPresses != Input.EMPTY) {
            event.cancelEvent()
            // Here only be called from render thread because [packet] is c2s
            delayedContainerPackets += packet
        }
    }


}
