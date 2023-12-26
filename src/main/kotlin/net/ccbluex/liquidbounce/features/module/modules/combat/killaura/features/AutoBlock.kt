package net.ccbluex.liquidbounce.features.module.modules.combat.killaura.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.RaycastMode.*
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.range
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.raycast
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.targetTracker
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.wallRange
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.facingEnemy
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.aiming.raytraceEntity
import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.UseAction
import net.minecraft.util.hit.HitResult

object AutoBlock : ToggleableConfigurable(ModuleKillAura, "AutoBlocking", false) {

    val tickOff by int("TickOff", 0, 0..5)
    val tickOn by int("TickOn", 0, 0..5)
    val onScanRange by boolean("OnScanRange", true)
    val interactWith by boolean("InteractWith", true)
    val onlyWhenInDanger by boolean("OnlyWhenInDanger", true)

    var blockingStateEnforced = false

    /**
     * Visual blocking shows a blocking state, while not actually blocking.
     * This is useful to make the blocking animation become much smoother.
     */
    var visualBlocking = false

    fun makeSeemBlock() {
        if (!enabled) {
            return
        }

        visualBlocking = true
    }

    fun startBlocking() {
        if (!enabled || player.isBlocking) {
            return
        }

        if (onlyWhenInDanger && !isInDanger()) {
            stopBlocking()
            return
        }

        if (canBlock(player.mainHandStack)) {
            if (interactWith) {
                interactWithFront()
            }

            interaction.interactItem(player, Hand.MAIN_HAND)
            blockingStateEnforced = true
            visualBlocking = true
        } else if (canBlock(player.offHandStack)) {
            if (interactWith) {
                interactWithFront()
            }

            interaction.interactItem(player, Hand.OFF_HAND)
            blockingStateEnforced = true
            visualBlocking = true
        }
    }

    fun stopBlocking(pauses: Boolean = false) {
        if (!pauses) {
            visualBlocking = false
        }

        // We do not want the player to stop eating or else. Only when he blocks.
        if (player.isBlocking && !mc.options.useKey.isPressed) {
            interaction.stopUsingItem(player)
        }

        blockingStateEnforced = false
    }

    val changeSlot = handler<PacketEvent> {
        val packet = it.packet

        if (packet is UpdateSelectedSlotC2SPacket) {
            visualBlocking = false
        }
    }

    private fun interactWithFront() {
        // Raycast using the current rotation and find a block or entity that should be interacted with
        val rotationToTheServer = RotationManager.serverRotation

        val entity = raytraceEntity(range.toDouble(), rotationToTheServer, filter = {
            when (raycast) {
                TRACE_NONE -> false
                TRACE_ONLYENEMY -> it.shouldBeAttacked()
                TRACE_ALL -> true
            }
        })

        if (entity != null) {
            // Interact with entity
            // Check if it makes use to interactAt the entity
            // interaction.interactEntityAtLocation()
            interaction.interactEntity(player, entity, Hand.MAIN_HAND)
            return
        }

        val hitResult = raycast(range.toDouble(), rotationToTheServer, includeFluids = false) ?: return

        if (hitResult.type != HitResult.Type.BLOCK) {
            return
        }

        // Interact with block
        interaction.interactBlock(player, Hand.MAIN_HAND, hitResult)
    }

    private fun canBlock(itemStack: ItemStack) =
        itemStack.item?.getUseAction(itemStack) == UseAction.BLOCK

    private fun isInDanger() = targetTracker.enemies().any { target ->
        facingEnemy(
            fromEntity = target, toEntity = player, rotation = target.rotation, range = range.toDouble(),
            wallsRange = wallRange.toDouble()
        )
    }

}
