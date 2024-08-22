/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.range
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.raycast
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.wallRange
import net.ccbluex.liquidbounce.utils.aiming.RotationObserver
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEquals1_7_10
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.isBlockAction
import net.ccbluex.liquidbounce.utils.entity.orientation
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.UseAction
import net.minecraft.util.hit.HitResult

object AutoBlock : ToggleableConfigurable(ModuleKillAura, "AutoBlocking", false) {

    private val blockMode by enumChoice("BlockMode", BlockMode.INTERACT)
    private val unblockMode by enumChoice("UnblockMode", UnblockMode.STOP_USING_ITEM)

    val tickOff by int("TickOff", 0, 0..2, "ticks")
    val tickOn by int("TickOn", 0, 0..2, "ticks")

    val onScanRange by boolean("OnScanRange", true)
    private val onlyWhenInDanger by boolean("OnlyWhenInDanger", false)

    /**
     * Enforces the blocking state on the Input
     *
     * todo: fix open screen affecting this
     * @see net.minecraft.client.MinecraftClient handleInputEvents
     */
    var blockingStateEnforced = false

    /**
     * Visual blocking shows a blocking state, while not actually blocking.
     * This is useful to make the blocking animation become much smoother.
     *
     * @see net.minecraft.client.render.item.HeldItemRenderer renderFirstPersonItem
     */
    var blockVisual = false
        get() = field && super.handleEvents()

    val shouldUnblockToHit
        get() = unblockMode != UnblockMode.NONE

    val blockImmediate
        get() = tickOn == 0 || blockMode == BlockMode.WATCHDOG

    /**
     * Make it seem like the player is blocking.
     */
    fun makeSeemBlock() {
        if (!enabled) {
            return
        }

        blockVisual = true
    }

    /**
     * Starts blocking.
     */
    fun startBlocking() {
        if (!enabled || (player.isBlockAction && blockMode != BlockMode.WATCHDOG)) {
            return
        }

        if (onlyWhenInDanger && !isInDanger()) {
            stopBlocking()
            return
        }

        val blockHand = when {
            canBlock(player.mainHandStack) -> Hand.MAIN_HAND
            canBlock(player.offHandStack) -> Hand.OFF_HAND
            else -> return  // We cannot block with any item.
        }

        val itemStack = player.getStackInHand(blockHand)

        // We do not want to block if the item is disabled.
        if (itemStack.isEmpty || !itemStack.isItemEnabled(world.enabledFeatures)) {
            return
        }

        // Since we fake the blocking state, we simply set the visual blocking state to true.
        if (blockMode == BlockMode.FAKE) {
            blockVisual = true
            return
        }

        if (blockMode == BlockMode.WATCHDOG) {
            val currentSlot = player.inventory.selectedSlot
            val nextSlot = (currentSlot + 1) % 9

            network.sendPacket(UpdateSelectedSlotC2SPacket(nextSlot))
            network.sendPacket(UpdateSelectedSlotC2SPacket(currentSlot))

            // We interact below as well. I am not sure if this is part of the magic bypass or an oversight.
            interactWithFront()
        }

        if (blockMode == BlockMode.INTERACT || blockMode == BlockMode.WATCHDOG) {
            interactWithFront()
        }

        // Interact with the item in the block hand
        val actionResult = interaction.interactItem(player, blockHand)
        if (actionResult.isAccepted) {
            if (actionResult.shouldSwingHand()) {
                player.swingHand(blockHand)
            }
        }

        blockVisual = true
        blockingStateEnforced = true
    }

    fun stopBlocking(pauses: Boolean = false): Boolean {
        if (!pauses) {
            blockVisual = false
        }

        // We do not want the player to stop eating or else. Only when he blocks.
        if (!player.isBlockAction || mc.options.useKey.isPressed) {
            return false
        }

        return when {
            unblockMode == UnblockMode.STOP_USING_ITEM -> {
                interaction.stopUsingItem(player)

                blockingStateEnforced = false
                true
            }

            unblockMode == UnblockMode.CHANGE_SLOT -> {
                val currentSlot = player.inventory.selectedSlot
                val nextSlot = (currentSlot + 1) % 9

                // todo: add support for tick-off delay, since this is a bit too fast
                network.sendPacket(UpdateSelectedSlotC2SPacket(nextSlot))
                network.sendPacket(UpdateSelectedSlotC2SPacket(currentSlot))

                blockingStateEnforced = false
                true
            }

            unblockMode == UnblockMode.NONE && !pauses -> {
                interaction.stopUsingItem(player)

                blockingStateEnforced = false
                true
            }
            else -> false
        }
    }

    val changeSlot = handler<PacketEvent> {
        val packet = it.packet

        if (packet is UpdateSelectedSlotC2SPacket) {
            blockVisual = false
        }
    }

    /**
     * Interact with the block or entity in front of the player.
     */
    private fun interactWithFront() {
        // Raycast using the current rotation and find a block or entity that should be interacted with
        val rotationToTheServer = RotationObserver.serverOrientation

        val entityHitResult = raytraceEntity(range.toDouble(), rotationToTheServer, filter = {
            when (raycast) {
                TRACE_NONE -> false
                TRACE_ONLYENEMY -> it.shouldBeAttacked()
                TRACE_ALL -> true
            }
        })
        val entity = entityHitResult?.entity

        if (entity != null) {
            // 1.7 players do not send INTERACT_AT
            if (!isOlderThanOrEquals1_7_10) {
                interaction.interactEntityAtLocation(player, entity, entityHitResult, Hand.MAIN_HAND)
            }

            // INTERACT
            interaction.interactEntity(player, entity, Hand.MAIN_HAND)
            return
        }

        val hitResult = raycast(rotationToTheServer) ?: return

        if (hitResult.type != HitResult.Type.BLOCK) {
            return
        }

        // Interact with block
        interaction.interactBlock(player, Hand.MAIN_HAND, hitResult)
    }

    /**
     * Check if the player can block with the given item stack.
     */
    private fun canBlock(itemStack: ItemStack) =
        itemStack.item?.getUseAction(itemStack) == UseAction.BLOCK

    /**
     * Check if the player is in danger.
     */
    private fun isInDanger() = targetTracker.enemies().any { target ->
        facingEnemy(
            fromEntity = target,
            toEntity = player,
            rotation = target.orientation,
            range = range.toDouble(),
            wallsRange = wallRange.toDouble()
        )
    }

    enum class BlockMode(override val choiceName: String) : NamedChoice {
        BASIC("Basic"),
        INTERACT("Interact"),
        WATCHDOG("Watchdog117"),
        FAKE("Fake"),
    }

    enum class UnblockMode(override val choiceName: String) : NamedChoice {
        STOP_USING_ITEM("StopUsingItem"),
        CHANGE_SLOT("ChangeSlot"),
        NONE("None")
    }

}
