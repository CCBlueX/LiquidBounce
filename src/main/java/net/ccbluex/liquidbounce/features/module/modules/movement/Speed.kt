/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.aac.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.hypixel.HypixelHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.matrix.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.ncp.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.other.*
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spartan.SpartanYPort
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreBHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.spectre.SpectreOnGround
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.NewVerusLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.verus.VerusLowHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanHop
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.vulcan.VulcanLowHop
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

object Speed : Module("Speed", ModuleCategory.MOVEMENT, hideModule = false) {

    private val speedModes = arrayOf(

        // NCP
        NCPBHop,
        NCPFHop,
        SNCPBHop,
        NCPHop,
        NCPYPort,
        UNCPHop,
        UNCPHop2,

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

        // Matrix
        OldMatrixHop,
        MatrixHop,
        MatrixSlowHop,

        // Server specific
        TeleportCubeCraft,
        HiveHop,
        HypixelHop,
        Mineplex,
        MineplexGround,

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
    val customSpeed by FloatValue("CustomSpeed", 1.6f, 0.2f..2f) { mode == "Custom" }
    val customY by FloatValue("CustomY", 0f, 0f..4f) { mode == "Custom" }
    val customTimer by FloatValue("CustomTimer", 1f, 0.1f..2f) { mode == "Custom" }
    val customStrafe by BoolValue("CustomStrafe", true) { mode == "Custom" }
    val resetXZ by BoolValue("CustomResetXZ", false) { mode == "Custom" }
    val resetY by BoolValue("CustomResetY", false) { mode == "Custom" }

    val aacPortLength by FloatValue("AAC-PortLength", 1f, 1f..20f) { mode == "AACPort" }
    val aacGroundTimer by FloatValue("AACGround-Timer", 3f, 1.1f..10f) { mode in arrayOf("AACGround", "AACGround2") }
    val cubecraftPortLength by FloatValue("CubeCraft-PortLength", 1f, 0.1f..2f) { mode == "TeleportCubeCraft" }
    val mineplexGroundSpeed by FloatValue("MineplexGround-Speed", 0.5f, 0.1f..1f) { mode == "MineplexGround" }

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
        if (mc.thePlayer.isSneaking)
            return

        modeModule.onMove(event)
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (mc.thePlayer.isSneaking)
            return

        modeModule.onTick()
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent) {
        if (mc.thePlayer.isSneaking)
            return

        modeModule.onStrafe()
    }

    override fun onEnable() {
        mc.thePlayer ?: return

        mc.timer.timerSpeed = 1f

        modeModule.onEnable()
    }

    override fun onDisable() {
        mc.thePlayer ?: return

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
