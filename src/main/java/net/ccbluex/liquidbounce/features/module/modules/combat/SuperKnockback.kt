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
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.*

object SuperKnockback : Module("SuperKnockback", ModuleCategory.COMBAT) {

    private val delay by IntegerValue("Delay", 0, 0, 500)
    private val hurtTime by IntegerValue("HurtTime", 10, 0, 10)
    private val mode by ListValue("Mode", arrayOf("Legit", "Old", "Silent", "Packet", "SneakPacket"), "Old")
    private val onlyGround by BoolValue("OnlyGround", false)

    private val onlyMove by BoolValue("OnlyMove", true)
    private val onlyMoveForward by BoolValue("OnlyMoveForward", true) { onlyMove }

    private var ticks = 0

    private val timer = MSTimer()

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity !is EntityLivingBase)
            return

        if (event.targetEntity.hurtTime > hurtTime || !timer.hasTimePassed(delay) || (onlyGround && !mc.thePlayer.onGround))
            return

        if (onlyMove && (!isMoving || (onlyMoveForward && mc.thePlayer.movementInput.moveStrafe != 0f)))
            return

        when (mode) {
            "Old" -> {
                // Users reported that this mode is better than the other ones

                if (mc.thePlayer.isSprinting) {
                    sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
                }

                sendPackets(
                    C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING),
                    C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING),
                    C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING)
                )
                mc.thePlayer.isSprinting = true
                mc.thePlayer.serverSprintState = true
            }

            "Legit", "Silent" -> ticks = 2

            "Packet" -> {
                sendPackets(
                    C0BPacketEntityAction(mc.thePlayer, STOP_SPRINTING),
                    C0BPacketEntityAction(mc.thePlayer, START_SPRINTING)
                )
            }

            "SneakPacket" -> {
                sendPackets(
                    C0BPacketEntityAction(mc.thePlayer, STOP_SPRINTING),
                    C0BPacketEntityAction(mc.thePlayer, START_SNEAKING),
                    C0BPacketEntityAction(mc.thePlayer, START_SPRINTING),
                    C0BPacketEntityAction(mc.thePlayer, STOP_SNEAKING)
                )
            }
        }

        timer.reset()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (mode == "Legit") {
            if (ticks == 2) {
                mc.thePlayer.isSprinting = false
                ticks--
            } else if (ticks == 1) {
                mc.thePlayer.isSprinting = true
                ticks--
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        if (packet is C03PacketPlayer && mode == "Silent") {
            if (ticks == 2) {
                sendPacket(C0BPacketEntityAction(mc.thePlayer, STOP_SPRINTING))
                ticks--
            } else if (ticks == 1) {
                sendPacket(C0BPacketEntityAction(mc.thePlayer, START_SPRINTING))
                ticks--
            }
        }
    }

    override val tag
        get() = mode
}