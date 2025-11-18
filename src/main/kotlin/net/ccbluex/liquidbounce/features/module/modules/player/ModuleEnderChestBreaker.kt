package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAirPlace
import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.isConsumable
import net.minecraft.block.Blocks
import net.minecraft.client.option.KeyBinding
import net.minecraft.item.Items
import net.minecraft.util.hit.BlockHitResult

/**
 * EnderChestBreaker
 * Automatically breaks ender chests from your inventory.
 */
object ModuleEnderChestBreaker : ClientModule("EnderChestBreaker", Category.PLAYER) {
    val allowAirPlace by boolean("AllowAirPlace", false)
    private val inventoryConstraints = tree(
        PlayerInventoryConstraints().apply {
            requirements += InventoryRequirements.NO_MOVEMENT
        }
    )

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (ModuleOffhand.isOperating()) {
            chat(markAsError(message("offhandConflict")))
            enabled = false
        }
    }

    @JvmStatic
    fun canUseOffhand(): Boolean {
        if (!player.offHandStack.isOf(Items.ENDER_CHEST) || player.mainHandStack.isConsumable) return false

        val hit = mc.crosshairTarget as? BlockHitResult ?: return false
        val targetBlock = hit.blockPos.getBlock() ?: return false

        if (targetBlock == Blocks.ENDER_CHEST) return false

        val adjacent = world.getBlockState(hit.blockPos.offset(hit.side))
        if (adjacent.block == Blocks.ENDER_CHEST) return false

        val targetState = world.getBlockState(hit.blockPos)
        if (!targetState.fluidState.isEmpty) return false

        if (targetState.isAir) return allowAirPlace && ModuleAirPlace.running

        return Blocks.ENDER_CHEST.defaultState.canPlaceAt(world, hit.blockPos)
    }


    @Suppress("unused")
    private val inventoryScheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (player.offHandStack.isOf(Items.ENDER_CHEST)) return@handler

        val fromSlot = (Slots.Hotbar + Slots.Inventory).findSlot(Items.ENDER_CHEST) ?: return@handler

        val actions = buildList(3) {
            this += InventoryAction.Click.performPickup(slot = fromSlot)
            this += InventoryAction.Click.performPickup(slot = OffHandSlot)
            if (!OffHandSlot.itemStack.isEmpty) this += InventoryAction.Click.performPickup(slot = fromSlot)
        }

        event.schedule(inventoryConstraints, actions)
    }

    @Suppress("unused")
    private val keybindIsPressedHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding != mc.options.attackKey || mc.attackCooldown > 0) return@handler

        val hit = mc.crosshairTarget as? BlockHitResult ?: return@handler
        val state = hit.blockPos.getState() ?: return@handler
        val isTargetEChest = state.block == Blocks.ENDER_CHEST

        if (canUseOffhand()) {
            KeyBinding.onKeyPressed(mc.options.useKey.boundKey)
            return@handler
        }

        if (!isTargetEChest) return@handler

        if (!interaction.isBreakingBlock) {
            KeyBinding.onKeyPressed(mc.options.attackKey.boundKey)
        }
        event.isPressed = true
    }
}
