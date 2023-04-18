/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.play.client.C0BPacketEntityAction

object Sneak : Module("Sneak", ModuleCategory.MOVEMENT) {

    val modeValue = ListValue("Mode", arrayOf("Legit", "Vanilla", "Switch", "MineSecure"), "MineSecure")
    val stopMoveValue = BoolValue("StopMove", false)

    private var sneaking = false

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (stopMoveValue.get() && isMoving) {
            if (sneaking)
                onDisable()
            return
        }

        when (modeValue.get().lowercase()) {
            "legit" -> mc.gameSettings.keyBindSneak.pressed = true
            "vanilla" -> {
                if (sneaking)
                    return

                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
            }

            "switch" -> {
                when (event.eventState) {
                    EventState.PRE -> {
                        sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
                        sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
                    }
                    EventState.POST -> {
                        sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
                        sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
                    }
                }
            }

            "minesecure" -> {
                if (event.eventState == EventState.PRE)
                    return

                sendPacket(C0BPacketEntityAction(mc.thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
            }
        }
    }

    @EventTarget
    fun onWorld(worldEvent: WorldEvent) {
        sneaking = false
    }

    override fun onDisable() {
        val player = mc.thePlayer ?: return

        when (modeValue.get().lowercase()) {
            "legit" -> {
                if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
                    mc.gameSettings.keyBindSneak.pressed = false
                }
            }
            "vanilla", "switch", "minesecure" -> sendPacket(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SNEAKING))
        }
        sneaking = false
    }
}