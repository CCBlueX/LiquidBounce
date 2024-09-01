/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.START_DESTROY_BLOCK
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.awt.Color

object CivBreak : Module("CivBreak", Category.WORLD) {

    private val range by FloatValue("Range", 5F, 1F..6F)
    private val visualSwing by BoolValue("VisualSwing", true, subjective = false)

    private val rotations by BoolValue("Rotations", true)
    private val strafe by ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off") { rotations }
    private val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative") { rotations }

    private val simulateShortStop by BoolValue("SimulateShortStop", false) { rotations }
    private val startRotatingSlow by BoolValue("StartRotatingSlow", false) { rotations }
    private val slowDownOnDirectionChange by BoolValue("SlowDownOnDirectionChange", false) { rotations }
    private val useStraightLinePath by BoolValue("UseStraightLinePath", true) { rotations }
    private val maxHorizontalSpeedValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed)
        override fun isSupported() = rotations

    }
    private val maxHorizontalSpeed by maxHorizontalSpeedValue

    private val minHorizontalSpeed: Float by object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed)
        override fun isSupported() = !maxHorizontalSpeedValue.isMinimal() && rotations
    }

    private val maxVerticalSpeedValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed)
    }
    private val maxVerticalSpeed by maxVerticalSpeedValue

    private val minVerticalSpeed: Float by object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed)
        override fun isSupported() = !maxVerticalSpeedValue.isMinimal() && rotations
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f) { rotations }

    private val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..1f) { rotations }

    private var blockPos: BlockPos? = null
    private var Direction: Direction? = null

    @EventTarget
    fun onBlockClick(event: ClickBlockEvent) {
        if (event.clickedBlock?.let { getBlock(it) } == Blocks.BEDROCK) {
            return
        }

        blockPos = event.clickedBlock ?: return
        Direction = event.direction ?: return

        // Break
        sendPackets(
            PlayerActionC2SPacket(START_DESTROY_BLOCK, blockPos, Direction),
            PlayerActionC2SPacket(STOP_DESTROY_BLOCK, blockPos, Direction)
        )
    }

    @EventTarget
    fun onRotationUpdate(event: RotationUpdateEvent) {
        val pos = blockPos ?: return
        val isAir = getBlock(pos) == Blocks.AIR

        if (isAir || getCenterDistance(pos) > range) {
            blockPos = null
            return
        }

        if (rotations) {
            val spot = faceBlock(pos) ?: return

            setTargetRotation(
                spot.rotation,
                strafe = strafe != "Off",
                strict = strafe == "Strict",
                turnSpeed = minHorizontalSpeed..maxHorizontalSpeed to minVerticalSpeed..maxVerticalSpeed,
                angleThresholdForReset = angleThresholdUntilReset,
                smootherMode = smootherMode,
                simulateShortStop = simulateShortStop,
                startOffSlow = startRotatingSlow,
                slowDownOnDirChange = slowDownOnDirectionChange,
                useStraightLinePath = useStraightLinePath,
                minRotationDifference = minRotationDifference
            )
        }
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        blockPos ?: return
        Direction ?: return

        if (visualSwing) {
            mc.player.swingHand()
        } else {
            sendPacket(HandSwingC2SPacket())
        }

        // Break
        sendPackets(
            PlayerActionC2SPacket(START_DESTROY_BLOCK, blockPos, Direction),
            PlayerActionC2SPacket(STOP_DESTROY_BLOCK, blockPos, Direction)
        )

        mc.interactionManager.attackBlock(blockPos, Direction)
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        drawBlockBox(blockPos ?: return, Color.RED, true)
    }
}