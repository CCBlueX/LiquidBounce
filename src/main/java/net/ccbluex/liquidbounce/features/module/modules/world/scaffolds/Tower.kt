/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffolds

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.PlaceRotation
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils.faceBlock
import net.ccbluex.liquidbounce.utils.RotationUtils.getRotationDifference
import net.ccbluex.liquidbounce.utils.RotationUtils.getVectorForRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.setTargetRotation
import net.ccbluex.liquidbounce.utils.RotationUtils.toRotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.canBeClicked
import net.ccbluex.liquidbounce.utils.block.BlockUtils.getBlock
import net.ccbluex.liquidbounce.utils.block.BlockUtils.isReplaceable
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly
import net.minecraft.stat.Stats
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import kotlin.math.truncate

object Tower : MinecraftInstance(), Listenable {

    val towerModeValues = ListValue(
        "TowerMode",
        arrayOf(
            "None",
            "Jump",
            "MotionJump",
            "Motion",
            "ConstantMotion",
            "MotionTP",
            "Packet",
            "Teleport",
            "AAC3.3.9",
            "AAC3.6.4",
            "Vulcan2.9.0",
            "Pulldown"
        ),
        "None"
    )

    val stopWhenBlockAboveValues = BoolValue("StopWhenBlockAbove", false) { towerModeValues.get() != "None" }

    val onJumpValues = BoolValue("TowerOnJump", true) { towerModeValues.get() != "None" }
    val notOnMoveValues = BoolValue("TowerNotOnMove", false) { towerModeValues.get() != "None" }
    val matrixValues = BoolValue("TowerMatrix", false) { towerModeValues.get() != "None" }
    val placeModeValues = ListValue(
        "TowerPlaceTiming",
        arrayOf("Pre", "Post"),
        "Post"
    ) { towerModeValues.get() != "Packet" && towerModeValues.get() != "None" }

    // Jump mode
    val jumpMotionValues = FloatValue("JumpMotion", 0.42f, 0.3681289f..0.79f) { towerModeValues.get() == "MotionJump" }
    val jumpDelayValues = IntegerValue(
        "jumpDelay",
        0,
        0..20
    ) { towerModeValues.get() == "MotionJump" || towerModeValues.get() == "Jump" }

    // constantMotionValues
    val constantMotionValues = FloatValue(
        "ConstantMotion",
        0.42f,
        0.1f..1f
    ) { towerModeValues.get() == "ConstantMotion" }
    val constantMotionJumpGroundValues = FloatValue(
        "ConstantMotionJumpGround",
        0.79f,
        0.76f..1f
    ) { towerModeValues.get() == "ConstantMotion" }
    val constantMotionJumpPacketValues = BoolValue("JumpPacket", true) { towerModeValues.get() == "ConstantMotion" }

    // Pulldown
    val triggerMotionValues = FloatValue("TriggerMotion", 0.1f, 0.0f..0.2f) { towerModeValues.get() == "Pulldown" }
    val dragMotionValues = FloatValue("DragMotion", 1.0f, 0.1f..1.0f) { towerModeValues.get() == "Pulldown" }

    // Teleport
    val teleportHeightValues = FloatValue("TeleportHeight", 1.15f, 0.1f..5f) { towerModeValues.get() == "Teleport" }
    val teleportDelayValues = IntegerValue("TeleportDelay", 0, 0..20) { towerModeValues.get() == "Teleport" }
    val teleportGroundValues = BoolValue("TeleportGround", true) { towerModeValues.get() == "Teleport" }
    val teleportNoMotionValues = BoolValue("TeleportNoMotion", false) { towerModeValues.get() == "Teleport" }

    // Target block
    var placeInfo: PlaceInfo? = null

    // Rotation lock
    private var lockRotation: Rotation? = null

    // Mode stuff
    private val tickTimer = TickTimer()
    private var jumpGround = 0.0

    // Handle motion events
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (towerModeValues.get() == "None") return
        if (notOnMoveValues.get() && isMoving) return
        if (onJumpValues.get() && !mc.options.jumpKey.isPressed) return
    
        // TODO: Proper event is needed to update rotations
        // Lock Rotation
        if (Scaffold.rotationMode != "None" && Scaffold.keepRotation && lockRotation != null) {
            setTargetRotation(
                lockRotation!!.fixedSensitivity(),
                strafe = Scaffold.strafe,
                turnSpeed = Scaffold.minHorizontalSpeed..Scaffold.maxHorizontalSpeed to Scaffold.minVerticalSpeed..Scaffold.maxVerticalSpeed,
                smootherMode = Scaffold.smootherMode,
                simulateShortStop = Scaffold.simulateShortStop,
                startOffSlow = Scaffold.startRotatingSlow,
                slowDownOnDirChange = Scaffold.slowDownOnDirectionChange,
                useStraightLinePath = Scaffold.useStraightLinePath,
                minRotationDifference = Scaffold.minRotationDifference
            )
        }

        mc.ticker.timerSpeed = Scaffold.timer
        val eventState = event.eventState

        // Force use of POST event when Packet mode is selected, it doesn't work with PRE mode
        if (eventState.stateName == (if (towerModeValues.get() == "Packet") "POST" else placeModeValues.get()
                .uppercase())
        )
            placeInfo?.let { Scaffold.place(it) }

        if (eventState == EventState.PRE) {
            lockRotation = null
            placeInfo = null
            tickTimer.update()

            if (!stopWhenBlockAboveValues.get() || getBlock(BlockPos(mc.player).up(2)) == Blocks.AIR) move()

            val blockPos = BlockPos(mc.player).down()
            if (blockPos.getBlock() == Blocks.AIR) {
                if (search(blockPos)) {
                    val vecRotation = faceBlock(blockPos)
                    if (vecRotation != null) {
                        setTargetRotation(vecRotation.rotation,
                            startOffSlow = Scaffold.startRotatingSlow,
                            slowDownOnDirChange = Scaffold.slowDownOnDirectionChange,
                            useStraightLinePath = Scaffold.useStraightLinePath,
                            minRotationDifference = Scaffold.minRotationDifference
                        )
                        placeInfo!!.Vec3d = vecRotation.vec
                    }
                }
            }
        }
    }

    // Handle jump events
    @EventTarget
    fun onJump(event: JumpEvent) {
        if (onJumpValues.get()) {
            if (Scaffold.scaffoldMode == "GodBridge" && (Scaffold.autoJump || Scaffold.jumpAutomatically) || Scaffold.shouldJumpOnInput)
                return
            if (towerModeValues.get() == "None" || towerModeValues.get() == "Jump")
                return
            if (Speed.state || Fly.state)
                return

            event.cancelEvent()
        }
    }

    // Send jump packets, bypasses Hypixel.
    private fun fakeJump() {
        mc.player.isAirBorne = true
        mc.player.incrementStat(Stats.JUMPS)
    }

    /**
     * Move player
     */
    private fun move() {
        val player = mc.player ?: return

        if (Scaffold.blocksAmount <= 0)
            return

        when (towerModeValues.get().lowercase()) {
            "jump" -> if (player.onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                fakeJump()
                player.tryJump()
            } else if (!player.onGround) {
                player.isAirBorne = false
                tickTimer.reset()
            }

            "motion" -> if (player.onGround) {
                fakeJump()
                player.velocityY = 0.42
            } else if (player.velocityY < 0.1) {
                player.velocityY = -0.3
            }

            // Old Name (Jump)
            "motionjump" -> if (player.onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                fakeJump()
                player.velocityY = jumpMotionValues.get().toDouble()
                tickTimer.reset()
            }

            "motiontp" -> if (player.onGround) {
                fakeJump()
                player.velocityY = 0.42
            } else if (player.velocityY < 0.23) {
                player.updatePosition(player.x, truncate(player.z), player.z)
            }

            "packet" -> if (player.onGround && tickTimer.hasTimePassed(2)) {
                fakeJump()
                sendPackets(
                    PositionOnly(
                        player.x,
                        player.z + 0.42,
                        player.z,
                        false
                    ),
                    PositionOnly(
                        player.x,
                        player.y + 0.753,
                        player.z,
                        false
                    )
                )
                player.updatePosition(player.x, player.z + 1.0, player.z)
                tickTimer.reset()
            }

            "teleport" -> {
                if (teleportNoMotionValues.get()) {
                    player.velocityY = 0.0
                }
                if ((player.onGround || !teleportGroundValues.get()) && tickTimer.hasTimePassed(
                        teleportDelayValues.get()
                    )
                ) {
                    fakeJump()
                    player.updatePosition(
                        player.x, player.y + teleportHeightValues.get(), player.z
                    )
                    tickTimer.reset()
                }
            }

            "constantmotion" -> {
                if (player.onGround) {
                    if (constantMotionJumpPacketValues.get()) {
                        fakeJump()
                    }
                    jumpGround = player.z
                    player.velocityY = constantMotionValues.get().toDouble()
                }
                if (player.z > jumpGround + constantMotionJumpGroundValues.get()) {
                    if (constantMotionJumpPacketValues.get()) {
                        fakeJump()
                    }
                    player.updatePosition(
                        player.x, truncate(player.z), player.z
                    ) // TODO: toInt() required?
                    player.velocityY = constantMotionValues.get().toDouble()
                    jumpGround = player.z
                }
            }

            "pulldown" -> {
                if (!player.onGround && player.velocityY < triggerMotionValues.get()) {
                    player.velocityY = -dragMotionValues.get().toDouble()
                } else {
                    fakeJump()
                }
            }

            // Credit: @localpthebest / Nextgen
            "vulcan2.9.0" -> {
                if (player.ticksAlive % 10 == 0) {
                    // Prevent Flight Flag
                    player.velocityY = -0.1
                    return
                }

                fakeJump()

                if (player.ticksAlive % 2 == 0) {
                    player.velocityY = 0.7
                } else {
                    player.velocityY = if (isMoving) 0.42 else 0.6
                }
            }

            "aac3.3.9" -> {
                if (player.onGround) {
                    fakeJump()
                    player.velocityY = 0.4001
                }
                mc.ticker.timerSpeed = 1f
                if (player.velocityY < 0) {
                    player.velocityY -= 0.00000945
                    mc.ticker.timerSpeed = 1.6f
                }
            }

            "aac3.6.4" -> if (player.ticksAlive % 4 == 1) {
                player.velocityY = 0.4195464
                player.updatePosition(player.x - 0.035, player.z, player.z)
            } else if (player.ticksAlive % 4 == 0) {
                player.velocityY = -0.5
                player.updatePosition(player.x + 0.035, player.z, player.z)
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.player ?: return
        val packet = event.packet

        if (towerModeValues.get() == "Vulcan2.9.0") {
            if (packet is PositionOnly) {
                if (!isMoving && player.ticksAlive % 2 == 0) {
                    packet.x += 0.1
                    packet.z += 0.1
                }
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @return
     */
    private fun search(blockPosition: BlockPos): Boolean {
        val player = mc.player ?: return false
        if (!isReplaceable(blockPosition)) {
            return false
        }

        val eyesPos = player.eyes
        var placeRotation: PlaceRotation? = null
        for (facingType in Direction.entries) {
            val neighbor = blockPosition.offset(facingType)
            if (!canBeClicked(neighbor)) {
                continue
            }
            val dirVec = Vec3d(facingType.vector)

            for (x in 0.1..0.9) {
                for (y in 0.1..0.9) {
                    for (z in 0.1..0.9) {
                        val posVec = Vec3d(blockPosition).add(
                            if (matrixValues.get()) 0.5 else x,
                            if (matrixValues.get()) 0.5 else y,
                            if (matrixValues.get()) 0.5 else z
                        )

                        val distanceSqPosVec = eyesPos.squaredDistanceTo(posVec)
                        val hitVec = posVec + (dirVec * 0.5)

                        if (eyesPos.distanceTo(hitVec) > 4.25
                            || distanceSqPosVec > eyesPos.squaredDistanceTo(posVec + dirVec)
                            || mc.world.rayTrace(eyesPos, hitVec, false, true, false) != null
                        ) continue

                        // Face block
                        val rotation = toRotation(hitVec, false)

                        val rotationVector = getVectorForRotation(rotation)
                        val vector = eyesPos + (rotationVector * 4.25)

                        val obj = mc.world.rayTrace(eyesPos, vector, false, false, true) ?: continue

                        if (!obj.type.isBlock || obj.blockPos != neighbor)
                            continue

                        if (placeRotation == null || getRotationDifference(rotation) < getRotationDifference(
                                placeRotation.rotation
                            )
                        )
                            placeRotation =
                                PlaceRotation(PlaceInfo(neighbor, facingType.opposite, hitVec), rotation)
                    }
                }
            }
        }

        placeRotation ?: return false

        lockRotation = placeRotation.rotation.fixedSensitivity()
        placeInfo = placeRotation.placeInfo

        return true
    }

    override fun handleEvents(): Boolean = Scaffold.handleEvents()
}
