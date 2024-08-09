/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACHop3313
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACHop350
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACHop4
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.AACHop5
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel.HypixelHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.MatrixHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.MatrixSlowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.OldMatrixHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spartan.SpartanYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreBHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreOnGround
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.NewVerusLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanGround288
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanLowHop
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue

object Speed : Module("Speed", Category.MOVEMENT, hideModule = false) {

    private val speedModes = arrayOf(

        // NCP
        NCPBHop,
        NCPFHop,
        SNCPBHop,
        NCPHop,
        NCPYPort,
        UNCPHop,
        UNCPHop2,
        UNCPLowHop,

        // AAC
        AACHop3313,
        AACHop350,
        AACHop4,
        AACHop5,

        // Spartan
        SpartanYPort,

        // Spectre
        SpectreLowHop,
        SpectreBHop,
        SpectreOnGround,

        // Verus
        VerusHop,
        VerusLowHop,
        NewVerusLowHop,

        // Vulcan
        VulcanHop,
        VulcanLowHop,
        VulcanGround288,

        // Matrix
        OldMatrixHop,
        MatrixHop,
        MatrixSlowHop,

        // Server specific
        TeleportCubeCraft,
        HypixelHop,

        // Other
        Boost,
        Frame,
        MiJump,
        OnGround,
        SlowHop,
        Legit,
        CustomSpeed,
        MineBlazeHop,
        MineBlazeTimer
    )

    private val modes = speedModes.map { it.modeName }.toTypedArray()

    val mode by object : ListValue("Mode", modes, "NCPBHop") {
        override fun onChange(oldValue: String, newValue: String): String {
            if (state)
                onDisable()

            return super.onChange(oldValue, newValue)
        }

        override fun onChanged(oldValue: String, newValue: String) {
            if (state)
                onEnable()
        }
    }

    // Custom Speed
    val customY by FloatValue("CustomY", 0.42f, 0f..4f) { mode == "Custom" }
    val customGroundStrafe by FloatValue("CustomGroundStrafe", 1.6f, 0f..2f) { mode == "Custom" }
    val customAirStrafe by FloatValue("CustomAirStrafe", 0f, 0f..2f) { mode == "Custom" }
    val customGroundTimer by FloatValue("CustomGroundTimer", 1f, 0.1f..2f) { mode == "Custom" }
    val customAirTimerTick by IntegerValue("CustomAirTimerTick", 5, 1..20) { mode == "Custom" }
    val customAirTimer by FloatValue("CustomAirTimer", 1f, 0.1f..2f) { mode == "Custom" }

    // Extra options
    val resetXZ by BoolValue("ResetXZ", false) { mode == "Custom" }
    val resetY by BoolValue("ResetY", false) { mode == "Custom" }
    val notOnConsuming by BoolValue("NotOnConsuming", false) { mode == "Custom" }
    val notOnFalling by BoolValue("NotOnFalling", false) { mode == "Custom" }
    val notOnVoid by BoolValue("NotOnVoid", true) { mode == "Custom" }

    // TeleportCubecraft Speed
    val cubecraftPortLength by FloatValue("CubeCraft-PortLength", 1f, 0.1f..2f) { mode == "TeleportCubeCraft" }

    // MineBlaze Speed
    val boost by BoolValue("Boost", true) { mode == "MineBlazeHop" }
    val strafeStrength by FloatValue("StrafeStrength", 0.29f, 0.1f..0.29f) { mode == "MineBlazeHop" }
    val groundTimer by FloatValue("GroundTimer", 0.5f, 0.1f..5f) { mode == "MineBlazeHop" }
    val airTimer by FloatValue("AirTimer", 1.09f, 0.1f..5f) { mode == "MineBlazeHop" }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val player = mc.thePlayer ?: return

        if (player.isSneaking)
            return

        if (isMoving && !sprintManually)
            player.isSprinting = true

        modeModule.onUpdate()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val player = mc.thePlayer ?: return

        if (player.isSneaking || event.eventState != EventState.PRE)
            return

        if (isMoving && !sprintManually)
            player.isSprinting = true

        modeModule.onMotion()
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onMove(event)
    }

    @EventTarget
    fun onTick(event: GameTickEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onTick()
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onStrafe()
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onJump(event)
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer?.isSneaking == true)
            return

        modeModule.onPacket(event)
    }

    override fun onEnable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f

        modeModule.onEnable()
    }

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        mc.timer.timerSpeed = 1f
        mc.thePlayer.speedInAir = 0.02f

        modeModule.onDisable()
    }

    override val tag
        get() = mode

    private val modeModule
        get() = speedModes.find { it.modeName == mode }!!

    private val sprintManually
        // Maybe there are more but for now there's the Legit mode.
        get() = modeModule in arrayOf(Legit)
}
