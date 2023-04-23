/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.eyes
import net.ccbluex.liquidbounce.utils.extensions.toRadiansD
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextDouble
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C00PacketKeepAlive
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.potion.Potion
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

object Fly : Module("Fly", ModuleCategory.MOVEMENT, Keyboard.KEY_F) {
    val mode by ListValue(
        "Mode", arrayOf(
            "Vanilla", "SmoothVanilla",

            // NCP
            "NCP", "OldNCP",

            // AAC
            "AAC1.9.10", "AAC3.0.5", "AAC3.1.6-Gomme", "AAC3.3.12", "AAC3.3.12-Glide", "AAC3.3.13",

            // CubeCraft
            "CubeCraft",

            // Hypixel
            "Hypixel", "BoostHypixel", "FreeHypixel",

            // Rewinside
            "Rewinside", "TeleportRewinside",

            // Other server specific flys
            "Mineplex", "NeruxVace", "Minesucht", "Redesky",

            // Spartan
            "Spartan", "Spartan2", "BugSpartan",

            // Other anticheats
            "MineSecure", "HawkEye", "HAC", "WatchCat",

            // Other
            "Jetpack", "KeepAlive", "Flag"
        ), "Vanilla"
    )
    private val vanillaSpeed by FloatValue("VanillaSpeed", 2f, 0f..5f) {
        mode in arrayOf("Vanilla", "KeepAlive", "MineSecure", "BugSpartan")
    }
    private val vanillaKickBypass by BoolValue("VanillaKickBypass", false) {
        mode in arrayOf("Vanilla", "SmoothVanilla")
    }
    private val ncpMotion by FloatValue("NCPMotion", 0f, 0f..1f) { mode == "NCP" }

    // AAC
    private val aacSpeed by FloatValue("AAC1.9.10-Speed", 0.3f, 0f..1f) { mode == "AAC1.9.10" }
    private val aacFast = BoolValue("AAC3.0.5-Fast", true) { mode == "AAC3.0.5" }
    private val aacMotion = FloatValue("AAC3.3.12-Motion", 10f, 0.1f..10f) { mode == "AAC3.3.12" }
    private val aacMotion2 = FloatValue("AAC3.3.13-Motion", 10f, 0.1f..10f) { mode == "AAC3.3.13" }

    // Hypixel
    private val hypixelBoost = BoolValue("Hypixel-Boost", true) { mode == "Hypixel" }
    private val hypixelBoostDelay = IntegerValue("Hypixel-BoostDelay", 1200, 0..2000) { mode == "Hypixel" }
    private val hypixelBoostTimer = FloatValue("Hypixel-BoostTimer", 1f, 0f..5f) { mode == "Hypixel" }

    // Other
    private val mineplexSpeed by FloatValue("MineplexSpeed", 1f, 0.5f..10f) { mode == "Mineplex" }
    private val neruxVaceTicks = IntegerValue("NeruxVace-Ticks", 6, 0..20) { mode == "NeruxVace" }
    private val redeskyHeight = FloatValue("Redesky-Height", 4f, 1f..7f) { mode == "Redesky" }

    // Visuals
    private val mark by BoolValue("Mark", true)
    private var startY = 0.0
    private val flyTimer = MSTimer()
    private val groundTimer = MSTimer()
    private var noPacketModify = false
    private var aacJump = 0.0
    private var aac3delay = 0
    private var aac3glideDelay = 0
    private var noFlag = false
    private val mineSecureVClipTimer = MSTimer()
    private val spartanTimer = TickTimer()
    private var minesuchtTP = 0L
    private val mineplexTimer = MSTimer()
    private var wasDead = false
    private val hypixelTimer = TickTimer()
    private var boostHypixelState = 1
    private var moveSpeed = 0.0
    private var lastDistance = 0.0
    private var failedStart = false
    private val cubecraft2TickTimer = TickTimer()
    private val cubecraftTeleportTickTimer = TickTimer()
    private val freeHypixelTimer = TickTimer()
    private var freeHypixelYaw = 0f
    private var freeHypixelPitch = 0f

    override fun onEnable() {
        val thePlayer = mc.thePlayer ?: return

        flyTimer.reset()
        noPacketModify = true

        val x = thePlayer.posX
        val y = thePlayer.posY
        val z = thePlayer.posZ

        val mode = mode

        run {
            when (mode.lowercase()) {
                "ncp" -> {
                    if (!thePlayer.onGround) return@run

                    for (i in 0..64) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.049, z, false),
                            C04PacketPlayerPosition(x, y, z, false)
                        )
                    }

                    sendPacket(C04PacketPlayerPosition(x, y + 0.1, z, true))

                    thePlayer.motionX *= 0.1
                    thePlayer.motionZ *= 0.1
                    thePlayer.swingItem()
                }
                "oldncp" -> {
                    if (!thePlayer.onGround) return@run

                    repeat(4) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 1.01, z, false),
                            C04PacketPlayerPosition(x, y, z, false)
                        )
                    }


                    thePlayer.jump()
                    thePlayer.swingItem()
                }
                "bugspartan" -> {
                    repeat(65) {
                        sendPackets(
                            C04PacketPlayerPosition(x, y + 0.049, z, false),
                            C04PacketPlayerPosition(x, y, z, false)
                        )
                    }

                    sendPacket(C04PacketPlayerPosition(x, y + 0.1, z, true))

                    thePlayer.motionX *= 0.1
                    thePlayer.motionZ *= 0.1
                    thePlayer.swingItem()
                }
                "infinitycubecraft" -> displayChatMessage("§8[§c§lCubeCraft-§a§lFly§8] §aPlace a block before landing.")
                "infinityvcubecraft" -> {
                    displayChatMessage("§8[§c§lCubeCraft-§a§lFly§8] §aPlace a block before landing.")

                    thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 2, thePlayer.posZ)
                }
                "boosthypixel" -> {
                    if (!thePlayer.onGround) return@run

                    repeat(10) {
                        //Imagine flagging to NCP.
                        sendPacket(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))
                    }

                    var fallDistance = 3.0125 //add 0.0125 to ensure we get the fall dmg

                    while (fallDistance > 0) {
                        sendPackets(
                            C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.0624986421, thePlayer.posZ, false),
                            C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.0625, thePlayer.posZ, false),
                            C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.0624986421, thePlayer.posZ, false),
                            C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 0.0000013579, thePlayer.posZ, false)
                        )
                        fallDistance -= 0.0624986421
                    }

                    sendPacket(C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY, thePlayer.posZ, true))

                    thePlayer.jump()

                    thePlayer.posY += 0.42f // Visual
                    boostHypixelState = 1
                    moveSpeed = 0.1
                    lastDistance = 0.0
                    failedStart = false
                }
                "redesky" -> {
                    if (mc.thePlayer.onGround) {
                        redeskyVClip1(redeskyHeight.get())
                    }
                }
            }
        }

        startY = thePlayer.posY
        aacJump = -3.8
        noPacketModify = false

        if (mode == "FreeHypixel") {
            freeHypixelTimer.reset()
            thePlayer.setPositionAndUpdate(thePlayer.posX, thePlayer.posY + 0.42, thePlayer.posZ)
            freeHypixelYaw = thePlayer.rotationYaw
            freeHypixelPitch = thePlayer.rotationPitch
        }

        super.onEnable()
    }

    override fun onDisable() {
        wasDead = false
        redeskySpeed(0)

        val thePlayer = mc.thePlayer ?: return

        noFlag = false

        val mode = mode

        if (!mode.uppercase().startsWith("AAC") && mode != "Hypixel" && mode != "CubeCraft"
        ) {
            thePlayer.motionX = 0.0
            thePlayer.motionY = 0.0
            thePlayer.motionZ = 0.0
        }
        if (mode == "Redesky") {
            redeskyHClip2(0.0)
        }

        thePlayer.capabilities.isFlying = false
        mc.timer.timerSpeed = 1f
        thePlayer.speedInAir = 0.02f
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val vanillaSpeed = vanillaSpeed
        val thePlayer = mc.thePlayer

        run {
            when (mode.lowercase()) {
                "vanilla" -> {
                    thePlayer.capabilities.isFlying = false
                    thePlayer.motionY = 0.0
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                    if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += vanillaSpeed
                    if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= vanillaSpeed
                    strafe(vanillaSpeed)
                    handleVanillaKickBypass()
                }
                "smoothvanilla" -> {
                    thePlayer.capabilities.isFlying = true
                    handleVanillaKickBypass()
                }
                "cubecraft" -> {
                    mc.timer.timerSpeed = 0.6f
                    cubecraftTeleportTickTimer.update()
                }
                "ncp" -> {
                    thePlayer.motionY = (-ncpMotion).toDouble()
                    if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.5
                    strafe()
                }
                "oldncp" -> {
                    if (startY > thePlayer.posY) thePlayer.motionY = -0.000000000000000000000000000000001
                    if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.2
                    if (mc.gameSettings.keyBindJump.isKeyDown && thePlayer.posY < startY - 0.1) thePlayer.motionY = 0.2
                    strafe()
                }
                "aac1.9.10" -> {
                    if (mc.gameSettings.keyBindJump.isKeyDown) aacJump += 0.2
                    if (mc.gameSettings.keyBindSneak.isKeyDown) aacJump -= 0.2

                    if (startY + aacJump > thePlayer.posY) {
                        sendPacket(C03PacketPlayer(true))
                        thePlayer.motionY = 0.8
                        strafe(aacSpeed)
                    }
                    strafe()
                }
                "aac3.0.5" -> {
                    if (aac3delay == 2) thePlayer.motionY = 0.1 else if (aac3delay > 2) aac3delay = 0
                    if (aacFast.get()) {
                        if (thePlayer.movementInput.moveStrafe == 0f) thePlayer.jumpMovementFactor = 0.08f
                        else thePlayer.jumpMovementFactor = 0f
                    }
                    aac3delay++
                }
                "aac3.1.6-gomme" -> {
                    thePlayer.capabilities.isFlying = true
                    if (aac3delay == 2) {
                        thePlayer.motionY += 0.05
                    } else if (aac3delay > 2) {
                        thePlayer.motionY -= 0.05
                        aac3delay = 0
                    }
                    aac3delay++
                    if (!noFlag) sendPacket(
                        C04PacketPlayerPosition(
                            thePlayer.posX, thePlayer.posY, thePlayer.posZ, thePlayer.onGround
                        )
                    )
                    if (thePlayer.posY <= 0.0) noFlag = true
                }
                "flag" -> {
                    sendPackets(
                        C06PacketPlayerPosLook(
                            thePlayer.posX + thePlayer.motionX * 999,
                            thePlayer.posY + (if (mc.gameSettings.keyBindJump.isKeyDown) 1.5624 else 0.00000001) - if (mc.gameSettings.keyBindSneak.isKeyDown) 0.0624 else 0.00000002,
                            thePlayer.posZ + thePlayer.motionZ * 999,
                            thePlayer.rotationYaw,
                            thePlayer.rotationPitch,
                            true
                        ),
                        C06PacketPlayerPosLook(
                            thePlayer.posX + thePlayer.motionX * 999,
                            thePlayer.posY - 6969,
                            thePlayer.posZ + thePlayer.motionZ * 999,
                            thePlayer.rotationYaw,
                            thePlayer.rotationPitch,
                            true
                        )
                    )
                    thePlayer.setPosition(
                        thePlayer.posX + thePlayer.motionX * 11, thePlayer.posY, thePlayer.posZ + thePlayer.motionZ * 11
                    )
                    thePlayer.motionY = 0.0
                }
                "keepalive" -> {
                    sendPacket(C00PacketKeepAlive())
                    thePlayer.capabilities.isFlying = false
                    thePlayer.motionY = 0.0
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                    if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += vanillaSpeed
                    if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= vanillaSpeed
                    strafe(vanillaSpeed)
                }
                "minesecure" -> {
                    thePlayer.capabilities.isFlying = false
                    if (!mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY = -0.01

                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                    strafe(vanillaSpeed)
                    if (mineSecureVClipTimer.hasTimePassed(150) && mc.gameSettings.keyBindJump.isKeyDown) {
                        sendPackets(
                            C04PacketPlayerPosition(thePlayer.posX, thePlayer.posY + 5, thePlayer.posZ, false),
                            C04PacketPlayerPosition(0.5, -1000.0, 0.5, false)
                        )

                        val yaw = thePlayer.rotationYaw.toRadiansD()
                        val x = -sin(yaw) * 0.4
                        val z = cos(yaw) * 0.4

                        thePlayer.setPosition(thePlayer.posX + x, thePlayer.posY, thePlayer.posZ + z)
                        mineSecureVClipTimer.reset()
                    }
                }
                "hac" -> {
                    thePlayer.motionX *= 0.8
                    thePlayer.motionZ *= 0.8
                    thePlayer.motionY = if (thePlayer.motionY <= -0.42) 0.42 else -0.42
                }
                "hawkeye" -> thePlayer.motionY = if (thePlayer.motionY <= -0.42) 0.42 else -0.42
                "teleportrewinside" -> {
                    val vectorStart = Vec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)
                    val yawRad = -thePlayer.rotationYaw.toRadiansD()
                    val pitchRad = -thePlayer.rotationPitch.toRadiansD()
                    val length = 9.9
                    val vectorEnd = Vec3(
                        sin(yawRad) * cos(pitchRad) * length + vectorStart.xCoord,
                        sin(pitchRad) * length + vectorStart.yCoord,
                        cos(yawRad) * cos(pitchRad) * length + vectorStart.zCoord
                    )
                    sendPackets(
                        C04PacketPlayerPosition(vectorEnd.xCoord, thePlayer.posY + 2, vectorEnd.zCoord, true),
                        C04PacketPlayerPosition(vectorStart.xCoord, thePlayer.posY + 2, vectorStart.zCoord, true)
                    )
                    thePlayer.motionY = 0.0
                }
                "minesucht" -> {
                    val posX = thePlayer.posX
                    val posY = thePlayer.posY
                    val posZ = thePlayer.posZ

                    if (!mc.gameSettings.keyBindForward.isKeyDown) return@run

                    if (System.currentTimeMillis() - minesuchtTP > 99) {
                        val vec3 = thePlayer.eyes
                        val vec31 = mc.thePlayer.getLook(1f)
                        val vec32 = vec3.addVector(vec31.xCoord * 7, vec31.yCoord * 7, vec31.zCoord * 7)
                        if (thePlayer.fallDistance > 0.8) {
                            sendPacket(C04PacketPlayerPosition(posX, posY + 50, posZ, false))
                            mc.thePlayer.fall(100f, 100f)
                            thePlayer.fallDistance = 0f
                            sendPacket(C04PacketPlayerPosition(posX, posY + 20, posZ, true))
                        }
                        sendPackets(
                            C04PacketPlayerPosition(vec32.xCoord, posY + 50, vec32.zCoord, true),
                            C04PacketPlayerPosition(posX, posY, posZ, false),
                            C04PacketPlayerPosition(vec32.xCoord, posY, vec32.zCoord, true),
                            C04PacketPlayerPosition(posX, posY, posZ, false)
                        )
                        minesuchtTP = System.currentTimeMillis()
                    } else {
                        sendPackets(
                            C04PacketPlayerPosition(posX, posY, posZ, false),
                            C04PacketPlayerPosition(posX, posY, posZ, true)
                        )
                    }
                }
                "jetpack" -> if (mc.gameSettings.keyBindJump.isKeyDown) {
//                    mc.effectRenderer.spawnEffectParticle(EnumParticleTypes.FLAME.getParticleID(), thePlayer.posX, thePlayer.posY + 0.2, thePlayer.posZ, -thePlayer.motionX, -0.5, -thePlayer.motionZ)
                    thePlayer.motionY += 0.15
                    thePlayer.motionX *= 1.1
                    thePlayer.motionZ *= 1.1
                }
                "mineplex" -> if (thePlayer.inventory.getCurrentItem() == null) {
                    if (mc.gameSettings.keyBindJump.isKeyDown && mineplexTimer.hasTimePassed(100)) {
                        thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 0.6, thePlayer.posZ)
                        mineplexTimer.reset()
                    }
                    if (mc.thePlayer.isSneaking && mineplexTimer.hasTimePassed(100)) {
                        thePlayer.setPosition(thePlayer.posX, thePlayer.posY - 0.6, thePlayer.posZ)
                        mineplexTimer.reset()
                    }
                    val blockPos = BlockPos(thePlayer).down()
                    val vec = Vec3(blockPos).addVector(0.4, 0.4, 0.4)
                        .add(Vec3(EnumFacing.UP.directionVec))
                    mc.playerController.onPlayerRightClick(
                        thePlayer,
                        mc.theWorld,
                        thePlayer.inventory.getCurrentItem(),
                        blockPos,
                        EnumFacing.UP,
                        Vec3(vec.xCoord * 0.4f, vec.yCoord * 0.4f, vec.zCoord * 0.4f)
                    )
                    strafe(0.27f)
                    mc.timer.timerSpeed = 1 + mineplexSpeed
                } else {
                    mc.timer.timerSpeed = 1f
                    state = false
                    displayChatMessage("§8[§c§lMineplex-§a§lFly§8] §aSelect an empty slot to fly.")
                }
                "aac3.3.12" -> {
                    if (thePlayer.posY < -70) thePlayer.motionY = aacMotion.get().toDouble()
                    mc.timer.timerSpeed = 1f
                    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                        mc.timer.timerSpeed = 0.2f
                        mc.rightClickDelayTimer = 0
                    }
                }
                "aac3.3.12-glide" -> {
                    if (!thePlayer.onGround) aac3glideDelay++
                    if (aac3glideDelay == 2) mc.timer.timerSpeed = 1f
                    if (aac3glideDelay == 12) mc.timer.timerSpeed = 0.1f
                    if (aac3glideDelay >= 12 && !thePlayer.onGround) {
                        aac3glideDelay = 0
                        thePlayer.motionY = .015
                    }
                }
                "aac3.3.13" -> {
                    if (thePlayer.isDead) wasDead = true
                    if (wasDead || thePlayer.onGround) {
                        wasDead = false
                        thePlayer.motionY = aacMotion2.get().toDouble()
                        thePlayer.onGround = false
                    }
                    mc.timer.timerSpeed = 1f
                    if (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL)) {
                        mc.timer.timerSpeed = 0.2f
                        mc.rightClickDelayTimer = 0
                    }
                }
                "watchcat" -> {
                    strafe(0.15f)
                    mc.thePlayer.isSprinting = true

                    if (thePlayer.posY < startY + 2) {
                        thePlayer.motionY = nextDouble(endInclusive = 0.5)
                        return@run
                    }

                    if (startY > thePlayer.posY) strafe(0f)
                }
                "spartan" -> {
                    thePlayer.motionY = 0.0

                    spartanTimer.update()
                    if (spartanTimer.hasTimePassed(12)) {
                        sendPacket(
                            C04PacketPlayerPosition(
                                thePlayer.posX, thePlayer.posY + 8, thePlayer.posZ, true
                            )
                        )
                        sendPacket(
                            C04PacketPlayerPosition(
                                thePlayer.posX, thePlayer.posY - 8, thePlayer.posZ, true
                            )
                        )
                        spartanTimer.reset()
                    }
                }
                "spartan2" -> {
                    strafe(0.264f)
                    if (thePlayer.ticksExisted % 8 == 0) sendPacket(
                        C04PacketPlayerPosition(
                            thePlayer.posX, thePlayer.posY + 10, thePlayer.posZ, true
                        )
                    )
                }
                "neruxvace" -> {
                    if (!thePlayer.onGround) aac3glideDelay++
                    if (aac3glideDelay >= neruxVaceTicks.get() && !thePlayer.onGround) {
                        aac3glideDelay = 0
                        thePlayer.motionY = .015
                    }
                }
                "hypixel" -> {
                    val boostDelay = hypixelBoostDelay.get()
                    if (hypixelBoost.get() && !flyTimer.hasTimePassed(boostDelay)) {
                        mc.timer.timerSpeed = 1f + hypixelBoostTimer.get() * (flyTimer.hasTimeLeft(boostDelay.toLong())
                            .toFloat() / boostDelay)
                    }
                    hypixelTimer.update()
                    if (hypixelTimer.hasTimePassed(2)) {
                        thePlayer.setPosition(thePlayer.posX, thePlayer.posY + 1.0E-5, thePlayer.posZ)
                        hypixelTimer.reset()
                    }
                }
                "freehypixel" -> {
                    if (freeHypixelTimer.hasTimePassed(10)) {
                        thePlayer.capabilities.isFlying = true
                        return@run
                    } else {
                        thePlayer.rotationYaw = freeHypixelYaw
                        thePlayer.rotationPitch = freeHypixelPitch
                        thePlayer.motionY = 0.0
                        thePlayer.motionZ = thePlayer.motionY
                        thePlayer.motionX = thePlayer.motionZ
                    }
                    if (startY == BigDecimal(thePlayer.posY).setScale(3, RoundingMode.HALF_DOWN)
                            .toDouble()
                    ) freeHypixelTimer.update()
                }
                "bugspartan" -> {
                    thePlayer.capabilities.isFlying = false
                    thePlayer.motionY = 0.0
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                    if (mc.gameSettings.keyBindJump.isKeyDown) thePlayer.motionY += vanillaSpeed
                    if (mc.gameSettings.keyBindSneak.isKeyDown) thePlayer.motionY -= vanillaSpeed

                    strafe(vanillaSpeed)
                }
                "redesky" -> {
                    mc.timer.timerSpeed = 0.3f
                    redeskyHClip2(7.0)
                    redeskyVClip2(10.0)
                    redeskyVClip1(-0.5f)
                    redeskyHClip1(2.0)
                    redeskySpeed(1)
                    mc.thePlayer.motionY = -0.01
                }
            }
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (mode == "BoostHypixel") {
            when (event.eventState) {
                EventState.PRE -> {
                    hypixelTimer.update()
                    if (hypixelTimer.hasTimePassed(2)) {
                        mc.thePlayer.setPosition(
                            mc.thePlayer.posX, mc.thePlayer.posY + 1.0E-5, mc.thePlayer.posZ
                        )
                        hypixelTimer.reset()
                    }
                    if (!failedStart) mc.thePlayer.motionY = 0.0
                }
                EventState.POST -> {
                    val xDist = mc.thePlayer.posX - mc.thePlayer.prevPosX
                    val zDist = mc.thePlayer.posZ - mc.thePlayer.prevPosZ
                    lastDistance = sqrt(xDist * xDist + zDist * zDist)
                }
            }
        }
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        val mode = mode
        if (!mark || mode == "Vanilla" || mode == "SmoothVanilla")
            return
        val y = startY + 2.0
        drawPlatform(
            y, if (mc.thePlayer.entityBoundingBox.maxY < y) Color(0, 255, 0, 90) else Color(255, 0, 0, 90), 1.0
        )
        when (mode.lowercase()) {
            "aac1.9.10" -> drawPlatform(startY + aacJump, Color(0, 0, 255, 90), 1.0)
            "aac3.3.12" -> drawPlatform(-70.0, Color(0, 0, 255, 90), 1.0)
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (noPacketModify) return

        if (event.packet is C03PacketPlayer) {
            val packetPlayer = event.packet

            val mode = mode

            if (mode == "NCP" || mode == "Rewinside" || mode == "Mineplex" && mc.thePlayer.heldItem == null)
                packetPlayer.onGround = true

            if (mode == "Hypixel" || mode == "BoostHypixel")
                packetPlayer.onGround = false
        }
        if (event.packet is C06PacketPlayerPosLook) {
            val mode = mode
            if (mode == "BoostHypixel") {
                failedStart = true
                displayChatMessage("§8[§c§lBoostHypixel-§a§lFly§8] §cSetback detected.")
            }
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        when (mode.lowercase()) {
            "cubecraft" -> {
                val yaw = mc.thePlayer.rotationYaw.toRadiansD()
                if (cubecraftTeleportTickTimer.hasTimePassed(2)) {
                    event.x = -sin(yaw) * 2.4
                    event.z = cos(yaw) * 2.4
                    cubecraftTeleportTickTimer.reset()
                } else {
                    event.x = -sin(yaw) * 0.2
                    event.z = cos(yaw) * 0.2
                }
            }
            "boosthypixel" -> {
                if (!isMoving) {
                    event.x = 0.0
                    event.z = 0.0
                    return
                }
                if (failedStart) return

                val amplifier =
                    1 + (if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier + 1.0) else 0.0)

                val baseSpeed = 0.29 * amplifier

                when (boostHypixelState) {
                    1 -> {
                        moveSpeed =
                            (if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) 1.56 else 2.034) * baseSpeed
                        boostHypixelState = 2
                    }
                    2 -> {
                        moveSpeed *= 2.16
                        boostHypixelState = 3
                    }
                    3 -> {
                        moveSpeed =
                            lastDistance - (if (mc.thePlayer.ticksExisted % 2 == 0) 0.0103 else 0.0123) * (lastDistance - baseSpeed)
                        boostHypixelState = 4
                    }
                    else -> moveSpeed = lastDistance - lastDistance / 159.8
                }

                moveSpeed = max(moveSpeed, 0.3)

                val yaw = direction

                event.x = -sin(yaw) * moveSpeed
                event.z = cos(yaw) * moveSpeed

                mc.thePlayer.motionX = event.x
                mc.thePlayer.motionZ = event.z
            }
            "freehypixel" -> if (!freeHypixelTimer.hasTimePassed(10)) event.zero()
        }
    }

    @EventTarget
    fun onBB(event: BlockBBEvent) {
        if (mc.thePlayer == null) return

        val mode = mode
        if (event.block == Blocks.air &&
            (mode == "Hypixel" || mode == "BoostHypixel" || mode == "Rewinside"
                    || mode == "Mineplex" && mc.thePlayer.heldItem == null
            ) && event.y < mc.thePlayer.posY
        )
            event.boundingBox = AxisAlignedBB.fromBounds(
                event.x.toDouble(),
                event.y.toDouble(),
                event.z.toDouble(),
                event.x + 1.0,
                mc.thePlayer.posY,
                event.z + 1.0
            )
    }

    @EventTarget
    fun onJump(e: JumpEvent) {
        val mode = mode
        if (mode == "Hypixel" || mode == "BoostHypixel" || mode == "Rewinside" || mode == "Mineplex"
            && mc.thePlayer.heldItem == null)
            e.cancelEvent()
    }

    @EventTarget
    fun onStep(e: StepEvent) {
        val mode = mode
        if (mode == "Hypixel" || mode == "BoostHypixel" || mode == "Rewinside" || mode == "Mineplex"
            && mc.thePlayer.heldItem == null)
            e.stepHeight = 0f
    }

    private fun handleVanillaKickBypass() {
        if (!vanillaKickBypass || !groundTimer.hasTimePassed(1000)) return
        val ground = calculateGround()
        run {
            var posY = mc.thePlayer.posY
            while (posY > ground) {
                sendPacket(
                    C04PacketPlayerPosition(
                        mc.thePlayer.posX, posY, mc.thePlayer.posZ, true
                    )
                )
                if (posY - 8.0 < ground) break // Prevent next step
                posY -= 8.0
            }
        }
        sendPacket(
            C04PacketPlayerPosition(
                mc.thePlayer.posX, ground, mc.thePlayer.posZ, true
            )
        )
        var posY = ground
        while (posY < mc.thePlayer.posY) {
            sendPacket(
                C04PacketPlayerPosition(
                    mc.thePlayer.posX, posY, mc.thePlayer.posZ, true
                )
            )
            if (posY + 8.0 > mc.thePlayer.posY) break // Prevent next step
            posY += 8.0
        }
        sendPacket(
            C04PacketPlayerPosition(
                mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ, true
            )
        )
        groundTimer.reset()
    }

    private fun redeskyVClip1(vertical: Float) {
        mc.thePlayer.setPosition(mc.thePlayer.posX, mc.thePlayer.posY + vertical, mc.thePlayer.posZ)
    }

    private fun redeskyHClip1(horizontal: Double) {
        val playerYaw = mc.thePlayer.rotationYaw.toRadiansD()
        mc.thePlayer.setPosition(
            mc.thePlayer.posX + horizontal * -sin(playerYaw),
            mc.thePlayer.posY,
            mc.thePlayer.posZ + horizontal * cos(playerYaw)
        )
    }

    private fun redeskyHClip2(horizontal: Double) {
        val playerYaw = mc.thePlayer.rotationYaw.toRadiansD()
        sendPacket(
            C04PacketPlayerPosition(
                mc.thePlayer.posX + horizontal * -sin(
                    playerYaw
                ), mc.thePlayer.posY, mc.thePlayer.posZ + horizontal * cos(playerYaw), false
            )
        )
    }

    private fun redeskyVClip2(vertical: Double) {
        sendPacket(
            C04PacketPlayerPosition(
                mc.thePlayer.posX, mc.thePlayer.posY + vertical, mc.thePlayer.posZ, false
            )
        )
    }

    private fun redeskySpeed(speed: Int) {
        val playerYaw = mc.thePlayer.rotationYaw.toRadiansD()
        mc.thePlayer.motionX = speed * -sin(playerYaw)
        mc.thePlayer.motionZ = speed * cos(playerYaw)
    }

    // TODO: Make better and faster calculation lol
    private fun calculateGround(): Double {
        val playerBoundingBox = mc.thePlayer.entityBoundingBox
        var blockHeight = 1.0
        var ground = mc.thePlayer.posY
        while (ground > 0.0) {
            val customBox = AxisAlignedBB.fromBounds(
                playerBoundingBox.maxX,
                ground + blockHeight,
                playerBoundingBox.maxZ,
                playerBoundingBox.minX,
                ground,
                playerBoundingBox.minZ
            )
            if (mc.theWorld.checkBlockCollision(customBox)) {
                if (blockHeight <= 0.05) return ground + blockHeight
                ground += blockHeight
                blockHeight = 0.05
            }
            ground -= blockHeight
        }
        return 0.0
    }

    override val tag
        get() = mode
}
