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
import net.ccbluex.liquidbounce.utils.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.RaycastUtils.raycastEntity
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.init.Items.egg
import net.minecraft.init.Items.snowball

object AutoProjectile : Module("AutoProjectile", ModuleCategory.COMBAT) {
    private val facingEnemy by BoolValue("FacingEnemy", true)

    private val mode by ListValue("Mode", arrayOf("Normal", "Smart"), "Normal")
    private val range by FloatValue("Range", 8F, 1F..20F)
    private val throwDelay by IntegerValue("ThrowDelay", 1000, 50..2000) { mode != "Smart" }

    private val minThrowDelay: IntegerValue = object : IntegerValue("MinThrowDelay", 1000, 50..2000) {
        override fun isSupported() = mode == "Smart"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtMost(maxThrowDelay.get())
    }

    private val maxThrowDelay: IntegerValue = object : IntegerValue("MaxThrowDelay", 1500, 50..2000) {
        override fun isSupported() = mode == "Smart"
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minThrowDelay.get())
    }

    private val switchBackDelay by IntegerValue("SwitchBackDelay", 500, 50..2000)

    private val throwTimer = MSTimer()
    private val projectilePullTimer = MSTimer()

    private var projectileInUse = false
    private var switchBack = -1

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val usingProjectile = (mc.thePlayer.isUsingItem && (mc.thePlayer.heldItem?.item == snowball || mc.thePlayer.heldItem?.item == egg)) || projectileInUse

        if (usingProjectile) {
            if (projectilePullTimer.hasTimePassed(switchBackDelay)) {
                if (switchBack != -1 && mc.thePlayer.inventory.currentItem != switchBack) {
                    mc.thePlayer.inventory.currentItem = switchBack

                    mc.playerController.updateController()
                } else {
                    mc.thePlayer.stopUsingItem()
                }

                switchBack = -1
                projectileInUse = false

                throwTimer.reset()
            }
        } else {
            var throwProjectile = false

            if (facingEnemy) {
                var facingEntity = mc.objectMouseOver?.entityHit

                if (facingEntity == null) {
                    facingEntity = raycastEntity(range.toDouble()) { isSelected(it, true) }
                }

                if (isSelected(facingEntity, true)) {
                    throwProjectile = true
                }
            } else {
                throwProjectile = true
            }

            if (throwProjectile) {
                if (mode == "Normal" && throwTimer.hasTimePassed(throwDelay)) {
                    if (mc.thePlayer.heldItem?.item != snowball && mc.thePlayer.heldItem?.item != egg) {
                        val projectile = findProjectile(36, 45)

                        if (projectile == -1) {
                            return
                        }

                        switchBack = mc.thePlayer.inventory.currentItem

                        mc.thePlayer.inventory.currentItem = projectile - 36
                        mc.playerController.updateController()
                    }

                    throwProjectile()
                }

                val randomThrowDelay = RandomUtils.nextInt(minThrowDelay.get(), maxThrowDelay.get())
                if (mode == "Smart" && throwTimer.hasTimePassed(randomThrowDelay)) {
                    if (mc.thePlayer.heldItem?.item != snowball && mc.thePlayer.heldItem?.item != egg) {
                        val projectile = findProjectile(36, 45)

                        if (projectile == -1) {
                            return
                        }

                        switchBack = mc.thePlayer.inventory.currentItem

                        mc.thePlayer.inventory.currentItem = projectile - 36
                        mc.playerController.updateController()
                    }

                    throwProjectile()
                }
            }
        }
    }

    /**
     * Throw projectile (snowball/egg)
     */
    private fun throwProjectile() {
        val projectile = findProjectile(36, 45)

        mc.thePlayer.inventory.currentItem = projectile - 36

        mc.playerController.sendUseItem(mc.thePlayer, mc.theWorld, mc.thePlayer.inventoryContainer.getSlot(projectile).stack)

        projectileInUse = true
        projectilePullTimer.reset()
    }

    /**
     * Find projectile (snowball/egg) in inventory
     */
    private fun findProjectile(startSlot: Int, endSlot: Int): Int {
        for (i in startSlot until endSlot) {
            val stack = mc.thePlayer?.inventoryContainer?.getSlot(i)?.stack
            if (stack != null) {
                if (stack.item == snowball || stack.item == egg) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * Reset everything when disabled
     */
    override fun onDisable() {
        throwTimer.reset()
        projectilePullTimer.reset()
        projectileInUse = false
        switchBack = -1
    }

    /**
     * HUD Tag
     */
    override val tag
        get() = mode
}
