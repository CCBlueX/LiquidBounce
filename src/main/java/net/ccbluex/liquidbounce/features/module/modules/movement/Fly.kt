/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.aac.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.blocksmc.BlocksMC
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.blocksmc.BlocksMC2
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.BoostHypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.FreeHypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.hypixel.Hypixel
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp.NCP
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp.OldNCP
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.BugSpartan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.Spartan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.spartan.Spartan2
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla.SmoothVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vanilla.Vanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.verus.Verus
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.verus.VerusGlide
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.Vulcan
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.VulcanGhost
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.vulcan.VulcanOld
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.extensions.stop
import net.ccbluex.liquidbounce.utils.extensions.stopXZ
import net.ccbluex.liquidbounce.utils.inventory.InventoryUtils.serverSlot
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawPlatform
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.WaitTickUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.PositionOnly
import net.minecraft.util.Box
import net.minecraft.util.math.BlockPos
import org.lwjgl.input.Keyboard
import java.awt.Color

object Fly : Module("Fly", Category.MOVEMENT, Keyboard.KEY_F, hideModule = false) {
    private val flyModes = arrayOf(
        Vanilla, SmoothVanilla,

        // NCP
        NCP, OldNCP,

        // AAC
        AAC1910, AAC305, AAC316, AAC3312, AAC3312Glide, AAC3313,

        // CubeCraft
        CubeCraft,

        // Hypixel
        Hypixel, BoostHypixel, FreeHypixel,

        // Other server specific flys
        NeruxVace, Minesucht, BlocksMC, BlocksMC2,

        // Spartan
        Spartan, Spartan2, BugSpartan,

        // Vulcan
        Vulcan, VulcanOld, VulcanGhost,

        // Verus
        Verus, VerusGlide,

        // Other anti-cheats
        MineSecure, HawkEye, HAC, WatchCat,

        // Other
        Jetpack, KeepAlive, Collide, Jump, Flag, Fireball
    )

    private val modes = flyModes.map { it.modeName }.toTypedArray()

    val mode by ListValue("Mode", modes, "Vanilla")

    val vanillaSpeed by FloatValue("VanillaSpeed", 2f, 0f..10f, subjective = true)
        { mode in arrayOf("Vanilla", "KeepAlive", "MineSecure", "BugSpartan") }
    private val vanillaKickBypass by BoolValue("VanillaKickBypass", false, subjective = true)
        { mode in arrayOf("Vanilla", "SmoothVanilla") }
    val ncpMotion by FloatValue("NCPMotion", 0f, 0f..1f) { mode == "NCP" }

    // AAC
    val aacSpeed by FloatValue("AAC1.9.10-Speed", 0.3f, 0f..1f) { mode == "AAC1.9.10" }
    val aacFast by BoolValue("AAC3.0.5-Fast", true) { mode == "AAC3.0.5" }
    val aacMotion by FloatValue("AAC3.3.12-Motion", 10f, 0.1f..10f) { mode == "AAC3.3.12" }
    val aacMotion2 by FloatValue("AAC3.3.13-Motion", 10f, 0.1f..10f) { mode == "AAC3.3.13" }

    // Hypixel
    val hypixelBoost by BoolValue("Hypixel-Boost", true) { mode == "Hypixel" }
    val hypixelBoostDelay by IntegerValue("Hypixel-BoostDelay", 1200, 50..2000)
        { mode == "Hypixel" && hypixelBoost }
    val hypixelBoostTimer by FloatValue("Hypixel-BoostTimer", 1f, 0.1f..5f)
        { mode == "Hypixel" && hypixelBoost }

    // Other
    val neruxVaceTicks by IntegerValue("NeruxVace-Ticks", 6, 2..20) { mode == "NeruxVace" }

    // Verus
    val damage by BoolValue("Damage", false) { mode == "Verus" }
    val timerSlow by BoolValue("TimerSlow", true) { mode == "Verus" }
    val boostTicksValue by IntegerValue("BoostTicks", 20, 1..30) { mode == "Verus" }
    val boostMotion by FloatValue("BoostMotion", 6.5f, 1f..9.85f) { mode == "Verus" }
    val yBoost by FloatValue("YBoost", 0.42f, 0f..10f) { mode == "Verus" }

    // BlocksMC
    val stable by BoolValue("Stable", false) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val timerSlowed by BoolValue("TimerSlowed", true) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val boostSpeed by FloatValue("BoostSpeed", 6f, 1f..15f) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val extraBoost by FloatValue("ExtraSpeed", 1f, 0.0F..2f) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val stopOnLanding by BoolValue("StopOnLanding", true) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val stopOnNoMove by BoolValue("StopOnNoMove", false) { mode == "BlocksMC" || mode == "BlocksMC2" }
    val debugFly by BoolValue("Debug", false) { mode == "BlocksMC" || mode == "BlocksMC2" }

    // Fireball
    val rotations by BoolValue("Rotations", true) { mode == "Fireball" }
    val pitchMode by ListValue("PitchMode", arrayOf("Custom", "Smart"), "Custom") { mode == "Fireball" }
    val rotationPitch by FloatValue("Pitch", 90f,0f..90f) { pitchMode != "Smart" && mode == "Fireball" }
    val invertYaw by BoolValue("InvertYaw", true) { pitchMode != "Smart" && mode == "Fireball" }

    val autoFireball by ListValue("AutoFireball", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof") { mode == "Fireball" }
    val swing by BoolValue("Swing", true) { mode == "Fireball" }
    val fireballTry by IntegerValue("MaxFireballTry", 1, 0..2) { mode == "Fireball" }
    val fireBallThrowMode by ListValue("FireballThrow", arrayOf("Normal", "Edge"), "Normal") { mode == "Fireball" }
    val edgeThreshold by FloatValue("EdgeThreshold", 1.05f,1f..2f) { fireBallThrowMode == "Edge" && mode == "Fireball" }

    val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative") { rotations && mode == "Fireball" }
    val keepRotation by BoolValue("KeepRotation", true) { rotations && mode == "Fireball" }
    val keepTicks by object : IntegerValue("KeepTicks", 1, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
        override fun isSupported() = rotations && keepRotation && mode == "Fireball"
    }

    val simulateShortStop by BoolValue("SimulateShortStop", false) {  rotations && mode == "Fireball" }
    val startFirstRotationSlow by BoolValue("StartFirstRotationSlow", false) { rotations && mode == "Fireball" }

    val maxHorizontalSpeed: FloatValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed.get())
        override fun isSupported() = rotations && mode == "Fireball"
    }

    val minHorizontalSpeed: FloatValue = object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed.get())
        override fun isSupported() = rotations && mode == "Fireball"
    }

    val maxVerticalSpeed: FloatValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed.get())
        override fun isSupported() = rotations && mode == "Fireball"
    }

    val minVerticalSpeed: FloatValue = object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed.get())
        override fun isSupported() = rotations && mode == "Fireball"
    }

    val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f) { rotations && mode == "Fireball" }

    val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..1f) { rotations && mode == "Fireball" }

    val autoJump by BoolValue("AutoJump", true) { mode == "Fireball" }

    // Visuals
    private val mark by BoolValue("Mark", true, subjective = true)

    var wasFired = false
    var firePosition: BlockPos ?= null

    var jumpY = 0.0

    var startY = 0.0
        private set

    private val groundTimer = MSTimer()
    private var wasFlying = false

    override fun onEnable() {
        val thePlayer = mc.player ?: return

        startY = thePlayer.z
        jumpY = thePlayer.z
        wasFlying = mc.player.abilities.flying

        modeModule.onEnable()
    }

    override fun onDisable() {
        val thePlayer = mc.player ?: return

        if (!mode.startsWith("AAC") && mode != "Hypixel" && mode != "VerusGlide"
            && mode != "SmoothVanilla" && mode != "Vanilla" && mode != "Rewinside"
            && mode != "Fireball" && mode != "Collide" && mode != "Jump") {

            if (mode == "CubeCraft") thePlayer.stopXZ()
            else thePlayer.stop()
        }

        wasFired = false
        firePosition = null
        serverSlot = thePlayer.inventory.selectedSlot
        thePlayer.abilities.flying = wasFlying
        mc.ticker.timerSpeed = 1f
        thePlayer.speedInAir = 0.02f

        modeModule.onDisable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        modeModule.onUpdate()
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        if (mode == "Fireball" && wasFired) {
            WaitTickUtils.scheduleTicks(2) {
                Fly.state = false
            }
        }

        modeModule.onTick()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        if (!mark || mode == "Vanilla" || mode == "SmoothVanilla")
            return

        val y = startY + 2.0 + (if (mode == "BoostHypixel") 0.42 else 0.0)
        drawPlatform(
            y,
            if (mc.player.boundingBox.maxY < y) Color(0, 255, 0, 90) else Color(255, 0, 0, 90),
            1.0
        )

        modeModule.onRender3D(event)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        mc.player ?: return

        modeModule.onPacket(event)
    }

    @EventTarget
    fun onBB(event: BlockBBEvent) {
        mc.player ?: return

        modeModule.onBB(event)
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        modeModule.onJump(event)
    }

    @EventTarget
    fun onStep(event: StepEvent) {
        modeModule.onStep(event)
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        modeModule.onMotion(event)
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        modeModule.onMove(event)
    }

    fun handleVanillaKickBypass() {
        if (!vanillaKickBypass || !groundTimer.hasTimePassed(1000)) return
        val ground = calculateGround() + 0.5
        run {
            var posY = mc.player.z
            while (posY > ground) {
                sendPacket(PositionOnly(mc.player.x, posY, mc.player.z, true))
                if (posY - 8.0 < ground) break // Prevent next step
                posY -= 8.0
            }
        }
        sendPacket(PositionOnly(mc.player.x, ground, mc.player.z, true))
        var posY = ground
        while (posY < mc.player.z) {
            sendPacket(PositionOnly(mc.player.x, posY, mc.player.z, true))
            if (posY + 8.0 > mc.player.z) break // Prevent next step
            posY += 8.0
        }
        sendPacket(PositionOnly(mc.player.x, mc.player.z, mc.player.z, true))
        groundTimer.reset()
    }

    // TODO: Make better and faster calculation lol
    private fun calculateGround(): Double {
        val playerBoundingBox = mc.player.boundingBox
        var blockHeight = 0.05
        var ground = mc.player.z
        while (ground > 0.0) {
            val customBox = Box.fromBounds(
                playerBoundingBox.maxX,
                ground + blockHeight,
                playerBoundingBox.maxZ,
                playerBoundingBox.minX,
                ground,
                playerBoundingBox.minZ
            )
            if (mc.world.checkBlockCollision(customBox)) {
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

    private val modeModule
        get() = flyModes.find { it.modeName == mode }!!
}
