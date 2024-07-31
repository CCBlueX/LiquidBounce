package net.ccbluex.liquidbounce.features.module.modules.world.scaffolds

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.MinecraftInstance
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
            "Pulldown"
        ),
        "None"
    )

    val stopWhenBlockAboveValues = BoolValue("StopWhenBlockAbove", false) { towerModeValues.get() != "None" }

    val onJumpValues = BoolValue("TowerOnJump", true) { towerModeValues.get() != "None" }
    val matrixValues = BoolValue("TowerMatrix", false) { towerModeValues.get() != "None" }
    val placeModeValues = ListValue(
        "TowerPlaceTiming",
        arrayOf("Pre", "Post"),
        "Post"
    ) { towerModeValues.get() != "Packet" && towerModeValues.get() != "None" }

    // Jump mode
    val jumpMotionValues = FloatValue("jumpMotion", 0.42f, 0.3681289f..0.79f) { towerModeValues.get() == "MotionJump" }
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

    private val towerWorker = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Handle motion events
    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (towerModeValues.get() == "None") return
        if (onJumpValues.get() && !mc.gameSettings.keyBindJump.isKeyDown) return

        // Lock Rotation
        if (Scaffold.keepRotation && lockRotation != null) {
            setTargetRotation(
                lockRotation!!.fixedSensitivity(),
                strafe = Scaffold.strafe,
                turnSpeed = Scaffold.minHorizontalSpeed..Scaffold.maxHorizontalSpeed to Scaffold.minVerticalSpeed..Scaffold.maxVerticalSpeed,
                smootherMode = Scaffold.smootherMode,
                simulateShortStop = Scaffold.simulateShortStop,
                startOffSlow = Scaffold.startRotatingSlow,
                slowDownOnDirChange = Scaffold.slowDownOnDirectionChange,
                useStraightLinePath = Scaffold.useStraightLinePath
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

            if (!stopWhenBlockAboveValues.get() || getBlock(BlockPos(mc.thePlayer).up(2)) == air) move()

            val blockPos = BlockPos(mc.thePlayer).down()
            if (blockPos.getBlock() == air) {
                if (search(blockPos)) {
                    val vecRotation = faceBlock(blockPos)
                    if (vecRotation != null) {
                        setTargetRotation(vecRotation.rotation,
                            startOffSlow = Scaffold.startRotatingSlow,
                            slowDownOnDirChange = Scaffold.slowDownOnDirectionChange,
                            useStraightLinePath = Scaffold.useStraightLinePath
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
        mc.thePlayer.isAirBorne = true
        mc.thePlayer.triggerAchievement(StatList.jumpStat)
    }

    /**
     * Move player
     */
    private fun move() {
        val thePlayer = mc.thePlayer ?: return

        if (Scaffold.blocksAmount <= 0)
            return

        when (towerModeValues.get().lowercase()) {
            "jump" -> if (thePlayer.onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                fakeJump()
                thePlayer.tryJump()
            } else if (!thePlayer.onGround) {
                thePlayer.isAirBorne = false
                tickTimer.reset()
            }

            "motion" -> if (thePlayer.onGround) {
                fakeJump()
                thePlayer.motionY = 0.42
            } else if (thePlayer.motionY < 0.1) {
                thePlayer.motionY = -0.3
            }

            // Old Name (Jump)
            "motionjump" -> if (thePlayer.onGround && tickTimer.hasTimePassed(jumpDelayValues.get())) {
                fakeJump()
                thePlayer.motionY = jumpMotionValues.get().toDouble()
                tickTimer.reset()
            }

            "motiontp" -> if (thePlayer.onGround) {
                fakeJump()
                thePlayer.motionY = 0.42
            } else if (thePlayer.motionY < 0.23) {
                thePlayer.setPosition(thePlayer.posX, truncate(thePlayer.posY), thePlayer.posZ)
            }

            "packet" -> if (thePlayer.onGround && tickTimer.hasTimePassed(2)) {
                fakeJump()
                sendPackets(
                    C04PacketPlayerPosition(
                        thePlayer.posX,
                        thePlayer.posY + 0.42,
                        thePlayer.posZ,
                        false
                    ),
                    C04PacketPlayerPosition(
                        thePlayer.posX,
                        thePlayer.posY + 0.753,
                        thePlayer.posZ,
                        false
                    )
                )
                thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 1.0, thePlayer.posZ)
                tickTimer.reset()
            }

            "teleport" -> {
                if (teleportNoMotionValues.get()) {
                    thePlayer.motionY = 0.0
                }
                if ((thePlayer.onGround || !teleportGroundValues.get()) && tickTimer.hasTimePassed(
                        teleportDelayValues.get()
                    )
                ) {
                    fakeJump()
                    thePlayer.setPositionAndUpdate(
                        thePlayer.posX, thePlayer.posY + teleportHeightValues.get(), thePlayer.posZ
                    )
                    tickTimer.reset()
                }
            }

            "constantmotion" -> {
                if (thePlayer.onGround) {
                    if (constantMotionJumpPacketValues.get()) {
                        fakeJump()
                    }
                    jumpGround = thePlayer.posY
                    thePlayer.motionY = constantMotionValues.get().toDouble()
                }
                if (thePlayer.posY > jumpGround + constantMotionJumpGroundValues.get()) {
                    if (constantMotionJumpPacketValues.get()) {
                        fakeJump()
                    }
                    thePlayer.setPosition(
                        thePlayer.posX, truncate(thePlayer.posY), thePlayer.posZ
                    ) // TODO: toInt() required?
                    thePlayer.motionY = constantMotionValues.get().toDouble()
                    jumpGround = thePlayer.posY
                }
            }

            "pulldown" -> {
                if (!thePlayer.onGround && thePlayer.motionY < triggerMotionValues.get()) {
                    thePlayer.motionY = -dragMotionValues.get().toDouble()
                } else {
                    fakeJump()
                }
            }

            "aac3.3.9" -> {
                if (thePlayer.onGround) {
                    fakeJump()
                    thePlayer.motionY = 0.4001
                }
                mc.timer.timerSpeed = 1f
                if (thePlayer.motionY < 0) {
                    thePlayer.motionY -= 0.00000945
                    mc.timer.timerSpeed = 1.6f
                }
            }

            "aac3.6.4" -> if (thePlayer.ticksExisted % 4 == 1) {
                thePlayer.motionY = 0.4195464
                thePlayer.setPosition(thePlayer.posX - 0.035, thePlayer.posY, thePlayer.posZ)
            } else if (thePlayer.ticksExisted % 4 == 0) {
                thePlayer.motionY = -0.5
                thePlayer.setPosition(thePlayer.posX + 0.035, thePlayer.posY, thePlayer.posZ)
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
        val thePlayer = mc.thePlayer ?: return false
        if (!isReplaceable(blockPosition)) {
            return false
        }

        val eyesPos = thePlayer.eyes
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
                            || mc.theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null
                        ) continue

                        // face block
                        val rotation = toRotation(hitVec, false)

                        val rotationVector = getVectorForRotation(rotation)
                        val vector = eyesPos + (rotationVector * 4.25)

                        val obj = mc.theWorld.rayTraceBlocks(eyesPos, vector, false, false, true) ?: continue

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