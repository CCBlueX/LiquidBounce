/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.other

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MovementUtils

class MineplexGround : SpeedMode("MineplexGround") {
    private var spoofSlot = false
    private var speed = 0f

    override fun onMotion() {
        if (!MovementUtils.isMoving || !mc.thePlayer!!.onGround || mc.thePlayer!!.inventory.getCurrentItemInHand() == null || mc.thePlayer!!.isUsingItem) return
        spoofSlot = false
        for (i in 36..44) {
            val itemStack = mc.thePlayer!!.inventory.getStackInSlot(i)
            if (itemStack != null) continue
            mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(i - 36))
            spoofSlot = true
            break
        }
    }

    override fun onUpdate() {
        if (!MovementUtils.isMoving || !mc.thePlayer!!.onGround || mc.thePlayer!!.isUsingItem) {
            speed = 0f
            return
        }
        if (!spoofSlot && mc.thePlayer!!.inventory.getCurrentItemInHand() != null) {
            ClientUtils.displayChatMessage("§8[§c§lMineplex§aSpeed§8] §cYou need one empty slot.")
            return
        }
        val blockPos = WBlockPos(mc.thePlayer!!.posX, mc.thePlayer!!.entityBoundingBox.minY - 1, mc.thePlayer!!.posZ)
        val vec = WVec3(blockPos).addVector(0.4, 0.4, 0.4).add(WVec3(classProvider.getEnumFacing(EnumFacingType.UP).directionVec))
        mc.playerController.onPlayerRightClick(mc.thePlayer!!, mc.theWorld!!, null, blockPos, classProvider.getEnumFacing(EnumFacingType.UP), WVec3(vec.xCoord * 0.4f, vec.yCoord * 0.4f, vec.zCoord * 0.4f))
        val targetSpeed = (LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed?)!!.mineplexGroundSpeedValue.get()
        if (targetSpeed > speed) speed += targetSpeed / 8
        if (speed >= targetSpeed) speed = targetSpeed
        MovementUtils.strafe(speed)
        if (!spoofSlot) mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
    }

    override fun onMove(event: MoveEvent) {}
    override fun onDisable() {
        speed = 0f
        mc.netHandler.addToSendQueue(classProvider.createCPacketHeldItemChange(mc.thePlayer!!.inventory.currentItem))
    }
}