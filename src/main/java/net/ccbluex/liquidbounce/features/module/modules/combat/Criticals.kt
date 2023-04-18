/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

object Criticals : Module("Criticals", category = ModuleCategory.COMBAT) {

    val modeValue = ListValue("Mode", arrayOf("Packet", "NcpPacket", "NoGround", "Hop", "TPHop", "Jump", "LowJump", "Visual"), "Packet")
    val delayValue = IntegerValue("Delay", 0, 0, 500)
    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

    val msTimer = MSTimer()

    override fun onEnable() {
        if (modeValue.get() == "NoGround")
            mc.thePlayer.jump()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            val thePlayer = mc.thePlayer ?: return
            val entity = event.targetEntity

            if (!thePlayer.onGround || thePlayer.isOnLadder || thePlayer.isInWeb || thePlayer.isInWater ||
                    thePlayer.isInLava || thePlayer.ridingEntity != null || entity.hurtTime > hurtTimeValue.get() ||
                    Fly.state || !msTimer.hasTimePassed(delayValue.get()))
                return

            val x = thePlayer.posX
            val y = thePlayer.posY
            val z = thePlayer.posZ

            when (modeValue.get().lowercase()) {
                "packet" -> {
                    sendPacket(C04PacketPlayerPosition(x, y + 0.0625, z, true))
                    sendPacket(C04PacketPlayerPosition(x, y, z, false))
                    sendPacket(C04PacketPlayerPosition(x, y + 1.1E-5, z, false))
                    sendPacket(C04PacketPlayerPosition(x, y, z, false))
                    thePlayer.onCriticalHit(entity)
                }
                "ncppacket" -> {
                    sendPacket(C04PacketPlayerPosition(x, y + 0.11, z, false))
                    sendPacket(C04PacketPlayerPosition(x, y + 0.1100013579, z, false))
                    sendPacket(C04PacketPlayerPosition(x, y + 0.0000013579, z, false))
                    mc.thePlayer.onCriticalHit(entity)
                }
                "hop" -> {
                    thePlayer.motionY = 0.1
                    thePlayer.fallDistance = 0.1f
                    thePlayer.onGround = false
                }
                "tphop" -> {
                    sendPacket(C04PacketPlayerPosition(x, y + 0.02, z, false))
                    sendPacket(C04PacketPlayerPosition(x, y + 0.01, z, false))
                    thePlayer.setPosition(x, y + 0.01, z)
                }
                "jump" -> thePlayer.motionY = 0.42
                "lowjump" -> thePlayer.motionY = 0.3425
                "visual" -> thePlayer.onCriticalHit(entity)
            }

            msTimer.reset()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer && modeValue.get() == "NoGround")
            packet.onGround = false
    }

    override val tag
        get() = modeValue.get()
}
