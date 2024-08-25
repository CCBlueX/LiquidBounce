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
import net.minecraft.init.Blocks.air
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.stats.StatList
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
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
        if (onJumpValues.get() && !mc.gameSettings.keyBindJump.isKeyDown) return
    
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

        mc.timer.timerSpeed = Scaffold.timer
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

            if (!stopWhenBlockAboveValues.get() || getBlock(BlockPos(mc.player).up(2)) == air) move()

            val blockPos = BlockPos(mc.player).down()
            if (blockPos.getBlock() == air) {
                if (search(blockPos)) {
                    val vecRotation = faceBlock(blockPos)
                    if (vecRotation != null) {
                        setTargetRotation(vecRotation.rotation,
                            startOffSlow = Scaffold.startRotatingSlow,
                            slowDownOnDirChange = Scaffold.slowDownOnDirectionChange,
                            useStraightLinePath = Scaffold.useStraightLinePath,
                            minRotationDifference = Scaffold.minRotationDifference
                        )
                        placeInfo!!.vec3 = vecRotation.vec
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
        mc.player.triggerAchievement(StatList.jumpStat)
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
                player.motionY = 0.42
            } else if (player.motionY < 0.1) {
                player.motionY = -0.3
            }

            // Old Name (Jump)
            "motionjump" -> if (player.onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                fakeJump()
                player.motionY = jumpMotionValues.get().toDouble()
                tickTimer.reset()
            }

            "motiontp" -> if (player.onGround) {
                fakeJump()
                player.motionY = 0.42
            } else if (player.motionY < 0.23) {
                player.setPosition(player.posX, truncate(player.posY), player.posZ)
            }

            "packet" -> if (player.onGround && tickTimer.hasTimePassed(2)) {
                fakeJump()
                sendPackets(
                    C04PacketPlayerPosition(
                        player.posX,
                        player.posY + 0.42,
                        player.posZ,
                        false
                    ),
                    C04PacketPlayerPosition(
                        player.posX,
                        player.posY + 0.753,
                        player.posZ,
                        false
                    )
                )
                player.setPosition(player.posX, player.posY + 1.0, player.posZ)
                tickTimer.reset()
            }

            "teleport" -> {
                if (teleportNoMotionValues.get()) {
                    player.motionY = 0.0
                }
                if ((player.onGround || !teleportGroundValues.get()) && tickTimer.hasTimePassed(
                        teleportDelayValues.get()
                    )
                ) {
                    fakeJump()
                    player.setPositionAndUpdate(
                        player.posX, player.posY + teleportHeightValues.get(), player.posZ
                    )
                    tickTimer.reset()
                }
            }

            "constantmotion" -> {
                if (player.onGround) {
                    if (constantMotionJumpPacketValues.get()) {
                        fakeJump()
                    }
                    jumpGround = player.posY
                    player.motionY = constantMotionValues.get().toDouble()
                }
                if (player.posY > jumpGround + constantMotionJumpGroundValues.get()) {
                    if (constantMotionJumpPacketValues.get()) {
                        fakeJump()
                    }
                    player.setPosition(
                        player.posX, truncate(player.posY), player.posZ
                    ) // TODO: toInt() required?
                    player.motionY = constantMotionValues.get().toDouble()
                    jumpGround = player.posY
                }
            }

            "pulldown" -> {
                if (!player.onGround && player.motionY < triggerMotionValues.get()) {
                    player.motionY = -dragMotionValues.get().toDouble()
                } else {
                    fakeJump()
                }
            }

            // Credit: @localpthebest / Nextgen
            "vulcan2.9.0" -> {
                if (player.ticksExisted % 10 == 0) {
                    // Prevent Flight Flag
                    player.motionY = -0.1
                    return
                }

                fakeJump()

                if (player.ticksExisted % 2 == 0) {
                    player.motionY = 0.7
                } else {
                    player.motionY = if (isMoving) 0.42 else 0.6
                }
            }

            "aac3.3.9" -> {
                if (player.onGround) {
                    fakeJump()
                    player.motionY = 0.4001
                }
                mc.timer.timerSpeed = 1f
                if (player.motionY < 0) {
                    player.motionY -= 0.00000945
                    mc.timer.timerSpeed = 1.6f
                }
            }

            "aac3.6.4" -> if (player.ticksExisted % 4 == 1) {
                player.motionY = 0.4195464
                player.setPosition(player.posX - 0.035, player.posY, player.posZ)
            } else if (player.ticksExisted % 4 == 0) {
                player.motionY = -0.5
                player.setPosition(player.posX + 0.035, player.posY, player.posZ)
            }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val player = mc.player ?: return
        val packet = event.packet

        if (towerModeValues.get() == "Vulcan2.9.0") {
            if (packet is C04PacketPlayerPosition) {
                if (!isMoving && player.ticksExisted % 2 == 0) {
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
        for (facingType in EnumFacing.values()) {
            val neighbor = blockPosition.offset(facingType)
            if (!canBeClicked(neighbor)) {
                continue
            }
            val dirVec = Vec3(facingType.directionVec)

            for (x in 0.1..0.9) {
                for (y in 0.1..0.9) {
                    for (z in 0.1..0.9) {
                        val posVec = Vec3(blockPosition).addVector(
                            if (matrixValues.get()) 0.5 else x,
                            if (matrixValues.get()) 0.5 else y,
                            if (matrixValues.get()) 0.5 else z
                        )

                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec + (dirVec * 0.5)

                        if (eyesPos.distanceTo(hitVec) > 4.25
                            || distanceSqPosVec > eyesPos.squareDistanceTo(posVec + dirVec)
                            || mc.world.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null
                        ) continue

                        // Face block
                        val rotation = toRotation(hitVec, false)

                        val rotationVector = getVectorForRotation(rotation)
                        val vector = eyesPos + (rotationVector * 4.25)

                        val obj = mc.world.rayTraceBlocks(eyesPos, vector, false, false, true) ?: continue

                        if (!obj.typeOfHit.isBlock || obj.blockPos != neighbor)
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
