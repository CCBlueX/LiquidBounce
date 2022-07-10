/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.minecraft.network.play.client.C01PacketChatMessage
import java.util.concurrent.LinkedBlockingQueue

@ModuleInfo(name = "AtAllProvider", description = "Automatically mentions everyone on the server when using '@a' in your message.", category = ModuleCategory.MISC)
class AtAllProvider : Module()
{
    private val delayValue = IntegerRangeValue("Delay", 500, 1000, 0, 20000, "MaxDelay" to "MinDelay")

    private val retryValue = BoolValue("Retry", false)
    private val sendQueue = LinkedBlockingQueue<String>()
    private val retryQueue: MutableList<String> = ArrayList()
    private val msTimer = MSTimer()
    private var delay = delayValue.getRandomLong()

    override fun onDisable()
    {
        synchronized(sendQueue, sendQueue::clear)
        synchronized(retryQueue, retryQueue::clear)

        super.onDisable()
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        if (!msTimer.hasTimePassed(delay)) return
        val thePlayer = mc.thePlayer ?: return

        try
        {
            synchronized(sendQueue) {
                if (sendQueue.isEmpty())
                {
                    if (!retryValue.get() || retryQueue.isEmpty()) return@onUpdate

                    sendQueue.addAll(retryQueue)
                }

                thePlayer.sendChatMessage(sendQueue.take())
                msTimer.reset()

                delay = delayValue.getRandomLong()
            }
        }
        catch (e: InterruptedException)
        {
            e.printStackTrace()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        val packet = event.packet
        if (packet is C01PacketChatMessage)
        {
            val message = packet.message
            if (message.contains("@a"))
            {
                synchronized(sendQueue) {
                    mc.netHandler.playerInfoMap.map { it.gameProfile.name }.filter { it != thePlayer.name }.mapTo(sendQueue) { message.replace("@a", it) }

                    if (retryValue.get())
                    {
                        synchronized(retryQueue) {
                            retryQueue.clear()
                            retryQueue += sendQueue
                        }
                    }
                }
                event.cancelEvent()
            }
        }
    }
}
