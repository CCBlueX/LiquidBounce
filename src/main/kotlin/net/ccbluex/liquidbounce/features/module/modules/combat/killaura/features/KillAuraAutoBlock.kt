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
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.ccbluex.liquidbounce.utils.client.releaseUsingItemInTickLoop
import net.ccbluex.liquidbounce.utils.client.sendHeldItemChange
import net.ccbluex.liquidbounce.utils.client.sendSwapItemWithOffhand
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.interactBlockStrict
import net.ccbluex.liquidbounce.utils.entity.interactEntityStrict
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.useItemStrict
import net.ccbluex.liquidbounce.utils.input.InputTracker.isPressedOnAny
import net.ccbluex.liquidbounce.utils.raytracing.findEntityInCrosshair
import net.ccbluex.liquidbounce.utils.raytracing.isLookingAtEntity
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.minecraft.client.renderer.ItemInHandRenderer
import net.minecraft.core.component.DataComponents.BLOCKS_ATTACKS
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket
import net.minecraft.network.protocol.game.ServerboundUseItemPacket
import net.minecraft.world.InteractionHand
import net.minecraft.world.phys.HitResult
import kotlin.random.Random

/**
 * ## Manual blocking (1.21.11) packet sequence
 *
 * ### On Entity
 *
 * - InteractAt (>=1.8)
 * - Interact (<=1.21.11)
 * - UseItem
 *
 * ### On block
 *
 * - UseItemOn
 * - UseItem
 *
 * If the effective blockable hand is offhand, the packets are doubled (main hand -> offhand).
 */
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
     * @see net.minecraft.client.Minecraft.handleKeybinds
     */
    var enforcedBlockingHand: InteractionHand? = null
        set(value) {
            ModuleDebug.debugParameter(this, "enforcedBlockingHand", value)
            ModuleDebug.debugParameter(this, if (value != null) {
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
        get() = currentTickOn == 0

    override fun onDisabled() {
        this.stopBlocking()
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
    fun startBlocking() {
        if (!running || Random.nextInt(100) > chance) {
            return
        }

        if (onlyWhenInDanger && !isInDanger()) {
            this.stopBlocking()
            return
        }

        if (player.isUsingItem) {
            return
        }

        val blockHand = InteractionHand.entries.firstOrNull {
            val itemStack = player.getItemInHand(it)
            itemStack.has(BLOCKS_ATTACKS)
                && itemStack.isItemEnabled(world.enabledFeatures())
                && !player.cooldowns.isOnCooldown(itemStack)
        } ?: return
        val rotation = RotationManager.serverRotation
        debugParameter("blockHand") { blockHand }

        when (blockMode) {
            BlockMode.INTERACT -> if (interactWithFacing(rotation, blockHand)) {
                currentTickOn = tickOnRange.random()
                blockVisual = true
                enforcedBlockingHand = blockHand
                return
            }
            BlockMode.FAKE -> {
                blockVisual = true
                return
            }
            else -> { }
        }

        // Interact with the item in the block hand
        val useItemResult = useItemStrict(rotation.yRot, rotation.xRot)
        if (useItemResult != null && useItemResult.hand == blockHand) {
            currentTickOn = tickOnRange.random()
            enforcedBlockingHand = blockHand
        }

        blockVisual = true
    }

    private var flushTicks = 0

    @Suppress("unused")
    private val gameTickHandler = handler<GameTickEvent> {
        flushTicks++

        if (enforcedBlockingHand != null) {
            blockingTicks++
        }
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
            ModuleDebug.debugParameter(this, "Flush", flushTicks)
            ModuleDebug.debugParameter(this, "Flush Reason", reason)
            flushTicks = 0
        }

        when {
            // Not blocking
            !blockVisual -> flush("N")

            // Start blocking
            enforcedBlockingHand != null || event.packet is ServerboundUseItemPacket -> flush("B")

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
            UnblockMode.SWAP_HAND -> if (isOlderThanOrEqual1_8) {
                false
            } else {
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
            // Interact with entity
            // Vanilla blocking action won't trigger swing
            val result = interactEntityStrict(entity, entityHitResult, rotation = rotation) ?: return false
            return result.isUseItemSuccess && result.hand == blockHand
        }

        val hitResult = traceFromPlayer(rotation)

        // Facing neither entity nor block -> call `useItem`
        return if (hitResult.type != HitResult.Type.BLOCK) {
            val useItemResult = useItemStrict(rotation.yRot, rotation.xRot)
            useItemResult != null && useItemResult.hand == blockHand
        } else {
            val result = interactBlockStrict(hitResult, rotation = rotation) ?: return false
            result.isUseItemSuccess && result.hand == blockHand
        }
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
        FAKE("Fake"),
    }

    enum class UnblockMode(override val tag: String) : Tagged {
        STOP_USING_ITEM("StopUsingItem"),
        CHANGE_SLOT("ChangeSlot"),
        SWAP_HAND("SwapHand"),
        NONE("None"),
    }

}
