/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.TickEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.player.Blink
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.limitAngleChange
import net.ccbluex.liquidbounce.utils.RotationUtils.performRaytrace
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getCenterDistance
import net.ccbluex.liquidbounce.utils.block.BlockUtils.searchBlocks
import net.ccbluex.liquidbounce.utils.extensions.getVec
import net.ccbluex.liquidbounce.utils.extensions.rotation
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.block.Block
import net.minecraft.client.gui.inventory.GuiContainer
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos

object ChestAura : Module("ChestAura", ModuleCategory.WORLD) {

    private val range by FloatValue("Range", 5F, 1F..6F)
    private val delay by IntegerValue("Delay", 100, 50..200)
    private val throughWalls by BoolValue("ThroughWalls", true)
    private val visualSwing by BoolValue("VisualSwing", true, subjective = true)
    private val chest by BlockValue("Chest", Block.getIdFromBlock(Blocks.chest))
    private val rotations by BoolValue("Rotations", true)
    private val strafe by ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off") { rotations }
    private val maxTurnSpeedValue: FloatValue = object : FloatValue("MaxTurnSpeed", 120f, 0f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minTurnSpeed)

        override fun isSupported() = rotations
    }
    private val maxTurnSpeed by maxTurnSpeedValue

    private val minTurnSpeed by object : FloatValue("MinTurnSpeed", 80f, 0f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxTurnSpeed)

        override fun isSupported() = !maxTurnSpeedValue.isMinimal() && rotations
    }

    private val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f)

    private var currentTarget: BlockPos? = null
    private val timer = MSTimer()

    val clickedBlocks = mutableListOf<BlockPos>()

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return

        if (Blink.handleEvents() || KillAura.isBlockingChestAura || event.eventState != EventState.POST) {
            return
        }

        if (mc.currentScreen is GuiContainer) {
            timer.reset()
            currentTarget = null
            return
        }

        val radius = range + 1

        val chestsToSearch = searchBlocks(radius.toInt()).filter {
            Block.getIdFromBlock(it.value) == chest && it.key !in clickedBlocks && getCenterDistance(it.key) <= range
        }.toList().sortedBy { getCenterDistance(it.first) }

        for ((chest, _) in chestsToSearch) {
            if (rotations) {
                val spot = faceBlock(chest, throughWalls) ?: continue

                val limitedRotation = limitAngleChange(
                    currentRotation ?: player.rotation,
                    spot.rotation,
                    nextFloat(minTurnSpeed, maxTurnSpeed)
                )

                setTargetRotation(
                    limitedRotation,
                    strafe = strafe != "Off",
                    strict = strafe == "Strict",
                    resetSpeed = minTurnSpeed to maxTurnSpeed,
                    angleThresholdForReset = angleThresholdUntilReset
                )
            }

            currentTarget = chest
            break
        }
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        val player = mc.thePlayer ?: return
        val world = mc.theWorld ?: return

        val target = currentTarget ?: return

        if (!timer.hasTimePassed(delay)) {
            return
        }

        val rotation = if (rotations) {
            currentRotation ?: player.rotation
        } else {
            toRotation(target.getVec(), false)
        }

        performRaytrace(target, rotation, range)?.let {
            if (it.blockPos == target) {
                if (mc.playerController.onPlayerRightClick(
                        player,
                        world,
                        player.inventoryContainer.getSlot(InventoryUtils.serverSlot + 36).stack,
                        it.blockPos,
                        it.sideHit,
                        it.hitVec
                    )
                ) {
                    if (visualSwing) {
                        player.swingItem()
                    } else {
                        sendPacket(C0APacketAnimation())
                    }

                    clickedBlocks += target
                    currentTarget = null
                    timer.reset()
                }
            }
        }
    }

    override fun onDisable() = clickedBlocks.clear()
}