/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speeds.ncp

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speeds.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.init.Blocks
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt

class YPort : SpeedMode("YPort") {
    private var moveSpeed = 0.2873
    private var level = 1
    private var lastDist = 0.0
    private var timerDelay = 0
    private var safeJump = false
    override fun onMotion() {
        if (!safeJump && !mc.gameSettings.keyBindJump.isKeyDown && !mc.thePlayer!!.isOnLadder && !mc.thePlayer!!.isInsideOfMaterial(
                Material.water) && !mc.thePlayer!!.isInsideOfMaterial(Material.lava) && !mc.thePlayer!!.isInWater && (this.getBlock(-1.1) != Blocks.air && this.getBlock(-1.1) != Blocks.air || this.getBlock(-0.1) != Blocks.air && mc.thePlayer!!.motionX != 0.0 && mc.thePlayer!!.motionZ != 0.0 && !mc.thePlayer!!.onGround && mc.thePlayer!!.fallDistance < 3.0f && mc.thePlayer!!.fallDistance > 0.05) && level == 3) mc.thePlayer!!.motionY = -0.3994
        val xDist = mc.thePlayer!!.posX - mc.thePlayer!!.prevPosX
        val zDist = mc.thePlayer!!.posZ - mc.thePlayer!!.prevPosZ
        lastDist = sqrt(xDist * xDist + zDist * zDist)
        if (!MovementUtils.isMoving) safeJump = true else if (mc.thePlayer!!.onGround) safeJump = false
    }

    override fun onUpdate() {}
    override fun onMove(event: MoveEvent) {
        timerDelay += 1
        timerDelay %= 5
        if (timerDelay != 0) {
            mc.timer.timerSpeed = 1f
        } else {
            if (MovementUtils.hasMotion()) mc.timer.timerSpeed = 32767f
            if (MovementUtils.hasMotion()) {
                mc.timer.timerSpeed = 1.3f
                mc.thePlayer!!.motionX *= 1.0199999809265137
                mc.thePlayer!!.motionZ *= 1.0199999809265137
            }
        }
        if (mc.thePlayer!!.onGround && MovementUtils.hasMotion()) level = 2
        if (round(mc.thePlayer!!.posY - mc.thePlayer!!.posY.toInt()) == round(0.138)) {
            mc.thePlayer!!.motionY -= 0.08
            event.y = event.y - 0.09316090325960147
            mc.thePlayer!!.posY -= 0.09316090325960147
        }
        if (level == 1 && (mc.thePlayer!!.moveForward != 0.0f || mc.thePlayer!!.moveStrafing != 0.0f)) {
            level = 2
            moveSpeed = 1.38 * baseMoveSpeed - 0.01
        } else if (level == 2) {
            level = 3
            mc.thePlayer!!.motionY = 0.399399995803833
            event.y = 0.399399995803833
            moveSpeed *= 2.149
        } else if (level == 3) {
            level = 4
            val difference = 0.66 * (lastDist - baseMoveSpeed)
            moveSpeed = lastDist - difference
        } else {
            if (mc.theWorld!!.getCollidingBoundingBoxes(mc.thePlayer!!, mc.thePlayer!!.entityBoundingBox.offset(0.0, mc.thePlayer!!.motionY, 0.0)).size > 0 || mc.thePlayer!!.isCollidedVertically) level = 1
            moveSpeed = lastDist - lastDist / 159.0
        }
        moveSpeed = moveSpeed.coerceAtLeast(baseMoveSpeed)
        var forward: Float = mc.thePlayer!!.movementInput.moveForward
        var strafe: Float = mc.thePlayer!!.movementInput.moveStrafe
        var yaw = mc.thePlayer!!.rotationYaw
        if (forward == 0f && strafe == 0f) {
            event.x = 0.0
            event.z = 0.0
        } else if (forward != 0f) {
            if (strafe >= 1f) {
                yaw += (if (forward > 0f) -45 else 45).toFloat()
                strafe = 0f
            } else if (strafe <= -1.0f) {
                yaw += (if (forward > 0f) 45 else -45).toFloat()
                strafe = 0f
            }
            if (forward > 0f) forward = 1f else if (forward < 0f) forward = -1f
        }
        val mx = cos(Math.toRadians(yaw + 90.0f.toDouble()))
        val mz = sin(Math.toRadians(yaw + 90.0f.toDouble()))
        event.x = forward * moveSpeed * mx + strafe * moveSpeed * mz
        event.z = forward * moveSpeed * mz - strafe * moveSpeed * mx
        mc.thePlayer!!.stepHeight = 0.6f
        if (forward == 0f && strafe == 0f) {
            event.x = 0.0
            event.z = 0.0
        }
    }

    private val baseMoveSpeed: Double
        get() {
            var baseSpeed = 0.2873
            if (mc.thePlayer!!.isPotionActive(Potion.moveSpeed)) {
                val amplifier: Int = mc.thePlayer!!.getActivePotionEffect(Potion.moveSpeed)!!.amplifier
                baseSpeed *= 1.0 + 0.2 * (amplifier + 1)
            }
            return baseSpeed
        }

    private fun getBlock(axisAlignedBB: AxisAlignedBB): Block? {
        for (x in floor(axisAlignedBB.minX).toInt() until floor(axisAlignedBB.maxX).toInt() + 1) {
            for (z in floor(axisAlignedBB.minZ).toInt() until floor(axisAlignedBB.maxZ).toInt() + 1) {
                val block = mc.theWorld!!.getBlockState(BlockPos(x, axisAlignedBB.minY.toInt(), z)).block

                if (block != null)
                    return block
            }
        }
        return null
    }

    private fun getBlock(offset: Double): Block? {
        return this.getBlock(mc.thePlayer!!.entityBoundingBox.offset(0.0, offset, 0.0))
    }

    private fun round(value: Double): Double {
        var bd = BigDecimal(value)
        bd = bd.setScale(3, RoundingMode.HALF_UP)
        return bd.toDouble()
    }
}