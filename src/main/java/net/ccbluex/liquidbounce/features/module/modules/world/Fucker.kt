/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.AutoTool
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.performRaytrace
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlockName
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isFullBlock
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils.disableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBlockBox
import net.ccbluex.liquidbounce.utils.render.RenderUtils.enableGlCap
import net.ccbluex.liquidbounce.utils.render.RenderUtils.resetCaps
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.Block
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C07PacketPlayerDigging.Action.*
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import org.lwjgl.opengl.GL11.*
import java.awt.Color

object Fucker : Module("Fucker", Category.WORLD, hideModule = false) {

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
    val noHit by BoolValue("NoHit", false)

    private val rotations by BoolValue("Rotations", true)
    private val strafe by ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off") { rotations }
    private val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative") { rotations }

    private val simulateShortStop by BoolValue("SimulateShortStop", false) { rotations }
    private val startFirstRotationSlow by BoolValue("StartFirstRotationSlow", false) { rotations }

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

    private val blockProgress by BoolValue("BlockProgress", true)

    private val scale by FloatValue("Scale", 2F, 1F..6F) { blockProgress }
    private val font by FontValue("Font", Fonts.font40) { blockProgress }
    private val fontShadow by BoolValue("Shadow", true) { blockProgress }

    private val colorRed by IntegerValue("R", 200, 0..255) { blockProgress }
    private val colorGreen by IntegerValue("G", 100, 0..255) { blockProgress }
    private val colorBlue by IntegerValue("B", 0, 0..255) { blockProgress }

    private val ignoreOwnBed by BoolValue("IgnoreOwnBed", false)

    /**
     * VALUES
     */

    var pos: BlockPos? = null
    private var spawnLocation: Vec3? = null
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
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null)
            return

        val packet = event.packet

        if (packet is S08PacketPlayerPosLook) {
            val pos = BlockPos(packet.x, packet.y, packet.z)

            spawnLocation = Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        }
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

        // Check if it is the player's own bed
        if (ignoreOwnBed && isBedNearSpawn(currentPos)) {
            return
        }

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
            setTargetRotation(
                spot.rotation,
                strafe = strafe != "Off",
                strict = strafe == "Strict",
                turnSpeed = minHorizontalSpeed..maxHorizontalSpeed to minVerticalSpeed..maxVerticalSpeed,
                angleThresholdForReset = angleThresholdUntilReset,
                smootherMode = smootherMode,
                simulateShortStop = simulateShortStop,
                startOffSlow = startFirstRotationSlow
            )
        }
    }

    /**
     * Check if the bed at the given position is near the spawn location
     */
    private fun isBedNearSpawn(currentPos: BlockPos): Boolean {
        if (getBlock(currentPos) != Block.getBlockById(block) || spawnLocation == null) {
            return false
        }

        val spawnPos = BlockPos(spawnLocation)
        return currentPos.distanceSqToCenter(spawnPos.x.toDouble(), spawnPos.y.toDouble(), spawnPos.z.toDouble()) < 256 // 16 * 16
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
                // Check if it is the player's own bed
                if (ignoreOwnBed && isBedNearSpawn(currentPos)) {
                    return
                }

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
        val pos = pos ?: return
        val player = mc.thePlayer ?: return
        val renderManager = mc.renderManager

        // Check if it is the player's own bed
        if (ignoreOwnBed && isBedNearSpawn(pos)) {
            return
        }

        if (blockProgress) {
            if (getBlockName(block) == "Air") return

            val progress = ((currentDamage * 100).coerceIn(0f, 100f)).toInt()
            val progressText = "%d%%".format(progress)

            glPushAttrib(GL_ENABLE_BIT)
            glPushMatrix()

            // Translate to block position
            glTranslated(
                pos.x + 0.5 - renderManager.renderPosX,
                pos.y + 0.5 - renderManager.renderPosY,
                pos.z + 0.5 - renderManager.renderPosZ
            )

            glRotatef(-renderManager.playerViewY, 0F, 1F, 0F)
            glRotatef(renderManager.playerViewX, 1F, 0F, 0F)

            disableGlCap(GL_LIGHTING, GL_DEPTH_TEST)
            enableGlCap(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)

            val fontRenderer = font
            val color = ((colorRed and 0xFF) shl 16) or ((colorGreen and 0xFF) shl 8) or (colorBlue and 0xFF)

            // Scale
            val scale = (player.getDistanceSq(pos) / 8F).coerceAtLeast(1.5) / 150F * scale
            glScaled(-scale, -scale, scale)

            // Draw text
            val width = fontRenderer.getStringWidth(progressText) * 0.5f
            fontRenderer.drawString(
                progressText, -width, if (fontRenderer == Fonts.minecraftFont) 1F else 1.5F, color, fontShadow
            )

            resetCaps()
            glPopMatrix()
            glPopAttrib()
        }

        // Render block box
        drawBlockBox(pos, Color.RED, true)
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