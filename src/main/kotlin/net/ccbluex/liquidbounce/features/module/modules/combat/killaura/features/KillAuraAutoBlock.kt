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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.isNewerThanOrEquals1_21_5
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.releaseUsingItemInTickLoop
import net.ccbluex.liquidbounce.utils.client.sendHeldItemChange
import net.ccbluex.liquidbounce.utils.client.sendSwapItemWithOffhand
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.interactBlock
import net.ccbluex.liquidbounce.utils.entity.interactBlockLikeVanilla
import net.ccbluex.liquidbounce.utils.entity.interactEntity
import net.ccbluex.liquidbounce.utils.entity.interactEntityLikeVanilla
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.useItem
import net.ccbluex.liquidbounce.utils.entity.useItemStrict
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.math.sq
import net.ccbluex.liquidbounce.utils.raytracing.findEntityInCrosshair
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.phys.HitResult
import kotlin.random.Random

object KillAuraAutoBlock : ToggleableValueGroup(ModuleKillAura, "AutoBlocking", false) {

    private val blockMode by enumChoice("BlockMode", BlockMode.INTERACT)
    /**
     * This options means to simulate vanilla use item action.
     * If the effective hand (item) is [InteractionHand.OFF_HAND],
     * It tries the main hand then offhand.
     */
    private val simulateVanillaUse by boolean("SimulateVanillaUse", true)
    private val unblockMode by enumChoice("UnblockMode", UnblockMode.STOP_USING_ITEM)

    private val reblockTicksRange by intRange(
        "Reblock", 0..0, 0..3, "ticks", aliases = listOf("TickOn")
    ).onChanged { range ->
        reblockTicks = range.random()
    }
    private val pauseOnUnblockTicksRange by intRange(
        "PauseOnUnblock", 0..0, 0..3, "ticks", aliases = listOf("TickOff")
    ).onChanged { range ->
        pauseOnUnblockTicks = range.random()
    }

    var reblockTicks: Int = reblockTicksRange.random()
    var pauseOnUnblockTicks: Int = pauseOnUnblockTicksRange.random()

    val chance by float("Chance", 100f, 0f..100f, "%")
    val blink by int("Blink", 0, 0..10, "ticks")

    private val prioritizeBlocking by boolean("PrioritizeBlocking", true)
    val onScanRange by boolean("OnScanRange", true)
    private val onlyWhenInDanger by boolean("OnlyWhenInDanger", false)

    /** For 1.9~1.21.4 protocol on 1.8 server, server will send a shield to your offhand on using item */
    private val assumeShield by boolean("AssumeShield", false)

    private var blockingTicks = 0

    /**
     * Enforces the blocking state on the Input
     *
     * todo: fix open screen affecting this
     * @see net.minecraft.client.Minecraft.handleKeybinds
     */
    var enforcedBlockingHand: InteractionHand? = null
        set(value) {
            debugParameter(this, "EnforcedBlockingHand", value)
            debugParameter(this, if (value != null) {
                "Block Age"
            } else {
                "Unblock Age"
            }, player.tickCount)

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
        get() = reblockTicks == 0

    var hasBlockedSinceAttack = false

    var isInDanger = false

    /**
     * This will decrease our CPS and prioritize blocking.
     */
    val isPrioritizingBlocking: Boolean
        get() {
            // Fixes the deadlock caused by [startBlocking]
            if (player.isUsingItem) {
                hasBlockedSinceAttack = true
            }

            // Check if we cannot prioritize blocking
            if (!running || !prioritizeBlocking || blockMode == BlockMode.FAKE || findBlockableHand() == null) {
                return false
            }

            // If we haven't blocked, and we are in danger, prioritize blocking
            return !hasBlockedSinceAttack && (!onlyWhenInDanger || isInDanger)
        }

    override fun onDisabled() {
        this.stopBlocking()
        this.hasBlockedSinceAttack = false
        this.isInDanger = false
        super.onDisabled()
    }

    /**
     * Make it seem like the player is blocking.
     */
    fun makeSeemBlock() {
        if (!running) {
            return
        }

        blockVisual = true
    }

    /**
     * Starts blocking.
     */
    @Suppress("ReturnCount", "CognitiveComplexMethod")
    fun startBlocking(): Boolean {
        if (!running || Random.nextInt(100) > chance) {
            return false
        }

        if (onlyWhenInDanger && !isInDanger) {
            this.stopBlocking()
            return false
        }

        if (player.isUsingItem) {
            hasBlockedSinceAttack = true
            return false
        }

        val blockHand = findBlockableHand() ?: return false
        val rotation = RotationManager.currentRotation ?: player.rotation
        debugParameter("BlockHand") { blockHand }

        when (blockMode) {
            BlockMode.INTERACT -> if (interactWithFacing(rotation, blockHand)) {
                reblockTicks = reblockTicksRange.random()
                blockVisual = true
                enforcedBlockingHand = blockHand
                hasBlockedSinceAttack = true
                return true
            }
            BlockMode.FAKE -> {
                blockVisual = true
                return false
            }
            else -> { }
        }

        // Interact with the item in the block hand
        if (genericUseItem(rotation, blockHand)) {
            reblockTicks = reblockTicksRange.random()
            enforcedBlockingHand = blockHand
            hasBlockedSinceAttack = true
        }

        blockVisual = true
        return true
    }

    private var flushTicks = 0

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent> {
        flushTicks++

        if (enforcedBlockingHand != null) {
            blockingTicks++
        }

        // Check if we are in danger by going through all possible targets and checking if they are looking at us.
        isInDanger = targetTracker.targets().any { target ->
            player.squaredBoxedDistanceTo(target) <= KillAuraRange.interactionRange.sq() && isLookingAtEntity(
                fromEntity = target,
                toEntity = player,
                rotation = target.rotation,
                range = range.interactionRange.toDouble(),
                throughWallsRange = range.interactionThroughWallsRange.toDouble()
            ) != null
        }
        debugParameter("IsInDanger") { isInDanger }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        enforcedBlockingHand = null
    }

    @Suppress("unused")
    private val blinkHandler = handler<BlinkPacketEvent> { event ->
        if (event.origin != TransferOrigin.OUTGOING) {
            return@handler
        }

        fun flush(reason: String) {
            debugParameter(this, "Flush", flushTicks)
            debugParameter(this, "Flush Reason", reason)
            flushTicks = 0
        }

        when {
            // Not blocking
            !blockVisual -> flush("Not blocking")

            // Start blocking
            enforcedBlockingHand != null || event.packet is ServerboundUseItemPacket -> flush("Start blocking")

            // Timeout reached
            flushTicks >= blink -> flush("Timed out")

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

        pauseOnUnblockTicks = pauseOnUnblockTicksRange.random()

        return when (unblockMode) {
            UnblockMode.STOP_USING_ITEM -> {
                interaction.releaseUsingItemInTickLoop()
                enforcedBlockingHand = null
                true
            }

            // Not working when blocking with offhand
            UnblockMode.CHANGE_SLOT -> {
                val currentSlot = player.inventory.selectedSlot
                val nextSlot = (currentSlot + 1) % 9
                network.sendHeldItemChange(nextSlot)
                network.sendHeldItemChange(currentSlot)
                if (enforcedBlockingHand == InteractionHand.MAIN_HAND) {
                    enforcedBlockingHand = null
                    true
                } else {
                    false
                }
            }

            // Not working when server doesn't have offhand
            UnblockMode.SWAP_HAND -> {
                network.sendSwapItemWithOffhand()
                network.sendSwapItemWithOffhand()
                enforcedBlockingHand = null
                true
            }

            UnblockMode.NONE -> if (!pauses) {
                interaction.releaseUsingItemInTickLoop()
                enforcedBlockingHand = null
                true
            } else {
                false
            }
        }
    }

    @Suppress("unused")
    private val changeSlot = handler<PacketEvent> { event ->
        val packet = event.packet

        if ((packet is ServerboundSetCarriedItemPacket &&
            enforcedBlockingHand == InteractionHand.MAIN_HAND) ||
            (packet is ServerboundPlayerActionPacket &&
            packet.action === ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND)
        ) {
            blockVisual = false
            enforcedBlockingHand = null
        }
    }

    /**
     * Interact with the block or entity in front of the player.
     *
     * @param rotation Raycast using the current rotation
     * and find a block or entity that should be interacted with
     * @return if successfully started blocking
     */
    private fun interactWithFacing(rotation: Rotation, blockHand: InteractionHand): Boolean {
        val entityHitResult =
            findEntityInCrosshair(range.interactionRange.toDouble(), rotation, predicate = {
                when (raycast) {
                    TRACE_NONE -> false
                    TRACE_ONLYENEMY -> it.shouldBeAttacked()
                    TRACE_ALL -> true
                }
            })
        val entity = entityHitResult?.entity

        if (entity != null) {
            return if (simulateVanillaUse) {
                // Interact with entity. Vanilla blocking action won't trigger swing.
                val result = interactEntityLikeVanilla(entity, entityHitResult, rotation = rotation) ?: return false
                result.isUseItemSuccess && result.hand == blockHand
            } else {
                interactEntity(entity, entityHitResult, hand = blockHand) is InteractionResult.Success
            }
        }

        val hitResult = traceFromPlayer(rotation)

        // Facing neither entity nor block -> call `useItem`
        return if (hitResult.type != HitResult.Type.BLOCK) {
            genericUseItem(rotation, blockHand)
        } else {
            if (simulateVanillaUse) {
                val result = interactBlockLikeVanilla(hitResult, rotation = rotation) ?: return false
                result.isUseItemSuccess && result.hand == blockHand
            } else {
                interactBlock(hitResult, hand = blockHand) is InteractionResult.Success
            }
        }
    }

    /**
     * Successfully started to block (e.g. sword/shield) -> useItem Success
     */
    private fun genericUseItem(rotation: Rotation, blockHand: InteractionHand): Boolean {
        return if (simulateVanillaUse) {
            val useItemResult = useItemStrict(rotation.yRot, rotation.xRot)
            useItemResult != null && useItemResult.hand == blockHand
        } else {
            useItem(blockHand, rotation.yRot, rotation.xRot) is InteractionResult.Success
        }
    }

    /**
     * @return the first hand can be used to block
     */
    private fun findBlockableHand() = InteractionHand.entries.find {
        val itemStack = player.getItemInHand(it)
        itemStack.has(BLOCKS_ATTACKS)
            && itemStack.isItemEnabled(world.enabledFeatures())
            && !player.cooldowns.isOnCooldown(itemStack)
    } ?: if (assumeShield && !isOlderThanOrEqual1_8 && !isNewerThanOrEquals1_21_5 && player.mainHandItem.isSword) {
        InteractionHand.MAIN_HAND
    } else {
        null
    }

    enum class BlockMode(override val tag: String) : Tagged {
        BASIC("Basic"),
        INTERACT("Interact"),
        FAKE("Fake"),
    }

    enum class UnblockMode(override val tag: String) : Tagged {
        STOP_USING_ITEM("StopUsingItem"),
        CHANGE_SLOT("ChangeSlot"),
        SWAP_HAND("SwapHand"),
        NONE("None"),
    }

}
