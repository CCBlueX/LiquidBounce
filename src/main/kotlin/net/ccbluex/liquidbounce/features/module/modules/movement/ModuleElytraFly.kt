package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.extensions.moving
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.Items

object ModuleElytraFly : Module("ElytraFly", Category.MOVEMENT) {

    private val instant by boolean("Instant", true)
    private val speed by boolean("Speed", true)
    private val vertical by float("Vertical", 0.5f, 0.1f..2f)
    private val horizontal by float("Horizontal", 1f, 0.1f..2f)

    val repeatable = repeatable {
        // Find the chest slot
        val chestSlot = player.getEquippedStack(EquipmentSlot.CHEST)

        if (player.abilities.creativeMode) {
            return@repeatable
        }

        // If the player doesn't have an elytra in the chest slot
        if (chestSlot.item != Items.ELYTRA) {
            return@repeatable
        }

        // If dude's flying
        if (player.isFallFlying) {
            if (speed) {
                if (player.moving) {
                    player.strafe(speed = horizontal.toDouble())
                }
                player.velocity.y = when {
                    mc.options.keyJump.isPressed -> vertical.toDouble()
                    mc.options.keySneak.isPressed -> -vertical.toDouble()
                    else -> return@repeatable
                }
            }
            // If the player has an elytra and wants to fly instead
        } else if (chestSlot.item == Items.ELYTRA && mc.options.keyJump.isPressed) {
            if (instant) {
                player.isSprinting = true
                // Jump must be off due to abnormal speed boosts
                player.input.jumping = false
                player.jump()
                player.lastSprinting = true
            }
        }
    }
}
