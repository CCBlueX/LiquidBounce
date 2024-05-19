/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other

import net.ccbluex.liquidbounce.features.module.modules.movement.Speed.mineplexGroundSpeed
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.extensions.onPlayerRightClick
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3

object MineplexGround : SpeedMode("MineplexGround") {
    private var spoofSlot = false
    private var speed = 0f

    override fun onMotion() {
        if (!isMoving || !player.onGround || player.heldItem == null || player.isUsingItem) return
        spoofSlot = false
        for (i in 36..44) {
            val itemStack = player.inventory.getStackInSlot(i)
            if (itemStack != null) continue

            serverSlot = i - 36
            spoofSlot = true
            break
        }
    }

    override fun onUpdate() {
        if (!isMoving || !player.onGround || player.isUsingItem) {
            speed = 0f
            return
        }
        if (!spoofSlot && player.heldItem != null) {
            displayChatMessage("§8[§c§lMineplex§aSpeed§8] §cYou need one empty slot.")
            return
        }

        val blockPos = BlockPos(player).down()
        val vec = Vec3(blockPos).addVector(0.4, 0.4, 0.4) + Vec3(EnumFacing.UP.directionVec)

        player.onPlayerRightClick(blockPos, EnumFacing.UP, Vec3(vec.xCoord * 0.4f, vec.yCoord * 0.4f, vec.zCoord * 0.4f))

        speed = (speed + mineplexGroundSpeed / 8).coerceAtMost(mineplexGroundSpeed)

        strafe(speed)

        if (!spoofSlot) serverSlot = player.inventory.currentItem
    }

    override fun onDisable() {
        speed = 0f
        serverSlot = player.inventory.currentItem
    }
}