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
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.extensions.sendPacketWithoutEvent
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import java.util.*
import kotlin.random.Random

@ModuleInfo(name = "AutoLeave", description = "Automatically makes you leave the server whenever your health is low.", category = ModuleCategory.COMBAT)
class AutoLeave : Module()
{
    private val healthValue = FloatValue("Health", 8f, 0f, 20f)
    private val modeValue = ListValue("Mode", arrayOf("Quit", "InvalidPacket", "SelfHurt", "IllegalChat"), "Quit")

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val netHandler = mc.netHandler

        if (thePlayer.health <= healthValue.get() && !thePlayer.capabilities.isCreativeMode && !mc.isIntegratedServerRunning)
        {
            when (modeValue.get().lowercase(Locale.getDefault()))
            {
                "quit" -> theWorld.sendQuittingDisconnectingPacket()
                "invalidpacket" -> netHandler.networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(Double.NaN, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, !thePlayer.onGround))
                "selfhurt" -> netHandler.networkManager.sendPacketWithoutEvent(C02PacketUseEntity(thePlayer, C02PacketUseEntity.Action.ATTACK))
                "illegalchat" -> thePlayer.sendChatMessage("${Random.nextInt()}\u00A7\u00A7\u00A7${Random.nextInt()}")
            }

            state = false
        }
    }

    override val tag: String
        get() = "${modeValue.get()}, ${healthValue.get()}"
}
