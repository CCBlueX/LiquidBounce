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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.play.client.C0BPacketEntityAction

@ModuleInfo(name = "Sneak", description = "Automatically sneaks all the time.", category = ModuleCategory.MOVEMENT)
class Sneak : Module()
{
    @JvmField
    val modeValue = ListValue("Mode", arrayOf("Legit", "Vanilla", "Switch", "MineSecure"), "MineSecure")

    @JvmField
    val stopMoveValue = BoolValue("StopMove", false)

    private var sneaking = false

    @EventTarget
    fun onMotion(event: MotionEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (stopMoveValue.get() && thePlayer.isMoving)
        {
            if (sneaking) onDisable()
            return
        }

        val netHandler = mc.netHandler

        when (modeValue.get().toLowerCase())
        {
            "legit" -> mc.gameSettings.keyBindSneak.pressed = true

            "vanilla" ->
            {
                if (sneaking) return

                netHandler.addToSendQueue(C0BPacketEntityAction(thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
            }

            "switch" ->
            {
                when (event.eventState)
                {
                    EventState.PRE ->
                    {
                        netHandler.addToSendQueue(C0BPacketEntityAction(thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
                        netHandler.addToSendQueue(C0BPacketEntityAction(thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
                    }

                    EventState.POST ->
                    {
                        netHandler.addToSendQueue(C0BPacketEntityAction(thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
                        netHandler.addToSendQueue(C0BPacketEntityAction(thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
                    }
                }
            }

            "minesecure" ->
            {
                if (event.eventState == EventState.PRE) return

                netHandler.addToSendQueue(C0BPacketEntityAction(thePlayer, C0BPacketEntityAction.Action.START_SNEAKING))
            }
        }
    }

    @EventTarget
    fun onWorld(@Suppress("UNUSED_PARAMETER") worldEvent: WorldEvent)
    {
        sneaking = false
    }

    override fun onDisable()
    {
        val player = mc.thePlayer ?: return

        when (modeValue.get().toLowerCase())
        {
            "legit" ->
            {
                val gameSettings = mc.gameSettings
                if (!GameSettings.isKeyDown(gameSettings.keyBindSneak)) gameSettings.keyBindSneak.pressed = false
            }

            "vanilla", "switch", "minesecure" -> mc.netHandler.addToSendQueue(C0BPacketEntityAction(player, C0BPacketEntityAction.Action.STOP_SNEAKING))
        }

        sneaking = false
    }

    override val tag: String
        get() = modeValue.get()
}
