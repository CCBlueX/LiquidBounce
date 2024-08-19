/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac.AAC
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac.AAC3311
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac.AAC3315
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.aac.LAAC
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.player.nofallmodes.other.Blink
import net.ccbluex.liquidbounce.features.module.modules.render.FreeCam
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.block.BlockUtils.collideBlock
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.block.BlockLiquid
import net.minecraft.util.AxisAlignedBB.fromBounds
import net.minecraft.util.BlockPos

object NoFall : Module("NoFall", Category.PLAYER, hideModule = false) {
    private val noFallModes = arrayOf(

        // Main
        SpoofGround,
        NoGround,
        Packet,
        Cancel,
        MLG,
        Blink,

        // AAC
        AAC,
        LAAC,
        AAC3311,
        AAC3315,

        // Hypixel (Watchdog)
        Hypixel,
        HypixelTimer,

        // Vulcan
        VulcanFast288,

        // Other Server
        Spartan,
        CubeCraft,
    )

    private val modes = noFallModes.map { it.modeName }.toTypedArray()

    val mode by ListValue("Mode", modes, "MLG")

    val minFallDistance by FloatValue("MinMLGHeight", 5f, 2f..50f, subjective = true) { mode == "MLG" }
    val retrieveDelay by IntegerValue("RetrieveDelayTicks", 5, 1..10, subjective = true) { mode == "MLG" }

    val rotations by BoolValue("Rotations", true) { mode == "MLG" }
    val autoMLG by ListValue("AutoMLG", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof") { mode == "MLG" }
    val swing by BoolValue("Swing", true) { mode == "MLG" }

    val smootherMode by ListValue("SmootherMode", arrayOf("Linear", "Relative"), "Relative") { rotations && mode == "MLG" }
    val keepRotation by BoolValue("KeepRotation", true) { rotations && mode == "MLG" }
    val keepTicks by object : IntegerValue("KeepTicks", 5, 1..20) {
        override fun onChange(oldValue: Int, newValue: Int) = newValue.coerceAtLeast(minimum)
        override fun isSupported() = rotations && keepRotation && mode == "MLG"
    }

    val startRotatingSlow by BoolValue("StartRotatingSlow", false) { rotations }
    val slowDownOnDirectionChange by BoolValue("SlowDownOnDirectionChange", false) { rotations }
    val useStraightLinePath by BoolValue("UseStraightLinePath", true) { rotations }
    val maxHorizontalSpeed: FloatValue = object : FloatValue("MaxHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minHorizontalSpeed.get())
        override fun isSupported() = rotations && mode == "MLG"
    }

    val minHorizontalSpeed: FloatValue = object : FloatValue("MinHorizontalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxHorizontalSpeed.get())
        override fun isSupported() = rotations && mode == "MLG"
    }

    val maxVerticalSpeed: FloatValue = object : FloatValue("MaxVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minVerticalSpeed.get())
        override fun isSupported() = rotations && mode == "MLG"
    }

    val minVerticalSpeed: FloatValue = object : FloatValue("MinVerticalSpeed", 180f, 1f..180f) {
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxVerticalSpeed.get())
        override fun isSupported() = rotations && mode == "MLG"
    }

    val angleThresholdUntilReset by FloatValue("AngleThresholdUntilReset", 5f, 0.1f..180f) { rotations && mode == "MLG" }

    val minRotationDifference by FloatValue("MinRotationDifference", 0f, 0f..1f) { rotations && mode == "MLG" }

    // Using too many times of simulatePlayer could result timer flag. Hence, why this is disabled by default.
    val checkFallDist by BoolValue("CheckFallDistance", false, subjective = true) { mode == "Blink" }

    val minFallDist: FloatValue = object : FloatValue("MinFallDistance", 2.5f, 0f..10f, subjective = true) {
        override fun isSupported() = mode == "Blink" && checkFallDist
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtMost(maxFallDist.get())
    }
    val maxFallDist: FloatValue = object : FloatValue("MaxFallDistance", 20f, 0f..100f, subjective = true) {
        override fun isSupported() = mode == "Blink" && checkFallDist
        override fun onChange(oldValue: Float, newValue: Float) = newValue.coerceAtLeast(minFallDist.get())
    }

    val autoOff by BoolValue("AutoOff", true) { mode == "Blink" }
    val simulateDebug by BoolValue("SimulationDebug", false, subjective = true) { mode == "Blink" }
    val fakePlayer by BoolValue("FakePlayer", true, subjective = true) { mode == "Blink" }

    var currentMlgBlock: BlockPos? = null
    var mlgInProgress = false
    var bucketUsed = false
    var shouldUse = false
    var mlgRotation: Rotation? = null

    override fun onEnable() {
        modeModule.onEnable()
    }

    override fun onDisable() {
        if (mode == "MLG") {
            currentMlgBlock = null
            mlgInProgress = false
            bucketUsed = false
            shouldUse = false
            mlgRotation = null
        }

        modeModule.onDisable()
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        modeModule.onTick()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer

        if (FreeCam.handleEvents()) return

        if (collideBlock(thePlayer.entityBoundingBox) { it is BlockLiquid } || collideBlock(
                fromBounds(
                    thePlayer.entityBoundingBox.maxX,
                    thePlayer.entityBoundingBox.maxY,
                    thePlayer.entityBoundingBox.maxZ,
                    thePlayer.entityBoundingBox.minX,
                    thePlayer.entityBoundingBox.minY - 0.01,
                    thePlayer.entityBoundingBox.minZ
                )
            ) { it is BlockLiquid }
        ) return

        modeModule.onUpdate()
    }

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        modeModule.onRender3D(event)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        mc.thePlayer ?: return

        modeModule.onPacket(event)
    }

    @EventTarget
    fun onBB(event: BlockBBEvent) {
        mc.thePlayer ?: return

        modeModule.onBB(event)
    }

    // Ignore condition used in LAAC mode
    @EventTarget(ignoreCondition = true)
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
        val thePlayer = mc.thePlayer

        if (collideBlock(thePlayer.entityBoundingBox) { it is BlockLiquid }
            || collideBlock(
                fromBounds(
                    thePlayer.entityBoundingBox.maxX,
                    thePlayer.entityBoundingBox.maxY,
                    thePlayer.entityBoundingBox.maxZ,
                    thePlayer.entityBoundingBox.minX,
                    thePlayer.entityBoundingBox.minY - 0.01,
                    thePlayer.entityBoundingBox.minZ
                )
            ) { it is BlockLiquid }
        ) return

        modeModule.onMove(event)
    }

    override val tag
        get() = mode

    private val modeModule
        get() = noFallModes.find { it.modeName == mode }!!
}