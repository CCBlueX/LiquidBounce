/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction

object SuperKnockback : Module("SuperKnockback", ModuleCategory.COMBAT) {

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)
    private val modeValue = ListValue("Mode", arrayOf("Legit", "Silent", "Packet", "SneakPacket"), "Silent")
    private val onlyGroundValue = BoolValue("OnlyGround", false)
    private val delayValue = IntegerValue("Delay", 0, 0, 500)
    private val onlyMoveValue = BoolValue("OnlyMove", true)
    private val onlyMoveForwardValue = object : BoolValue("OnlyMoveForward", true) {
        override fun isSupported() = onlyMoveValue.get()
    }

    private var ticks = 0

    val timer = MSTimer()

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            if (event.targetEntity.hurtTime > hurtTimeValue.get() || !timer.hasTimePassed(delayValue.get().toLong()) ||
                    (!MovementUtils.isMoving && onlyMoveValue.get()) || (!mc.thePlayer.onGround && onlyGroundValue.get())) {
                return
            }

            if (onlyMoveForwardValue.get() && RotationUtils.getRotationDifference(Rotation(MovementUtils.movingYaw, mc.thePlayer.rotationPitch), Rotation(mc.thePlayer.rotationYaw, mc.thePlayer.rotationPitch)) > 35) {
                return
            }

            when (modeValue.get().lowercase()) {

                "legit" -> {
                    ticks = 2
                }

                "silent" -> {
                    ticks = 1
                }

                "packet" -> {
                    if (mc.thePlayer.isSprinting) {
                        mc.thePlayer.isSprinting = true
                    }
                    sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                    sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                    mc.thePlayer.serverSprintState = true
                }

                "sneakpacket" -> {
                    if (mc.thePlayer.isSprinting) {
                        mc.thePlayer.isSprinting = true
                    }
                    sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                    sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
                    sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                    sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
                    mc.thePlayer.serverSprintState = true
                }
            }
            timer.reset()
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (modeValue.equals("Legit")) {
            if (ticks == 2) {
                mc.thePlayer.isSprinting = false
                ticks = 1
            } else if (ticks == 1) {
                mc.thePlayer.isSprinting = true
                ticks = 0
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if ((packet is C03PacketPlayer.C04PacketPlayerPosition || packet is C03PacketPlayer.C06PacketPlayerPosLook) && modeValue.equals("Silent")) {
            if (ticks == 1) {
                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                ticks = 2
            } else if (ticks == 2) {
                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
                ticks = 0
            }
        }
    }

    override val tag: String
        get() = modeValue.get()
}