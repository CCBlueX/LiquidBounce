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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.range
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.raycast
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.wallRange
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.utils.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEquals1_7_10
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.isBlockAction
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.input.shouldSwingHand
import net.minecraft.item.ItemStack
import net.minecraft.item.consume.UseAction
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.HitResult
import kotlin.random.Random

object KillAuraAutoBlock : ToggleableConfigurable(ModuleKillAura, "AutoBlocking", false) {

    private val blockMode by enumChoice("BlockMode", BlockMode.INTERACT)
    private val unblockMode by enumChoice("UnblockMode", UnblockMode.STOP_USING_ITEM)

    val tickOffRange by intRange("TickOff", 0..0, 0..5, "ticks").onChanged { range ->
        currentTickOff = range.random()
    }
    val tickOnRange by intRange("TickOn", 0..0, 0..5, "ticks").onChanged { range ->
        currentTickOn = range.random()
    }

    var currentTickOff: Int = tickOffRange.random()
    var currentTickOn: Int = tickOnRange.random()

    val chance by float("Chance", 100f, 0f..100f, "%")
    val blink by int("Blink", 0, 0..10, "ticks")

    val onScanRange by boolean("OnScanRange", true)
    private val onlyWhenInDanger by boolean("OnlyWhenInDanger", false)

    private var blockingTicks = 0

    /**
     * Enforces the blocking state on the Input
     *
     * todo: fix open screen affecting this
     * @see net.minecraft.client.MinecraftClient handleInputEvents
     */
    var blockingStateEnforced = false
        set(value) {
            ModuleDebug.debugParameter(this, "BlockingStateEnforced", value)
            ModuleDebug.debugParameter(this, if (value) {
                "Block Age"
            } else {
                "Unblock Age"
            }, player.age)

            field = value
        }

    /**
     * Visual blocking shows a blocking state, while not actually blocking.
     * This is useful to make the blocking animation become much smoother.
     *
     * @see net.minecraft.client.render.item.HeldItemRenderer renderFirstPersonItem
     */
    var blockVisual = false
        get() = field && super.running && (isOlderThanOrEqual1_8 || ModuleSwordBlock.running)

    val shouldUnblockToHit
        get() = unblockMode != UnblockMode.NONE

    val blockImmediate
        get() = currentTickOn == 0 || blockMode == BlockMode.HYPIXEL

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
    @Suppress("ReturnCount", "CognitiveComplexMethod")
    fun startBlocking() {
        if (!enabled || (player.isBlockAction && blockMode != BlockMode.HYPIXEL)) {
            return
        }

        if (Random.nextInt(100) > chance) {
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

        when (blockMode) {
            BlockMode.HYPIXEL -> {
                val target = targetTracker.target

                if (target == null) {
                    interaction.interactItem(player, Hand.MAIN_HAND)
                } else {
                    interaction.interactEntity(player, target, Hand.MAIN_HAND)
                }
            }
            BlockMode.FAKE -> {
                blockVisual = true
                return
            }
            else -> { }
        }

        if (blockMode == BlockMode.INTERACT || blockMode == BlockMode.HYPIXEL) {
            interactWithFront()
        }

        // Interact with the item in the block hand
        val actionResult = interaction.interactItem(player, blockHand)

        if (actionResult.isAccepted) {
            if (actionResult.shouldSwingHand()) {
                currentTickOn = tickOnRange.random()
                player.swingHand(blockHand)
            }
        }

        blockVisual = true
        blockingStateEnforced = true
    }

    private var flushTicks = 0

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent> {
        flushTicks++

        if (blockingStateEnforced) {
            blockingTicks++
        }

        if (blockMode == BlockMode.HYPIXEL && blockingTicks % 5 == 0 && blockingStateEnforced) {
            interaction.interactItem(player, Hand.MAIN_HAND)
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        blockingStateEnforced = false
    }

    @Suppress("unused")
    private val blinkHandler = handler<QueuePacketEvent> { event ->
        if (event.origin != TransferOrigin.OUTGOING) {
            return@handler
        }

        fun flush(reason: String) {
            ModuleDebug.debugParameter(this, "Flush", flushTicks)
            ModuleDebug.debugParameter(this, "Flush Reason", reason)
            flushTicks = 0
        }

        when {
            // Not blocking
            !blockVisual -> flush("N")

            // Start blocking
            blockingStateEnforced || event.packet is PlayerInteractItemC2SPacket -> flush("B")

            // Timeout reached
            flushTicks >= blink -> flush("T")

            // Start to queue
            else -> event.action = PacketQueueManager.Action.QUEUE
        }
    }

    fun stopBlocking(pauses: Boolean = false): Boolean {
        if (!pauses) {
            blockVisual = false

            if (mc.options.useKey.isPressedOnAny) {
                return false
            }
        }

        // We do not want the player to stop eating or else. Only when he blocks.
        if (!player.isBlockAction) {
            return false
        }

        currentTickOff = tickOffRange.random()

        return when {
            unblockMode == UnblockMode.STOP_USING_ITEM -> {
                interaction.stopUsingItem(player)

                blockingStateEnforced = false
                true
            }

            unblockMode == UnblockMode.CHANGE_SLOT -> {
                val currentSlot = player.inventory.selectedSlot
                val nextSlot = (currentSlot + 1) % 8
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

    @Suppress("unused")
    private val changeSlot = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is UpdateSelectedSlotC2SPacket) {
            blockVisual = false
            blockingStateEnforced = false
        }
    }

    /**
     * Interact with the block or entity in front of the player.
     */
    private fun interactWithFront() {
        // Raycast using the current rotation and find a block or entity that should be interacted with
        val rotationToTheServer = RotationManager.serverRotation

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
    private fun isInDanger() = targetTracker.targets().any { target ->
        facingEnemy(
            fromEntity = target,
            toEntity = player,
            rotation = target.rotation,
            range = range.toDouble(),
            wallsRange = wallRange.toDouble()
        )
    }

    enum class BlockMode(override val choiceName: String) : NamedChoice {
        BASIC("Basic"),
        INTERACT("Interact"),
        HYPIXEL("Hypixel"),
        FAKE("Fake"),
    }

    enum class UnblockMode(override val choiceName: String) : NamedChoice {
        STOP_USING_ITEM("StopUsingItem"),
        CHANGE_SLOT("ChangeSlot"),
        NONE("None")
    }

}
