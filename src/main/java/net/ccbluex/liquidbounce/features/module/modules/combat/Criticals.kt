/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.stats.StatList
import kotlin.random.Random

@ModuleInfo(name = "Criticals", description = "Automatically deals critical hits.", category = ModuleCategory.COMBAT)
class Criticals : Module()
{
    /**
     * Options
     */
    val modeValue = ListValue("Mode", arrayOf("Packet", "NCPPacket", "AACPacket", "NoGround", "Hop", "TPHop", "Jump", "LowJump", "FakeCollide", "TpCollide", "Custom", "Visual"), "Packet")

    private val delayValue = IntegerRangeValue("Delay", 0, 0, 0, 1000, "MaxDelay" to "MinDelay")

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
    private val hitChanceValue = IntegerValue("Chance", 100, 0, 100)

    private val customGroup = object : ValueGroup("Custom")
    {
        override fun showCondition() = modeValue.get().equals("Custom", ignoreCase = true)
    }
    private val customStepsValue = IntegerValue("Steps", 3, 2, 6, "Custom-Steps")

    private val customYStep1Value = object : FloatValue("Step1", 0.11F, 0f, 2.9f, "Custom-Step1")
    {
        override fun showCondition() = customStepsValue.get() >= 1
    }
    private val customYStep2Value = object : FloatValue("Step2", 0.1100013579F, 0f, 2.9f, "Custom-Step2")
    {
        override fun showCondition() = customStepsValue.get() >= 2
    }
    private val customYStep3Value = object : FloatValue("Step3", 0.0000013579F, 0f, 2.9f, "Custom-Step3")
    {
        override fun showCondition() = customStepsValue.get() >= 3
    }
    private val customYStep4Value = object : FloatValue("Step4", 0f, 0f, 2.9f, "Custom-Step4")
    {
        override fun showCondition() = customStepsValue.get() >= 4
    }
    private val customYStep5Value = object : FloatValue("Step5", 0f, 0f, 2.9f, "Custom-Step5")
    {
        override fun showCondition() = customStepsValue.get() >= 5
    }
    private val customYStep6Value = object : FloatValue("Step6", 0f, 0f, 2.9f, "Custom-Step6")
    {
        override fun showCondition() = customStepsValue.get() >= 6
    }

    /**
     * Delay Timer
     */
    private val delayTimer = MSTimer()
    private var nextDelay = delayValue.getRandomLong()

    init
    {
        customGroup.addAll(customStepsValue, customYStep1Value, customYStep2Value, customYStep3Value, customYStep4Value, customYStep5Value, customYStep6Value)
    }

    override fun onEnable()
    {
        if (modeValue.get().equals("NoGround", ignoreCase = true)) (mc.thePlayer ?: return).jump()
    }

    @EventTarget
    fun onAttack(event: AttackEvent)
    {
        val entity = (event.targetEntity ?: return)
        if (entity is EntityLivingBase)
        {
            val thePlayer = mc.thePlayer ?: return

            val attackPos = event.attackPos

            val networkManager = mc.netHandler.networkManager

            val chance = hitChanceValue.get()
            if (!thePlayer.onGround || thePlayer.isOnLadder || thePlayer.ridingEntity != null || entity.hurtTime > hurtTimeValue.get() || !(chance > 0 && Random.nextInt(100) <= chance) || LiquidBounce.moduleManager[Fly::class.java].state || !canCritical(thePlayer)) return

            val x = attackPos.xCoord
            val y = attackPos.yCoord
            val z = attackPos.zCoord

            val motion = (if (thePlayer.isMoving) thePlayer.motionX to thePlayer.motionZ else 0.0 to 0.0)

            when (modeValue.get().lowercase())
            {
                "packet" ->
                {
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.0625, z, true))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y, z, false))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.000011, z, false))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y, z, false))
                    thePlayer.onCriticalHit(entity)
                }

                "ncppacket" ->
                {
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.11, z, false))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.1100013579, z, false))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.0000013579, z, false))
                    thePlayer.onCriticalHit(entity)
                }

                "aacpacket" ->
                {
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.05250000001304, z, true))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.00150000001304, z, false))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.01400000001304, z, false))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.00150000001304, z, false))
                    thePlayer.onCriticalHit(entity)
                }

                "tphop" ->
                {
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.02, z, false))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + 0.01, z, false))
                    thePlayer.setPosition(x, y + 0.01, z)
                    thePlayer.onCriticalHit(entity)
                }

                "fakecollide" ->
                {
                    thePlayer.triggerAchievement(StatList.jumpStat)
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x + motion.first / 3, y + 0.20, z + motion.second / 3, false))
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x + motion.first / 1.5, y + 0.121600000013, z + motion.second / 1.5, false))
                    thePlayer.onCriticalHit(entity)
                }

                "tpcollide" ->
                {
                    thePlayer.triggerAchievement(StatList.jumpStat)
                    thePlayer.isAirBorne = true
                    thePlayer.motionY = 0.0
                    thePlayer.setPosition(x, y + 0.2, z)
                }

                "custom" ->
                {
                    val ystep = arrayOf(customYStep1Value.get(), customYStep2Value.get(), customYStep3Value.get(), customYStep4Value.get(), customYStep5Value.get(), customYStep6Value.get())

                    repeat(customStepsValue.get()) { networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(x, y + ystep[it], z, false)) }

                    thePlayer.onCriticalHit(entity)
                }

                "hop" ->
                {
                    thePlayer.motionY = 0.1
                    thePlayer.fallDistance = 0.1f
                    thePlayer.onGround = false
                }

                "jump" -> thePlayer.motionY = 0.42
                "lowjump" -> thePlayer.motionY = 0.3425
                "visual" -> thePlayer.onCriticalHit(entity)
            }

            delayTimer.reset()
            nextDelay = delayValue.getRandomLong()
        }
    }

    fun canCritical(thePlayer: Entity): Boolean = !thePlayer.isInWeb && !thePlayer.isInWater && !thePlayer.isInLava && delayTimer.hasTimePassed(nextDelay)

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet
        if (packet is C03PacketPlayer && modeValue.get().equals("NoGround", ignoreCase = true)) packet.onGround = false
    }

    override val tag: String
        get() = modeValue.get()
}
