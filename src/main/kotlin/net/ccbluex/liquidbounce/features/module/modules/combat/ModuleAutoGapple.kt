package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.client.util.InputUtil
import net.minecraft.item.Items

object ModuleAutoGapple : Module("AutoGapple", Category.PLAYER) {

    val health by int("Health", 18, 1..20)

    var prevSlot = -1
    var eating = false

    override fun disable() {
        if (!InputUtil.isKeyPressed(mc.window.handle, mc.options.keyUse.boundKey.code)) {
            mc.options.keyUse.isPressed = false
        }
    }

    val repeatable = repeatable {
        val slot = (0..8).firstOrNull {
            player.inventory.getStack(it).item == Items.GOLDEN_APPLE
        } ?: return@repeatable

        if (player.isDead) {
            return@repeatable
        }

        if (eating && player.health + player.absorptionAmount >= health) {
            eating = false
            mc.options.keyUse.isPressed = false
            player.inventory.selectedSlot = prevSlot
        }

        if (player.health < health) {
            prevSlot = player.inventory.selectedSlot
            player.inventory.selectedSlot = slot
            eating = true
            mc.options.keyUse.isPressed = true
        }
    }
}
