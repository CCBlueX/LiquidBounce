/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.AutoTool
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.performRaytrace
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isFullBlock
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.Block
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.*
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import java.awt.Color

object Fucker : Module("Fucker", ModuleCategory.WORLD, hideModule = false) {

    /**
     * SETTINGS
     */

    private val hypixel by BoolValue("Hypixel", false)

    private val block by BlockValue("Block", 26)
    private val throughWalls by ListValue("ThroughWalls", arrayOf("None", "Raycast", "Around"), "None") { !hypixel }
    private val range by FloatValue("Range", 5F, 1F..7F)

    private val action by ListValue("Action", arrayOf("Destroy", "Use"), "Destroy")
    private val surroundings by BoolValue("Surroundings", true) { !hypixel }
    private val instant by BoolValue("Instant", false) { (action == "Destroy" || surroundings) && !hypixel }

    private val switch by IntegerValue("SwitchDelay", 250, 0..1000)
    private val swing by BoolValue("Swing", true)
    private val noHit by BoolValue("NoHit", false)

    private val rotations by BoolValue("Rotations", true)
    private val strafe by ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off") { rotations }
    private val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative") { rotations }
    private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 120f, 0f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minTurnSpeed)
        override fun isSupported() = rotations
    }

    private val maxTurnSpeed by maxTurnSpeedValue

    private val minTurnSpeed by object : FloatValue("MinTurnSpeed", 80f, 0f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxTurnSpeed)
        override fun isSupported() = !maxTurnSpeedValue.isMinimal() && rotations
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f) { rotations }

    /**
     * VALUES
     */

    private var pos: BlockPos? = null
    private var oldPos: BlockPos? = null
    private var blockHitDelay = 0
    private val switchTimer = MSTimer()
    var currentDamage = 0F

    // Surroundings
    private var areSurroundings = false

    override fun onToggle(state: Boolean) {
        if (pos != null && !mc.thePlayer.capabilities.isCreativeMode) {
            sendPacket(C07PacketPlayerDigging(ABORT_DESTROY_BLOCK, pos, EnumFacing.DOWN))
        }

        currentDamage = 0F
        pos = null
        areSurroundings = false
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        if (event.eventState != EventState.POST || noHit && KillAura.handleEvents() && KillAura.target != null) {
            return
        }

        val targetId = block

        if (pos == null || Block.getIdFromBlock(getBlock(pos!!)) != targetId || getCenterDistance(pos!!) > range) {
            pos = find(targetId)
        }

        // Reset current breaking when there is no target block
        if (pos == null) {
            currentDamage = 0F
            areSurroundings = false
            return
        }

        var currentPos = pos ?: return
        var spot = faceBlock(currentPos) ?: return

        if (surroundings || hypixel) {
            val eyes = player.eyes

            val blockPos = if (hypixel) {
                currentPos.up()
            } else {
                world.rayTraceBlocks(eyes, spot.vec, false, false, true)?.blockPos
            }

            if (blockPos != null && blockPos.getBlock() != air) {
                if (currentPos.x != blockPos.x || currentPos.y != blockPos.y || currentPos.z != blockPos.z) {
                    areSurroundings = true
                }

                pos = blockPos
                currentPos = pos ?: return
                spot = faceBlock(currentPos) ?: return
            }
        }

        // Reset switch timer when position changed
        if (oldPos != null && oldPos != currentPos) {
            currentDamage = 0F
            switchTimer.reset()
        }

        oldPos = currentPos

        if (!switchTimer.hasTimePassed(switch)) {
            return
        }

        // Block hit delay
        if (blockHitDelay > 0) {
            blockHitDelay--
            return
        }

        // Face block
        if (rotations) {
            val limitedRotation = limitAngleChange(
                currentRotation ?: player.rotation,
                spot.rotation,
                nextFloat(minTurnSpeed, maxTurnSpeed),
                smootherMode
            )

            setTargetRotation(
                limitedRotation,
                strafe = strafe != "Off",
                strict = strafe == "Strict",
                resetSpeed = minTurnSpeed to maxTurnSpeed,
                angleThresholdForReset = angleThresholdUntilReset,
                smootherMode = this.smootherMode
            )
        }
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        val controller = mc.playerController ?: return

        val currentPos = pos ?: return

        val targetRotation = if (rotations) {
            currentRotation ?: player.rotation
        } else {
            toRotation(currentPos.getVec(), false).fixedSensitivity()
        }

        val raytrace = performRaytrace(currentPos, targetRotation, range) ?: return

        when {
            // Destroy block
            action == "Destroy" || areSurroundings -> {
                // Auto Tool
                if (AutoTool.handleEvents()) {
                    AutoTool.switchSlot(currentPos)
                }

                // Break block
                if (instant && !hypixel) {
                    // CivBreak style block breaking
                    sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, currentPos, raytrace.sideHit))

                    if (swing) {
                        player.swingItem()
                    }

                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    currentDamage = 0F
                    return
                }

                // Minecraft block breaking
                val block = currentPos.getBlock() ?: return

                if (currentDamage == 0F) {
                    sendPacket(C07PacketPlayerDigging(START_DESTROY_BLOCK, currentPos, raytrace.sideHit))

                    if (player.capabilities.isCreativeMode || block.getPlayerRelativeBlockHardness(
                            player,
                            world,
                            currentPos
                        ) >= 1f
                    ) {
                        if (swing) {
                            player.swingItem()
                        }

                        controller.onPlayerDestroyBlock(currentPos, raytrace.sideHit)

                        currentDamage = 0F
                        pos = null
                        areSurroundings = false
                        return
                    }
                }

                if (swing) {
                    player.swingItem()
                }

                currentDamage += block.getPlayerRelativeBlockHardness(player, world, currentPos)
                world.sendBlockBreakProgress(player.entityId, currentPos, (currentDamage * 10F).toInt() - 1)

                if (currentDamage >= 1F) {
                    sendPacket(C07PacketPlayerDigging(STOP_DESTROY_BLOCK, currentPos, raytrace.sideHit))
                    controller.onPlayerDestroyBlock(currentPos, raytrace.sideHit)
                    blockHitDelay = 4
                    currentDamage = 0F
                    pos = null
                    areSurroundings = false
                }
            }

            // Use block
            action == "Use" -> {
                if (player.onPlayerRightClick(currentPos, raytrace.sideHit, raytrace.hitVec, player.heldItem)) {
                    if (swing) player.swingItem()
                    else sendPacket(C0APacketAnimation())

                    blockHitDelay = 4
                    currentDamage = 0F
                    pos = null
                    areSurroundings = false
                }
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        drawBlockBox(pos ?: return, Color.RED, true)
    }

    /**
     * Find new target block by [targetID]
     */
    private fun find(targetID: Int): BlockPos? {
        val thePlayer = mc.thePlayer ?: return null

        val radius = range.toInt() + 1

        var nearestBlockDistance = Double.MAX_VALUE
        var nearestBlock: BlockPos? = null

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = BlockPos(thePlayer).add(x, y, z)
                    val block = getBlock(blockPos) ?: continue

                    val distance = getCenterDistance(blockPos)

                    if (Block.getIdFromBlock(block) != targetID
                        || getCenterDistance(blockPos) > range
                        || nearestBlockDistance < distance
                        || !isHittable(blockPos) && !surroundings && !hypixel) {
                        continue
                    }

                    nearestBlockDistance = distance
                    nearestBlock = blockPos
                }
            }
        }

        return nearestBlock
    }

    /**
     * Check if block is hittable (or allowed to hit through walls)
     */
    private fun isHittable(blockPos: BlockPos): Boolean {
        val thePlayer = mc.thePlayer ?: return false

        return when (throughWalls.lowercase()) {
            "raycast" -> {
                val eyesPos = thePlayer.eyes
                val movingObjectPosition = mc.theWorld.rayTraceBlocks(eyesPos, blockPos.getVec(), false, true, false)

                movingObjectPosition != null && movingObjectPosition.blockPos == blockPos
            }

            "around" -> EnumFacing.values().any { !isFullBlock(blockPos.offset(it)) }

            else -> true
        }
    }

    override val tag
        get() = getBlockName(block)
}