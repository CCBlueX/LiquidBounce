/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isFullBlock
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.DeadBushBlock
import net.minecraft.client.option.GameOptions
import net.minecraft.init.Blocks
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.util.math.BlockPos
import net.minecraft.util.Direction
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.Vec3d
import net.minecraftforge.event.ForgeEventFactory
import java.awt.Color

object BedDefender : Module("BedDefender", Category.WORLD, hideModule = false) {

    private val rotations by BoolValue("Rotations", true)

    private val autoBlock by ListValue("AutoBlock", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof")
    private val swing by BoolValue("Swing", true)
    private val placeDelay by IntegerValue("PlaceDelay", 500, 0..1000)
    private val raycastMode by ListValue("Raycast", arrayOf("None", "Normal", "Around"), "Normal") { rotations }
    private val scannerMode by ListValue("Scanner", arrayOf("Nearest", "Random"), "Nearest")

    private val strafe by ListValue("Strafe", arrayOf("Off", "Strict", "Silent"), "Off") { rotations }
    private val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative") { rotations }
    private val keepRotation by BoolValue("KeepRotation", true) { rotations }
    private val keepTicks by object : IntegerValue("KeepTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
        override fun isSupported() = rotations && keepRotation
    }

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

    private val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..2f) { rotations }

    private val onSneakOnly by BoolValue("OnSneakOnly", true)
    private val autoSneak by ListValue("AutoSneak", arrayOf("Off", "Normal", "Packet"), "Off") { !onSneakOnly }
    private val trackCPS by BoolValue("TrackCPS", false)
    private val mark by BoolValue("Mark", false)

    private val defenceBlocks = mutableListOf<BlockPos>()
    private val bedTopPositions = mutableListOf<BlockPos>()
    private val bedBottomPositions = mutableListOf<BlockPos>()

    private val timerCounter = MSTimer()
    private var blockPosition: BlockPos ?= null

    override fun onDisable() {
        val player = mc.player ?: return

        if (!GameOptions.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
            if (player.isSneaking) player.isSneaking = false
        }

        blockPosition = null
        defenceBlocks.clear()
        bedTopPositions.clear()
        bedBottomPositions.clear()

        TickScheduler += {
            serverSlot = player.inventory.selectedSlot
        }
    }

    // TODO: Proper event to update.
    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.player ?: return
        val world = mc.world ?: return

        if (onSneakOnly && !mc.options.sneakKey.isPressed) {
            return
        }

        val radius = 4
        val posX = player.x.toInt()
        val posY = player.y.toInt()
        val posZ = player.z.toInt()

        bedTopPositions.clear()
        bedBottomPositions.clear()
        defenceBlocks.clear()

        // Get placing positions
        for (x in posX - radius..posX + radius) {
            for (y in posY - radius..posY + radius) {
                for (z in posZ - radius..posZ + radius) {
                    val blockPos = BlockPos(x, y, z)
                    val block = world.getBlockState(blockPos).block
                    if (block == Blocks.bed) {
                        val metadata = block.getMetaFromState(world.getBlockState(blockPos))
                        
                        if (metadata >= 8) {
                            bedTopPositions.add(blockPos)
                        } else {
                            bedBottomPositions.add(blockPos)
                        }
                    }
                }
            }
        }

        addDefenceBlocks(bedTopPositions)
        addDefenceBlocks(bedBottomPositions)

        if (defenceBlocks.isNotEmpty()) {
            val playerPos = player.pos ?: return
            val pos = if (scannerMode == "Nearest") defenceBlocks.minByOrNull { it.getSquaredDistance(playerPos) } ?: return else defenceBlocks.random()
            val blockPos = BlockPos(pos.x.toDouble(), pos.y - player.eyeHeight + 1.5, pos.z.toDouble())
            val rotation = RotationUtils.toRotation(blockPos.getVec(), false, player)
            val raytrace = performBlockRaytrace(rotation, mc.interactionManager.blockReachDistance) ?: return

            if (rotations) {
                RotationUtils.setTargetRotation(
                    rotation,
                    if (keepRotation) keepTicks else 1,
                    strafe != "Off",
                    strafe == "Strict",
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

            blockPosition = blockPos

            if (timerCounter.hasTimePassed(placeDelay)) {
                if (!isPlaceablePos(blockPos)) return

                when (autoSneak.lowercase()) {
                    "normal" -> mc.gameSettings.keyBindSneak.pressed = false
                    "packet" -> sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.START_SNEAKING))
                }

                placeBlock(blockPos, raytrace.direction, raytrace.pos)
                timerCounter.reset()
            } else {
                when (autoSneak.lowercase()) {
                    "normal" -> mc.gameSettings.keyBindSneak.pressed = true
                    "packet" -> sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.STOP_SNEAKING))
                }
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (mark && blockPosition != null) {
            val blockPos = BlockPos(blockPosition!!.x, blockPosition!!.y + 1, blockPosition!!.z)
            RenderUtils.drawBlockBox(blockPos, Color(68, 117, 255, 100), false)
            return
        }
    }

    private fun addDefenceBlocks(bedPositions: List<BlockPos>) {
        for (bedPos in bedPositions) {
            val surroundingPositions = listOf(
                bedPos.up(),
                bedPos.north(),
                bedPos.south(),
                bedPos.east(),
                bedPos.west()
            )

            for (pos in surroundingPositions) {
                if (pos !in bedTopPositions && pos !in bedBottomPositions && mc.world.isAirBlock(pos)) {
                    defenceBlocks.add(pos)
                }
            }
        }
    }

    private fun placeBlock(blockPos: BlockPos, side: Direction, hitVec: Vec3d) {
        val player = mc.player ?: return

        var stack = player.inventoryContainer.getSlot(serverSlot + 36).stack

        if (stack == null || stack.item !is BlockItem || (stack.item as BlockItem).block is DeadBushBlock
            || InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as BlockItem).block) || stack.stackSize <= 0) {
            val blockSlot = InventoryUtils.findBlockInHotbar() ?: return

            when (autoBlock.lowercase()) {
                "off" -> return

                "pick" -> {
                    player.inventory.selectedSlot = blockSlot - 36
                   mc.interactionManager.syncSelectedSlot()
                }

                "spoof", "switch" -> serverSlot = blockSlot - 36
            }
            stack = player.inventoryContainer.getSlot(blockSlot).stack
        }

        tryToPlaceBlock(stack, blockPos, side, hitVec)

        // Since we violate vanilla slot switch logic if we send the packets now, we arrange them for the next tick
        if (autoBlock == "Switch")
            serverSlot = player.inventory.selectedSlot

        switchBlockNextTickIfPossible(stack)

        if (trackCPS) {
            CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)
        }
    }

    private fun tryToPlaceBlock(
        stack: ItemStack,
        clickPos: BlockPos,
        side: Direction,
        hitVec: Vec3d,
    ): Boolean {
        val player = mc.player ?: return false

        val prevSize = stack.stackSize

        val clickedSuccessfully = player.onPlayerRightClick(clickPos, side, hitVec, stack)

        if (clickedSuccessfully) {
            if (swing) player.swingItem() else sendPacket(HandSwingC2SPacket())

            if (stack.stackSize <= 0) {
                player.inventory.main[serverSlot] = null
                ForgeEventFactory.onPlayerDestroyItem(player, stack)
            } else if (stack.stackSize != prevSize || mc.interactionManager.isInCreativeMode)
                mc.entityRenderDispatcher.itemRenderer.resetEquippedProgress()

            blockPosition = null
        } else {
            if (player.sendUseItem(stack))
                mc.entityRenderDispatcher.itemRenderer.resetEquippedProgress2()
        }

        return clickedSuccessfully
    }

    private fun isPlaceablePos(pos: BlockPos): Boolean {
        val player = mc.player ?: return false
        val world = mc.world ?: return false
        
        return when (raycastMode.lowercase()) {
            "normal" -> {
                val eyesPos = player.eyes
                val movingObjectPosition = world.rayTrace(eyesPos, pos.getVec(), false, true, false)

                movingObjectPosition != null && movingObjectPosition.blockPos == pos
            }
            
            "around" -> Direction.values().any { !isFullBlock(pos.offset(it)) }
            
            else -> true
        }
    }

    private fun switchBlockNextTickIfPossible(stack: ItemStack) {
        val player = mc.player ?: return
        if (autoBlock in arrayOf("Off","Switch")) return
        if (stack.stackSize > 0) return

        val switchSlot = InventoryUtils.findBlockInHotbar() ?: return

        TickScheduler += {
            if (autoBlock == "Pick") {
                player.inventory.selectedSlot = switchSlot - 36
               mc.interactionManager.syncSelectedSlot()
            } else {
                serverSlot = switchSlot - 36
            }
        }
    }

    private fun performBlockRaytrace(rotation: Rotation, maxReach: Float): BlockHitResult? {
        val player = mc.player ?: return null
        val world = mc.world ?: return null

        val eyes = player.eyes
        val rotationVec = getVectorForRotation(rotation)

        val reach = eyes + (rotationVec * maxReach.toDouble())

        return world.rayTrace(eyes, reach, false, true, false)
    }
}