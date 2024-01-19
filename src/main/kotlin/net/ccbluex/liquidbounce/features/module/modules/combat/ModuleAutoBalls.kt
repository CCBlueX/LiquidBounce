package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.box
import net.ccbluex.liquidbounce.utils.item.findHotbarSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.math.Vec3d

object ModuleAutoBalls : Module("AutoBalls", Category.COMBAT) {

    val range by float("Range", 30f, 5f..60f)

    val clickScheduler = tree(ClickScheduler(ModuleAutoBalls, showCooldown = false))

    val targetTracker = tree(TargetTracker())
    val rotationsConfigurable = tree(RotationsConfigurable())

    val considerInventory by boolean("ConsiderInventory", true)

    val hasSnowballs: Boolean
        get() = findHotbarSlot(Items.SNOWBALL) != null


    val handleSimulatedTick = handler<SimulatedTickEvent> {
        targetTracker.cleanup()

        if (!hasSnowballs) {
            return@handler
        }

        for (enemy in targetTracker.enemies()) {
            if (player.canSee(enemy) && player.distanceTo(enemy) <= range) {
                val box = enemy.box

                // Center body but aim at head
                val vec3 = Vec3d(
                    box.minX + (box.maxX - box.minX) * 0.5,
                    box.maxY,
                    box.minZ + (box.maxZ - box.minZ) * 0.5
                )

                // todo: calculate actual gravity of snowball to find the right angle
                val rotation = RotationManager.makeRotation(vec3, player.eyePos)

                RotationManager.aimAt(rotationsConfigurable.toAimPlan(rotation, considerInventory),
                    Priority.IMPORTANT_FOR_USAGE_1, this)
                targetTracker.lock(enemy)
                break
            }
        }
    }

    val repeatable = repeatable {
        targetTracker.lockedOnTarget ?: return@repeatable

        val snowballOffHand = player.offHandStack.item == Items.SNOWBALL
        val snowballMainHand = player.mainHandStack.item == Items.SNOWBALL

        if (!snowballMainHand && !snowballOffHand) {
            val snowballs = findHotbarSlot(Items.SNOWBALL) ?: return@repeatable
            SilentHotbar.selectSlotSilently(this, snowballs)
            waitTicks(1)
        }

        clickScheduler.clicks {
            if (player.isUsingItem) {
                return@clicks false
            }

            interaction.interactItem(player, if (snowballOffHand) Hand.OFF_HAND else Hand.MAIN_HAND).isAccepted
        }
    }

}
