package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RaycastUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.init.Items

object AutoRod : Module("AutoRod", ModuleCategory.COMBAT) {

    private val facingEnemy by BoolValue("FacingEnemy", true)

    private val pushDelay by IntegerValue("PushDelay", 100, 50..1000)
    private val pullbackDelay by IntegerValue("PullbackDelay", 500, 50..1000)

    private val pushTimer = MSTimer()
    private val rodPullTimer = MSTimer()

    private var rodInUse = false
    private var switchBack = -1

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Check if player is using rod
        val usingRod = (mc.thePlayer.isUsingItem && mc.thePlayer.heldItem?.item == Items.fishing_rod) || rodInUse

        if (usingRod) {
            // Check if rod pull timer has reached delay
            // mc.thePlayer.fishEntity?.caughtEntity != null is always null

            if (rodPullTimer.hasTimePassed(pullbackDelay)) {
                if (switchBack != -1 && mc.thePlayer.inventory.currentItem != switchBack) {
                    // Switch back to previous item
                    mc.thePlayer.inventory.currentItem = switchBack
                    mc.playerController.updateController()
                } else {
                    // Stop using rod
                    mc.thePlayer.stopUsingItem()
                }

                switchBack = -1
                rodInUse = false

                // Reset push timer. Push will always wait for pullback delay.
                pushTimer.reset()
            }
        } else {
            var rod = false

            if (facingEnemy) {
                // Check if player is facing enemy
                var facingEntity = mc.objectMouseOver?.entityHit

                if (facingEntity == null) {
                    // Check if player is looking at enemy, 8 blocks should be enough
                    facingEntity = RaycastUtils.raycastEntity(8.0) { isSelected(it, true) }
                }

                if (isSelected(facingEntity, true)) {
                    rod = true
                }
            } else {
                // Rod anyway, spam it.
                rod = true
            }

            if (rod && pushTimer.hasTimePassed(pushDelay)) {
                // Check if player has rod in hand
                if (mc.thePlayer.heldItem?.item != Items.fishing_rod) {
                    // Check if player has rod in hotbar
                    val rod = findRod(36, 45)

                    if (rod == -1) {
                        // There is no rod in hotbar
                        return
                    }

                    // Switch to rod
                    switchBack = mc.thePlayer.inventory.currentItem

                    mc.thePlayer.inventory.currentItem = rod - 36
                    mc.playerController.updateController()
                }

                rod()
            }
        }
    }

    /**
     * Use rod
     */
    private fun rod() {
        val rod = findRod(36, 45)

        mc.thePlayer.inventory.currentItem = rod - 36
        // We do not need to send our own packet, because sendUseItem will handle it for us.
        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventoryContainer.getSlot(rod).stack)

        rodInUse = true
        rodPullTimer.reset()
    }

    /**
     * Find rod in inventory
     */
    private fun findRod(startSlot: Int, endSlot: Int): Int {
        for (i in startSlot until endSlot) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(i).stack
            if (stack != null && stack.item === Items.fishing_rod) {
                return i
            }
        }
        return -1
    }

}