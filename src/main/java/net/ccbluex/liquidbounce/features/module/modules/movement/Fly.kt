/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.features.module.modules.movement.flies.ncp.*
//import net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac.*
//import net.ccbluex.liquidbounce.features.module.modules.movement.flies.cubecraft.Cubecraft
//import net.ccbluex.liquidbounce.features.module.modules.movement.flies.hypixel.*
//import net.ccbluex.liquidbounce.features.module.modules.movement.flies.rewinside.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.Flag
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla.SmoothVanilla
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla.Vanilla
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

object Fly : Module("Fly", ModuleCategory.MOVEMENT) {

    private val flyModes = arrayOf(

        /* Vanilla */
        Vanilla(),
        SmoothVanilla(),

        /* NCP */
        NCP(),
        OldNCP(),

        /* AAC */
        // AAC1910(),
        // AAC305(),
        // AAC316-Gomme(),
        // AAC312(),
        // AAC312-Glide(),
        // AAC313(),

        /* CubeCraft */
        // CubeCraft(),

        /* Hypixel */
        // Hypixel(),
        // BoostHypixel(),
        // FreeHypixel(),

        /* Rewinside */
        // Rewinside(),
        // TeleportRewinside(),

        /* Other server specific flys */
        // Mineplex(),
        // NeruxVace(),
        // Minesucht(),
        // Redesky(),

        /* Spartan */
        // Spartan(),
        // Spartan2(),
        // BugSpartan(),

        /* Other anticheats */
        // MineSecure(),
        // HawkEye(),
        // HAC(),
        // WatchCat(),

        /* Other */
        // Jetpack(),
        // KeepAlive(),
        Flag()
    )

    val selectedFlyMode by object : ListValue("Mode", flyModuleModes, "Vanilla") {
        override fun onChange(oldValue: String, newValue: String): String {
            if (state) onDisable()

            return super.onChange(oldValue, newValue)
        }

        override fun onChanged(oldValue: String, newValue: String) {
            if (state) onEnable()
        }
    }

    val vanillaSpeed by FloatValue("VanillaSpeed", 2f, 0f..5f) {
        selectedFlyMode in arrayOf("Vanilla", "SmoothVanilla" /*"MineSecure", "BugSpartan"*/)
    }
    val vanillaKickBypass by BoolValue("VanillaKickBypass", false) {
        selectedFlyMode in arrayOf("Vanilla", "SmoothVanilla")
    }
    val vanillaY by FloatValue("VanillaY", 0f, -5f..5f) { selectedFlyMode == "Vanilla" }
    val vanillaUpwardsY by FloatValue("VanillaUpwardsY", 0f, -5f..5f) { selectedFlyMode == "Vanilla" }
    val vanillaDownwardsY by FloatValue("VanillaDownwardsY", 0f, -5f..5f) { selectedFlyMode == "Vanilla" }
    val vanillaKeepAlive by BoolValue("VanillaKeepAlive", false) { selectedFlyMode == "Vanilla" }
    val vanillaTimer by FloatValue("VanillaTimer", 1f, 0.1f..2f) {
        selectedFlyMode in arrayOf("Vanilla", "SmoothVanilla")
    }
    val vanillaUpwardsY by FloatValue("VanillaUpwardsTimer", 1f, 0.1f..2f) { selectedFlyMode == "Vanilla" }
    val vanillaDownwardsY by FloatValue("VanillaDownwardsTimer", 1f, 0.1f..2f) { selectedFlyMode == "Vanilla" }
    val ncpMotion by FloatValue("NCPMotion", 0f, 0f..1f) { selectedFlyMode == "NCP" }

        // AAC
        /*public val aacSpeed by FloatValue("AAC1.9.10-Speed", 0.3f, 0f..1f) { mode == "AAC1.9.10" }
        public val aacFast by BoolValue("AAC3.0.5-Fast", true) { mode == "AAC3.0.5" }
        public val aacMotion by FloatValue("AAC3.3.12-Motion", 10f, 0.1f..10f) { mode == "AAC3.3.12" }
        public val aacMotion2 by FloatValue("AAC3.3.13-Motion", 10f, 0.1f..10f) { mode == "AAC3.3.13" }

        // Hypixel
        public val hypixelBoost by BoolValue("Hypixel-Boost", true) { mode == "Hypixel" }
        public val hypixelBoostDelay by IntegerValue("Hypixel-BoostDelay", 1200, 0..2000) { mode == "Hypixel" }
        public val hypixelBoostTimer by FloatValue("Hypixel-BoostTimer", 1f, 0f..5f) { mode == "Hypixel" }

        // Other
        public val mineplexSpeed by FloatValue("MineplexSpeed", 1f, 0.5f..10f) { mode == "Mineplex" }
        public val neruxVaceTicks by IntegerValue("NeruxVace-Ticks", 6, 0..20) { mode == "NeruxVace" }
        public val redeskyHeight by FloatValue("Redesky-Height", 4f, 1f..7f) { mode == "Redesky" }*/
    private val stopOnDisable by BoolValue("StopOnDisable", true)

    // Values
    private val groundTimer = MSTimer()
    public val startY = 0.0
    // Variables
    public var noPacketModify = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isSneaking) {
            return
        }

        if (isMoving) {
            thePlayer.isSprinting = true
        }

        flyModeModule?.onUpdate()
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.isSneaking || event.eventState != EventState.PRE) {
            return
        }

        if (isMoving) {
            thePlayer.isSprinting = true
        }

        flyModeModule?.onMotion()
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        if (mc.thePlayer.isSneaking) {
            return
        }

        flyModeModule?.onMove(event)
    }

    @EventTarget
    fun onTick(event: TickEvent) {
        if (mc.thePlayer.isSneaking) {
            return
        }

        flyModeModule?.onTick()
    }

    override fun onEnable() {
        if (mc.thePlayer == null) return

        mc.timer.timerSpeed = 1f

        val startY = mc.thePlayer.posY
        
        flyModeModule?.onEnable()
    }

    override fun onDisable() {
        val thePlayer = mc.thePlayer ?: return

        if (stopOnDisable) {
            thePlayer.motionX = 0.0
            thePlayer.motionY = 0.0
            thePlayer.motionZ = 0.0
        }

        mc.timer.timerSpeed = 1f

        flyModeModule?.onDisable()
    }

    override val tag
        get() = selectedFlyMode

    private val flyModeModule
        get() = flyModes.find { it.flyModeName == selectedFlyMode }

    private val flyModuleModes
        get() = flyModes.map { it.flyModeName }.toTypedArray()
}
