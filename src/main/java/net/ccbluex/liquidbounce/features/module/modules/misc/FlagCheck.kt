/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.exploit.Disabler
import net.ccbluex.liquidbounce.features.module.modules.exploit.Disabler.isOnCombat
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.gui.GuiGameOver
import net.minecraft.init.Blocks
import net.minecraft.network.packet.s2c.login.LoginDisconnectS2CPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import net.minecraft.util.math.BlockPos
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlin.math.sqrt

object FlagCheck : Module("FlagCheck", Category.MISC, gameDetecting = true, hideModule = false) {

    private val resetFlagCounterTicks by IntegerValue("ResetCounterTicks", 600, 100..1000)
    private val rubberbandCheck by BoolValue("RubberbandCheck", false)
    private val rubberbandThreshold by FloatValue("RubberBandThreshold", 5.0f, 0.05f..10.0f) { rubberbandCheck }

    private var flagCount = 0
    private var lastYaw = 0F
    private var lastPitch = 0F

    private var blockPlacementAttempts = mutableMapOf<BlockPos, Long>()
    private var successfulPlacements = mutableSetOf<BlockPos>()

    private fun clearFlags() {
        flagCount = 0
        blockPlacementAttempts.clear()
        successfulPlacements.clear()
    }

    private var lagbackDetected = false
    private var forceRotateDetected = false

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
        val player = mc.player ?: return
        val packet = event.packet

        if (player.ticksAlive <= 100)
            return

        if (player.isDead || (player.abilities.isFlying && player.abilities.disableDamage && !player.onGround))
            return

        if (packet is PlayerPositionLookS2CPacket) {
            val deltaYaw = calculateAngleDelta(packet.yaw, lastYaw)
            val deltaPitch = calculateAngleDelta(packet.pitch, lastPitch)

            if (deltaYaw > 90 || deltaPitch > 90) {
                forceRotateDetected = true
                flagCount++
                Chat.print("§dDetected §3Force-Rotate §e(${deltaYaw.roundToLong()}° | ${deltaPitch.roundToLong()}°) §b(§c${flagCount}x§b)")
            } else {
                forceRotateDetected = false
            }

            if (!forceRotateDetected) {
                lagbackDetected = true
                flagCount++
                Chat.print("§dDetected §3Lagback §b(§c${flagCount}x§b)")
            }

            if (mc.player.ticksAlive % 3 == 0) {
                lagbackDetected = false
            }

            lastYaw = mc.player.yawHead
            lastPitch = mc.player.pitch
        }

        if (packet is PlayerInteractBlockC2SPacket) {
            val blockPos = packet.position
            blockPlacementAttempts[blockPos] = System.currentTimeMillis()
            successfulPlacements.add(blockPos)
        }

        when (packet) {
            is GameJoinS2CPacket, is LoginDisconnectS2CPacket -> {
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
     * Rubberband, Invalid Health/Hunger & GhostBlock Checks
     */
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return
        val world = mc.world ?: return

        if (player.isDead || mc.currentScreen is GuiGameOver || player.ticksAlive <= 100) {
            return
        }

        val currentTime = System.currentTimeMillis()

        // GhostBlock Checks | Checks is disabled when using VerusFly Disabler, to prevent false flag.
        if (!Disabler.handleEvents() || (Disabler.handleEvents() && Disabler.verusFly && isOnCombat)) {
            blockPlacementAttempts.filter { (_, timestamp) ->
                currentTime - timestamp > 500
            }.forEach { (blockPos, _) ->
                val block = world.getBlockState(blockPos).block
                val isNotUsing =
                    !player.isUsingItem && !player.isBlocking && (!KillAura.renderBlocking || !KillAura.blockStatus)

                if (block == Blocks.air && player.swingProgressInt > 2 && successfulPlacements != blockPos && isNotUsing) {
                    successfulPlacements.remove(blockPos)
                    flagCount++
                    Chat.print("§dDetected §3GhostBlock §b(§c${flagCount}x§b)")
                }

                blockPlacementAttempts.remove(blockPos)
            }
        }

        // Invalid Health/Hunger bar Checks (This is a known lagback by Intave AC)
        val invalidReason = mutableListOf<String>()
        if (player.health <= 0.0f) invalidReason.add("Health")
        if (player.foodStats.foodLevel <= 0) invalidReason.add("Hunger")

        if (invalidReason.isNotEmpty()) {
            flagCount++
            val reasonString = invalidReason.joinToString(" §8|§e ")
            Chat.print("§dDetected §3Invalid §e$reasonString §b(§c${flagCount}x§b)")
            invalidReason.clear()
        }

        // Rubberband Checks
        if (!rubberbandCheck || (player.abilities.isFlying && player.abilities.disableDamage && !player.onGround))
            return

        val velocityX = player.velocityX
        val velocityY = player.velocityY
        val velocityZ = player.velocityZ

        val deltaX = player.x - lastPosX
        val deltaY = player.y - lastPosY
        val deltaZ = player.z - lastPosZ

        val distanceTraveled = sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)

        val rubberbandReason = mutableListOf<String>()

        if (distanceTraveled > rubberbandThreshold) {
            rubberbandReason.add("Invalid Position")
        }

        if (abs(velocityX) > rubberbandThreshold || abs(velocityY) > rubberbandThreshold || abs(velocityZ) > rubberbandThreshold) {
            if (!player.isCollided && !player.onGround) {
                rubberbandReason.add("Invalid Motion")
            }
        }

        if (rubberbandReason.isNotEmpty()) {
            flagCount++
            val reasonString = rubberbandReason.joinToString(" §8|§e ")
            Chat.print("§7(§9FlagCheck§7) §dDetected §3Rubberband §8(§e$reasonString§8) §b(§c${flagCount}x§b)")
            rubberbandReason.clear()
        }

        // Update last position and motion
        lastPosX = player.prevPosX
        lastPosY = player.prevY
        lastPosZ = player.prevZ

        lastMotionX = velocityX
        lastMotionY = velocityY
        lastMotionZ = velocityZ

        // Automatically clear flags (Default: 10 minutes)
        if (player.ticksAlive % (resetFlagCounterTicks * 20) == 0) {
            clearFlags()
        }
    }

    @EventTarget
    fun onWorld(event: WorldEvent) {
        clearFlags()
    }
}