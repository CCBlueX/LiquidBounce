/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.Vec3

object LagRange : Module("LagRange", Category.COMBAT) {

    private val onlyOnKillAura by BoolValue("OnlyOnKillAura", true)
    private val freezeTime by IntegerValue("FreezeTime", 100, 10..1000)
    private val delay by IntegerValue("Delay", 1500, 100..10000)

    private val maxRangeToAttack: FloatValue = object : FloatValue("MaxRangeToAttack", 5.0f, 0f..10f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minRangeToAttack.get())
    }
    private val minRangeToAttack: FloatValue = object : FloatValue("MinRangeToAttack", 3.0f, 0f..10f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxRangeToAttack.get())
    }
    private val onlyOnGround by BoolValue("OnlyOnGround", false)

    private val lastFreeze: Long = 0

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (!shouldStart) return

        Thread.sleep(lagTime.get())
        lastLagTime = System.currentTimeMillis()
    }

    private val shouldStart() {
        if (!isMoving || System.currentTimeMillis() - lastLagTime < delay.get() || (onlyOnGround.get() && !mc.thePlayer.onGround)) return false
        mc.thePlayer ?: return false
    }

    private fun getNearestEntityInRange(): EntityLivingBase? {
        val player = mc.thePlayer ?: return null

        return mc.theWorld?.loadedEntityList
            ?.filterIsInstance<EntityLivingBase>()
            ?.filter { EntityUtils.isSelected(it, true) }
            ?.minByOrNull { player.getDistanceToEntity(it) }
    }
}
