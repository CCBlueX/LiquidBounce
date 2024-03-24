/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.utils.extensions.onPlayerRightClick
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.network.login.server.S00PacketDisconnect
import net.minecraft.network.play.server.S01PacketJoinGame
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

object FlagCheck : Module("FlagCheck", ModuleCategory.MISC, gameDetecting = true) {

    private val resetFlagCounterTicks by IntegerValue("ResetCounterTicks", 600, 100..1000)
    private val rubberbandCheck by BoolValue("RubberbandCheck", false)
    private val rubberbandThreshold by FloatValue("RubberBandThreshold", 5.0f, 0.05f..10.0f) { rubberbandCheck }

    private var flagCount = 0
    private var lastYaw = 0F
    private var lastPitch = 0F

    private fun clearFlags() {
        flagCount = 0
    }

    private var lagbackDetected = false
    private var forceRotateDetected = false
    private var ghostBlockDetected = false

    private var lastMotionX = 0.0
    private var lastMotionY = 0.0
    private var lastMotionZ = 0.0

    private var lastPosX = 0.0
    private var lastPosY = 0.0
    private var lastPosZ = 0.0

    override fun onDisable() {
        clearFlags()
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.thePlayer ?: return
        val packet = event.packet

        if (player.ticksExisted <= 100)
            return

        if (player.isDead || (player.capabilities.isFlying && player.capabilities.disableDamage && !player.onGround))
            return

        if (packet is S08PacketPlayerPosLook) {
            val deltaYaw = calculateAngleDelta(packet.yaw, lastYaw)
            val deltaPitch = calculateAngleDelta(packet.pitch, lastPitch)

            if (deltaYaw > 90 || deltaPitch > 90) {
                forceRotateDetected = true
                flagCount++
                Chat.print("§7(§9FlagCheck§7) §dDetected §3Force-Rotate §e(${deltaYaw.roundToLong()}° | ${deltaPitch.roundToLong()}°) §b(§c${flagCount}x§b)")
            } else {
                forceRotateDetected = false
            }

            // Idk still testing :/
            // TODO: Make better check for ghostblock
            if (player.onGround && player.onPlayerRightClick(BlockPos.ORIGIN, EnumFacing.DOWN, Vec3(packet.x, packet.y, packet.z))
                && player.lookVec.rotatePitch(-90f) != null) {
                ghostBlockDetected = true
                flagCount++
                Chat.print("§7(§9FlagCheck§7) §dDetected §3GhostBlock §b(§eS08Packet§b) §b(§c${flagCount}x§b)")
            } else {
                ghostBlockDetected = false
            }

            if (!forceRotateDetected && !ghostBlockDetected) {
                lagbackDetected = true
                flagCount++
                Chat.print("§7(§9FlagCheck§7) §dDetected §3Lagback §b(§c${flagCount}x§b)")
            }

            if (mc.thePlayer.ticksExisted % 3 == 0) {
                lagbackDetected = false
            }

            lastYaw = mc.thePlayer.rotationYawHead
            lastPitch = mc.thePlayer.rotationPitch
        }

        when (packet) {
            is S01PacketJoinGame, is S00PacketDisconnect -> {
                clearFlags()
            }
        }
    }

    private fun calculateAngleDelta(newAngle: Float, oldAngle: Float): Float {
        var delta = newAngle - oldAngle
        if (delta > 180) delta -= 360
        if (delta < -180) delta += 360
        return abs(delta)
    }

    /**
     * Rubberband Checks (Still Under Testing)
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        if (!rubberbandCheck || player.ticksExisted <= 100)
            return

        if (player.isDead || (player.capabilities.isFlying && player.capabilities.disableDamage && !player.onGround))
            return

        val motionX = player.motionX
        val motionY = player.motionY
        val motionZ = player.motionZ

        val deltaX = player.posX - lastPosX
        val deltaY = player.posY - lastPosY
        val deltaZ = player.posZ - lastPosZ

        val distanceTraveled = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        val rubberbandReason = mutableListOf<String>()

        if (distanceTraveled > rubberbandThreshold) {
            rubberbandReason.add("Invalid Position")
        }

        if (abs(motionX) > rubberbandThreshold || abs(motionY) > rubberbandThreshold || abs(motionZ) > rubberbandThreshold) {
            if (!player.isCollided && !player.onGround) {
                rubberbandReason.add("Invalid Motion")
            }
        }

        if (rubberbandReason.isNotEmpty()) {
            flagCount++
            val reasonString = rubberbandReason.joinToString(" §8|§e ")
            Chat.print("§7(§9FlagCheck§7) §dDetected §3Rubberband §8(§e$reasonString§8) §b(§c${flagCount}x§b)")
        }

        // Update last position and motion
        lastPosX = player.prevPosX
        lastPosY = player.prevPosY
        lastPosZ = player.prevPosZ

        lastMotionX = motionX
        lastMotionY = motionY
        lastMotionZ = motionZ

        // Automatically clear flags (Default: 10 minutes)
        if (player.ticksExisted % (resetFlagCounterTicks * 20) == 0) {
            clearFlags()
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        clearFlags()
    }
}