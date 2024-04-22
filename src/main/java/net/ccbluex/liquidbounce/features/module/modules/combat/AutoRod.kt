/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.EntityUtils.getHealth
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RaycastUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items

object AutoRod : Module("AutoRod", ModuleCategory.COMBAT, hideModule = false) {

    private val facingEnemy by BoolValue("FacingEnemy", true)

    private val ignoreOnEnemyLowHealth by BoolValue("IgnoreOnEnemyLowHealth", true) { facingEnemy }
        private val healthFromScoreboard by BoolValue("HealthFromScoreboard", false) { facingEnemy && ignoreOnEnemyLowHealth }
        private val absorption by BoolValue("Absorption", false) { facingEnemy && ignoreOnEnemyLowHealth }

    private val activationDistance by FloatValue("ActivationDistance", 8f, 1f..20f)
    private val enemiesNearby by IntegerValue("EnemiesNearby", 1, 1..5)

    // Improve health check customization
    private val playerHealthThreshold by IntegerValue("PlayerHealthThreshold", 5, 1..20)
    private val enemyHealthThreshold by IntegerValue("EnemyHealthThreshold", 5, 1..20) { facingEnemy && ignoreOnEnemyLowHealth }
    private val escapeHealthThreshold by IntegerValue("EscapeHealthThreshold", 10, 1..20)

    private val pushDelay by IntegerValue("PushDelay", 100, 50..1000)
    private val pullbackDelay by IntegerValue("PullbackDelay", 500, 50..1000)

    private val onUsingItem by BoolValue("OnUsingItem", false)

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

            if (facingEnemy && getHealth(mc.thePlayer, healthFromScoreboard, absorption) >= playerHealthThreshold) {
                var facingEntity = mc.objectMouseOver?.entityHit
                val nearbyEnemies = getAllNearbyEnemies()

                if (facingEntity == null) {
                    // Check if player is looking at enemy.
                    facingEntity = RaycastUtils.raycastEntity(activationDistance.toDouble()) { isSelected(it, true) }
                }

                // Check whether player is using items/blocking.
                if (!onUsingItem) {
                    if (mc.thePlayer?.itemInUse?.item != Items.fishing_rod && (mc.thePlayer?.isUsingItem == true || KillAura.blockStatus)) {
                        return
                    }
                }

                if (isSelected(facingEntity, true)) {
                    // Checks how many enemy is nearby, if <= then should rod.
                    if (nearbyEnemies?.size!! <= enemiesNearby) {

                        // Check if the enemy's health is below the threshold.
                        if (ignoreOnEnemyLowHealth) {
                            if (getHealth(facingEntity as EntityLivingBase, healthFromScoreboard, absorption) >= enemyHealthThreshold) {
                                rod = true
                            }
                        } else {
                            rod = true
                        }
                    }
                }
            } else if (getHealth(mc.thePlayer, healthFromScoreboard, absorption) <= escapeHealthThreshold) {
                // use rod for escaping when health is low.
                rod = true
            } else if (!facingEnemy) {
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

    private fun getAllNearbyEnemies(): List<Entity>? {
        val player = mc.thePlayer ?: return null

        return mc.theWorld.loadedEntityList.toList()
            .filter { isSelected(it, true) }
            .filter { player.getDistanceToEntityBox(it) < activationDistance }
    }

}