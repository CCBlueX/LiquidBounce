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
package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.combat.ModuleSwordBlock
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.TRACE_ALL
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.TRACE_NONE
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.TRACE_ONLYENEMY
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.range
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.raycast
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEquals1_7_10
import net.ccbluex.liquidbounce.utils.client.releaseUsingItemNextTick
import net.ccbluex.liquidbounce.utils.client.sendHeldItemChange
import net.ccbluex.liquidbounce.utils.client.sendSwapItemWithOffhand
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.isBlockAction
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.input.shouldSwingHand
import net.ccbluex.liquidbounce.utils.raytracing.findEntityInCrosshair
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.HitResult
import kotlin.random.Random

object KillAuraAutoBlock : ToggleableValueGroup(ModuleKillAura, "AutoBlocking", false) {

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
     * @see Minecraft.handleKeybinds
     */
    var blockingStateEnforced = false
        set(value) {
            ModuleDebug.debugParameter(this, "BlockingStateEnforced", value)
            ModuleDebug.debugParameter(this, if (value) {
                "Block Age"
            } else {
                "Unblock Age"
            }, player.tickCount
            )

            field = value
        }

    /**
     * Visual blocking shows a blocking state, while not actually blocking.
     * This is useful to make the blocking animation become much smoother.
     *
     * @see ItemInHandRenderer.renderArmWithItem
     */
    var blockVisual = false
        get() = field && running &&
            (isOlderThanOrEqual1_8 || ModuleSwordBlock.running)

    val shouldUnblockToHit
        get() = unblockMode != UnblockMode.NONE

    val blockImmediate
        get() = currentTickOn == 0 || blockMode == BlockMode.HYPIXEL

    override fun onDisabled() {
        this.stopBlocking()
        super.onDisabled()
    }

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
            this.stopBlocking()
            return
        }

        val blockHand = InteractionHand.entries.find {
            val itemStack = player.getItemInHand(it)
            itemStack.has(BLOCKS_ATTACKS)
                && itemStack.isItemEnabled(world.enabledFeatures())
                && !player.cooldowns.isOnCooldown(itemStack)
        } ?: return

        when (blockMode) {
            BlockMode.HYPIXEL -> {
                val target = targetTracker.target

                if (target == null) {
                    interaction.useItem(player, InteractionHand.MAIN_HAND)
                } else {
                    interaction.interact(player, target, InteractionHand.MAIN_HAND)
                }

                interactWithFront()
            }
            BlockMode.INTERACT -> interactWithFront()
            BlockMode.FAKE -> {
                blockVisual = true
                return
            }
            else -> { }
        }

        // Interact with the item in the block hand
        val actionResult = interaction.useItem(player, blockHand)

        if (actionResult.consumesAction()) {
            if (actionResult.shouldSwingHand()) {
                currentTickOn = tickOnRange.random()
                player.swing(blockHand)
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
            interaction.useItem(player, InteractionHand.MAIN_HAND)
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        blockingStateEnforced = false
    }

    @Suppress("unused")
    private val blinkHandler = handler<BlinkPacketEvent> { event ->
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
            blockingStateEnforced || event.packet is ServerboundUseItemPacket -> flush("B")

            // Timeout reached
            flushTicks >= blink -> flush("T")

            // Start to queue
            else -> event.action = BlinkManager.Action.QUEUE
        }
    }

    fun stopBlocking(pauses: Boolean = false): Boolean {
        if (!pauses) {
            blockVisual = false

            if (mc.options.keyUse.isPressedOnAny) {
                return false
            }
        }

        // We do not want the player to stop eating or else. Only when he blocks.
        if (!player.isBlocking) {
            return false
        }

        currentTickOff = tickOffRange.random()

        return when (unblockMode) {
            UnblockMode.STOP_USING_ITEM -> {
                interaction.releaseUsingItemNextTick()
                blockingStateEnforced = false
                true
            }
            // Not working when blocking with offhand
            UnblockMode.CHANGE_SLOT -> {
                val currentSlot = player.inventory.selectedSlot
                val nextSlot = (currentSlot + 1) % 8
                network.sendHeldItemChange(nextSlot)
                network.sendHeldItemChange(currentSlot)
                blockingStateEnforced = false
                true
            }
            // Not working when server doesn't have offhand
            UnblockMode.SWAP_HAND -> {
                network.sendSwapItemWithOffhand()
                network.sendSwapItemWithOffhand()
                blockingStateEnforced = false
                true
            }
            UnblockMode.NONE -> if (!pauses) {
                interaction.releaseUsingItemNextTick()
                blockingStateEnforced = false
                true
            } else {
                false
            }
        }
    }

    @Suppress("unused")
    private val changeSlot = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ServerboundSetCarriedItemPacket) {
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

        val entityHitResult =
            findEntityInCrosshair(range.interactionRange.toDouble(), rotationToTheServer, predicate = {
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
                interaction.interactAt(player, entity, entityHitResult, InteractionHand.MAIN_HAND)
            }

            // INTERACT
            interaction.interact(player, entity, InteractionHand.MAIN_HAND)
            return
        }

        val hitResult = traceFromPlayer(rotationToTheServer)

        if (hitResult.type != HitResult.Type.BLOCK) {
            return
        }

        // Interact with block
        interaction.useItemOn(player, InteractionHand.MAIN_HAND, hitResult)
    }

    /**
     * Check if the player is in danger.
     */
    private fun isInDanger() = targetTracker.targets().any { target ->
        isLookingAtEntity(
            fromEntity = target,
            toEntity = player,
            rotation = target.rotation,
            range = range.interactionRange.toDouble(),
            throughWallsRange = range.interactionThroughWallsRange.toDouble()
        ) != null
    }

    enum class BlockMode(override val tag: String) : Tagged {
        BASIC("Basic"),
        INTERACT("Interact"),
        HYPIXEL("Hypixel"),
        FAKE("Fake"),
    }

    enum class UnblockMode(override val tag: String) : Tagged {
        STOP_USING_ITEM("StopUsingItem"),
        CHANGE_SLOT("ChangeSlot"),
        SWAP_HAND("SwapHand"),
        NONE("None"),
    }

}
