/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0BPacketEntityAction

@ModuleInfo(name = "FreeCam", description = "Allows you to move out of your body.", category = ModuleCategory.RENDER)
class FreeCam : Module() {
    private val speedValue = FloatValue("Speed", 0.8f, 0.1f, 2f)
    private val flyValue = BoolValue("Fly", true)
    private val noClipValue = BoolValue("NoClip", true)

    private var fakePlayer: EntityOtherPlayerMP? = null

    private var oldX = 0.0
    private var oldY = 0.0
    private var oldZ = 0.0

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return

        oldX = thePlayer.posX
        oldY = thePlayer.posY
        oldZ = thePlayer.posZ

        val playerMP = EntityOtherPlayerMP(mc.theWorld, thePlayer.gameProfile)


        playerMP.rotationYawHead = thePlayer.rotationYawHead;
        playerMP.renderYawOffset = thePlayer.renderYawOffset;
        playerMP.rotationYawHead = thePlayer.rotationYawHead
        playerMP.copyLocationAndAnglesFrom(thePlayer)

        mc.theWorld!!.addEntityToWorld(-1000, playerMP)

        if (noClipValue.get())
            thePlayer.noClip = true

        fakePlayer = playerMP
    }

    override fun onDisable() {
        val thePlayer = mc.thePlayer

        if (thePlayer == null || fakePlayer == null)
            return

        thePlayer.setPositionAndRotation(oldX, oldY, oldZ, thePlayer.rotationYaw, thePlayer.rotationPitch)

        mc.theWorld!!.removeEntityFromWorld(fakePlayer!!.entityId)
        fakePlayer = null

        thePlayer.motionX = 0.0
        thePlayer.motionY = 0.0
        thePlayer.motionZ = 0.0
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        val thePlayer = mc.thePlayer!!

        if (noClipValue.get())
            thePlayer.noClip = true

        thePlayer.fallDistance = 0.0f

        if (flyValue.get()) {
            val value = speedValue.get()

            thePlayer.motionY = 0.0
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0

            if (mc.gameSettings.keyBindJump.isKeyDown)
                thePlayer.motionY += value

            if (mc.gameSettings.keyBindSneak.isKeyDown)
                thePlayer.motionY -= value

            MovementUtils.strafe(value)
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer || packet is C0BPacketEntityAction)
            event.cancelEvent()
    }
}