/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C0BPacketEntityAction

object SuperKnockback : Module("SuperKnockback", ModuleCategory.COMBAT) {

    private val hurtTimeValue = IntegerValue("HurtTime", 10, 0, 10)

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (event.targetEntity is EntityLivingBase) {
            if (event.targetEntity.hurtTime > hurtTimeValue.get())
                return

            if (mc.thePlayer.isSprinting)
                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))

            sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
            sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SPRINTING))
            sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SPRINTING))
            mc.thePlayer.isSprinting = true
            mc.thePlayer.serverSprintState = true
        }
    }

}