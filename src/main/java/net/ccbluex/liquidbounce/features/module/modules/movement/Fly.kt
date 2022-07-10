/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.BlockBBEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.StepEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.DamageOnStart
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.FlyMode
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac.AAC1_9_10Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac.AAC3_0_5Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac.AAC3_1_6GommeFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac.AAC3_3_12Glide
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac.AAC3_3_12HighJump
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac.AAC3_3_13HighJump
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.aac.AAC4_xGlide
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.hypixel.FreeHypixelFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.hypixel.HypixelFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs.ACPFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs.HACFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs.HawkEyeFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs.MineSecureGlide
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.minorACs.WatchCatFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.ncp.NCPGlide
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.ncp.OldNCPFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.BlockWalkFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.CubeCraftGlide
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.FlagFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.Jetpack
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.KeepAliveFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.MCCentralFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.MineplexFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.MinesuchtFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.MushMCFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.other.NeruxVaceGlide
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.redesky.RedeSkyCollideFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.redesky.RedeSkyGlide
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.redesky.RedeSkySmoothFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.rewinside.RewinsideFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.rewinside.TeleportRewinsideFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.spartan.BugSpartan
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.spartan.Spartan185Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.spartan.Spartan194Glide
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla.SmoothVanillaFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla.TeleportFly
import net.ccbluex.liquidbounce.features.module.modules.movement.flies.vanilla.VanillaFly
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.features.module.modules.render.Bobbing
import net.ccbluex.liquidbounce.features.module.modules.world.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.world.Tower
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.extensions.drawString
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.zeroXYZ
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.network.play.client.C13PacketPlayerAbilities
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11

@ModuleInfo(name = "Fly", description = "Allows you to fly in survival mode.", category = ModuleCategory.MOVEMENT, defaultKeyBinds = [Keyboard.KEY_F])
object Fly : Module()
{
    private val flyModes = arrayOf(

        // Vanilla
        VanillaFly(), SmoothVanillaFly(), TeleportFly(),

        // NCP
        NCPGlide(), OldNCPFly(),

        // AAC
        AAC1_9_10Fly(), AAC3_0_5Fly(), AAC3_1_6GommeFly(), AAC3_3_12HighJump(), AAC3_3_12Glide(), AAC3_3_13HighJump(), AAC4_xGlide(),

        // Spartan
        Spartan185Fly(), Spartan194Glide(), BugSpartan(),

        // Minor anticheats
        ACPFly(), HACFly(), HawkEyeFly(), MineSecureGlide(), WatchCatFly(),

        // Cubecraft
        CubeCraftGlide(),

        // Hypixel
        HypixelFly(), FreeHypixelFly(),

        // Rewinside
        RewinsideFly(), TeleportRewinsideFly(),

        // RedeSky (https://github.com/Project-EZ4H/FDPClient/blob/master/src/main/java/net/ccbluex/liquidbounce/features/module/modules/movement/Fly.java)
        RedeSkyGlide(), RedeSkyCollideFly(), RedeSkySmoothFly(),

        //  MushMC (https://github.com/Project-EZ4H/FDPClient/blob/master/src/main/java/net/ccbluex/liquidbounce/features/module/modules/movement/Fly.java)
        MushMCFly(),

        // Other server specific flies
        MineplexFly(), NeruxVaceGlide(), MinesuchtFly(), MCCentralFly(),

        // Others
        Jetpack(), KeepAliveFly(), FlagFly(), BlockWalkFly()

    )

    private val flyModeMap = mapOf(*flyModes.map { it.modeName to it }.toTypedArray())

    /**
     * Mode
     */
    val modeValue = object : ListValue("Mode", flyModeMap.keys.toTypedArray(), "Vanilla")
    {
        override fun onChange(oldValue: String, newValue: String)
        {
            if (state) onDisable()
        }

        override fun onChanged(oldValue: String, newValue: String)
        {
            if (state) onEnable()
        }
    }

    // <editor-fold desc="Options">
    /**
     * Damage on start
     */
    private val damageOnStartModeValue = ListValue("DamageOnStart", arrayOf("Off", "NCP", "Hypixel"), "Off")

    /**
     * Vanilla
     */
    val baseSpeedValue = FloatValue("Speed", 2f, 0f, 5f, "VanillaSpeed")
    val vanillaKickBypassValue = object : BoolValue("VanillaKickBypass", false)
    {
        override fun showCondition() = modeValue.get().endsWith("Vanilla", ignoreCase = true)
    }

    /**
     * Teleport
     */
    private val teleportGroup = object : ValueGroup("Teleport")
    {
        override fun showCondition() = modeValue.get().equals("Teleport", ignoreCase = true)
    }
    val teleportDistanceValue = FloatValue("Distance", 1.0f, 1.0f, 5.0f, "TeleportDistance")
    val teleportDelayValue = IntegerValue("Delay", 100, 0, 1000, "TeleportDelay")

    /**
     * NCP
     */
    val ncpMotionValue = object : FloatValue("NCPMotion", 0f, 0f, 1f)
    {
        override fun showCondition() = modeValue.get().equals("NCP", ignoreCase = true)
    }

    /**
     * AAC
     */
    val aacSpeedValue = object : FloatValue("AAC1.9.10-Speed", 0.3f, 0f, 5f)
    {
        override fun showCondition() = modeValue.get().equals("AAC1.9.10", ignoreCase = true)
    }

    val aacFast = object : BoolValue("AAC3.0.5-Fast", true)
    {
        override fun showCondition() = modeValue.get().equals("AAC3.0.5", ignoreCase = true)
    }

    private val aac3_3_12Group = object : ValueGroup("AAC3.3.12")
    {
        override fun showCondition() = modeValue.get().equals("AAC3.3.12", ignoreCase = true)
    }
    val aac3_3_12YValue = FloatValue("BoostYPos", -70F, -90F, 0F, "AAC3.3.12-BoostYPos")
    val aac3_3_12MotionValue = FloatValue("Motion", 10f, 0.1f, 10f, "AAC3.3.12-Motion")

    val aac3_3_13_MotionValue = object : FloatValue("AAC3.3.13-Motion", 10f, 0.1f, 10f)
    {
        override fun showCondition() = modeValue.get().equals("AAC3.3.13", ignoreCase = true)
    }

    /**
     * Hypixel
     */
    private val hypixelGroup = object : ValueGroup("Hypixel")
    {
        override fun showCondition() = modeValue.get().equals("Hypixel", ignoreCase = true)
    }

    private val hypixelDamageBoostGroup = ValueGroup("DamageBoost")
    val hypixelDamageBoostEnabledValue: BoolValue = BoolValue("Enabled", false, "Hypixel-DamageBoost")
    val hypixelDamageBoostStartTimingValue = ListValue("BoostTiming", arrayOf("Immediately", "AfterDamage"), "Immediately", "Hypixel-DamageBoost-BoostTiming")
    val hypixelDamageBoostAirStartModeValue = ListValue("AirStartMode", arrayOf("WaitForDamage", "JustFlyWithoutDamageBoost"), "WaitForDamage", "Hypixel-DamageBoost-AirStartMode")

    private val hypixelTimerBoostGroup = ValueGroup("TimerBoost")
    val hypixelTimerBoostEnabledValue = BoolValue("Enabled", true, "Hypixel-TimerBoost")
    val hypixelTimerBoostTimerValue = FloatValue("Timer", 1f, 0f, 5f, "Hypixel-TimerBoost-BoostTimer")
    val hypixelTimerBoostDelayValue = IntegerValue("Duration", 1200, 0, 2000, "Hypixel-TimerBoost-BoostDelay")

    val hypixelOnGroundValue = BoolValue("OnGround", false, "Hypixel-OnGround")
    val hypixelYchIncValue = BoolValue("ychinc", true, "Hypixel-ychinc")
    val hypixelJumpValue = BoolValue("Jump", false, "Hypixel-Jump")

    /**
     * Mineplex
     */
    val mineplexSpeedValue = object : FloatValue("MineplexSpeed", 1f, 0.5f, 10f)
    {
        override fun showCondition() = modeValue.get().equals("Mineplex", ignoreCase = true)
    }

    /**
     * MushMC
     */
    private val mushMCGroup = object : ValueGroup("MushMC")
    {
        override fun showCondition() = modeValue.get().equals("MushMC", ignoreCase = true)
    }
    val mushMCSpeedValue = FloatValue("Speed", 3f, 1f, 5f, "MushSpeed")
    val mushMCBoostDelay = IntegerValue("BoostDelay", 10, 0, 20, "MushBoostDelay")

    /**
     * RedeSky
     */
    private val redeSkyCollideGroup = object : ValueGroup("RedeSkyCollide")
    {
        override fun showCondition() = modeValue.get().equals("RedeSky-Collide", ignoreCase = true)
    }
    val redeSkyCollideSpeedValue: FloatValue = FloatValue("Speed", 15.5f, 0f, 30f, "RSCollideSpeed")
    val redeSkyCollideBoostValue = FloatValue("Boost", 0.3f, 0.0f, 1f, "RSCollideBoost")
    val redeSkyCollideMaxSpeedValue = FloatValue("MaxSpeed", 20f, 7f, 30f, "RSCollideMaxSpeed")
    val redeSkyCollideTimerValue = FloatValue("Timer", 0.8f, 0.1f, 1f, "RSCollideTimer")

    private val redeSkySmoothGroup = object : ValueGroup("RedeSkySmooth")
    {
        override fun showCondition() = modeValue.get().equals("RedeSky-Smooth", ignoreCase = true)
    }
    val redeSkySmoothSpeedValue = FloatValue("Speed", 0.9f, 0.05f, 1f, "RSSmoothSpeed")
    val redeSkySmoothSpeedChangeValue = FloatValue("ChangeSpeed", 0.1f, -1f, 1f, "RSSmoothChangeSpeed")
    val redeSkySmoothMotionValue = FloatValue("Motion", 0.2f, 0f, 0.5f, "RSSmoothMotion")
    val redeSkySmoothTimerValue = FloatValue("Timer", 0.3f, 0.1f, 1f, "RSSmoothTimer")
    val redeSkySmoothDropoffValue = FloatValue("Dropoff", 1f, 0f, 5f, "RSSmoothDropoff")
    val redeSkySmoothDropoffAValue = BoolValue("DropoffA", true, "RSSmoothDropoffA")

    val neruxVaceTicks = object : IntegerValue("NeruxVace-Ticks", 6, 0, 20)
    {
        override fun showCondition() = modeValue.get().equals("NeruxVace", ignoreCase = true)
    }

    val redeskyVClipHeight = object : FloatValue("RedeSkyGlideHeight", 4f, 1f, 7f, "RedeSky-Height")
    {
        override fun showCondition() = modeValue.get().equals("RedeSky-Glide", ignoreCase = true)
    }

    val mccTimerSpeedValue = object : FloatValue("MCCentral-Timer", 2.0f, 1.0f, 5.0f)
    {
        override fun showCondition() = modeValue.get().equals("MCCentral", ignoreCase = true)
    }

    /**
     * Reset Motions On Disable
     */
    private val resetMotionOnDisable = BoolValue("ResetMotionOnDisable", false)

    private val bypassAbilitiesValue = BoolValue("BlockFlyingAbilities", false)

    /**
     * Visuals
     */
    private val visualGroup = ValueGroup("Visual")
    private val visualBobValue = BoolValue("Bob", true, "Bob")
    private val visualMarkValue = BoolValue("Mark", true, "Mark")

    private val visualVanillaFlightRemainingTimeCounterGroup = object : ValueGroup("VanillaFlightRemainingTimeCounter")
    {
        override fun showCondition() = modeValue.get().endsWith("Vanilla", ignoreCase = true)
    }
    private val visualVanillaFlightRemainingTimeCounterEnabledValue = BoolValue("Enabled", false, "VanillaFlightRemainingTimeCounter")
    private val visualVanillaFlightRemainingTimeCounterFontValue = FontValue("Font", Fonts.font40)
    // </editor-fold>

    private var mode: FlyMode? = null

    /**
     * Timers
     */
    val groundTimer = MSTimer()

    private val vanillaRemainingTime = TickTimer()

    /**
     * Visual variables
     */
    var startY = 0.0
    var markStartY = 0.0

    init
    {
        teleportGroup.addAll(teleportDistanceValue, teleportDelayValue)

        aac3_3_12Group.addAll(aac3_3_12YValue, aac3_3_12MotionValue)

        hypixelGroup.addAll(hypixelDamageBoostGroup, hypixelTimerBoostGroup, hypixelOnGroundValue, hypixelYchIncValue, hypixelJumpValue)
        hypixelDamageBoostGroup.addAll(hypixelDamageBoostEnabledValue, hypixelDamageBoostStartTimingValue, hypixelDamageBoostAirStartModeValue)
        hypixelTimerBoostGroup.addAll(hypixelTimerBoostEnabledValue, hypixelTimerBoostTimerValue, hypixelTimerBoostDelayValue)

        mushMCGroup.addAll(mushMCSpeedValue, mushMCBoostDelay)

        redeSkyCollideGroup.addAll(redeSkyCollideSpeedValue, redeSkyCollideBoostValue, redeSkyCollideMaxSpeedValue, redeSkyCollideTimerValue)
        redeSkySmoothGroup.addAll(redeSkySmoothSpeedValue, redeSkySmoothSpeedChangeValue, redeSkySmoothMotionValue, redeSkySmoothTimerValue, redeSkySmoothDropoffValue, redeSkySmoothDropoffAValue)

        visualVanillaFlightRemainingTimeCounterGroup.addAll(visualVanillaFlightRemainingTimeCounterEnabledValue, visualVanillaFlightRemainingTimeCounterFontValue)
        visualGroup.addAll(visualBobValue, visualMarkValue, visualVanillaFlightRemainingTimeCounterGroup)
    }

    override fun onEnable()
    {
        mode = flyModeMap[modeValue.get()]

        val thePlayer = mc.thePlayer ?: return

        vanillaRemainingTime.reset()

        if (thePlayer.onGround)
        {
            val modeDOS = mode?.damageOnStart
            val selectedDOS = DamageOnStart.byName(damageOnStartModeValue.get())
            (if (modeDOS != DamageOnStart.OFF) modeDOS else selectedDOS)?.execute?.invoke()
        }

        mode?.onEnable()

        startY = thePlayer.posY.also { markStartY = it } // apply y change caused by jump() and redeskyVClip()
    }

    override fun onDisable()
    {
        val thePlayer = mc.thePlayer ?: return

        mode?.onDisable()

        if (resetMotionOnDisable.get()) thePlayer.zeroXYZ()

        thePlayer.capabilities.isFlying = false
        mc.timer.timerSpeed = 1f
        thePlayer.speedInAir = 0.02f

        lastAbilitiesPacket = null
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        vanillaRemainingTime.update()

        mode?.onUpdate()
    }

    @EventTarget
    fun onMotion(event: MotionEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (LiquidBounce.moduleManager[Bobbing::class.java].state && visualBobValue.get() && thePlayer.isMoving) thePlayer.cameraYaw = 0.1f

        mode?.onMotion(event.eventState)
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        if (!visualMarkValue.get()) return

        if (mode?.mark == true)
        {
            val y = markStartY + 2.0
            RenderUtils.drawPlatform(y, if ((mc.thePlayer ?: return).entityBoundingBox.maxY < y) 0x5A00FF00 else 0x5AFF0000, 1.0)
        }

        mode?.onRender3D(event.partialTicks)
    }

    @EventTarget
    fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
    {
        if (visualVanillaFlightRemainingTimeCounterEnabledValue.get())
        {
            val theWorld = mc.theWorld ?: return
            GL11.glPushMatrix()

            val moduleManager = LiquidBounce.moduleManager

            val blockOverlay = moduleManager[BlockOverlay::class.java] as BlockOverlay
            if (blockOverlay.state && blockOverlay.infoEnabledValue.get() && blockOverlay.getCurrentBlock(theWorld) != null) GL11.glTranslatef(0f, 15f, 0f)

            val scaffold = moduleManager[Scaffold::class.java] as Scaffold
            val tower = moduleManager[Tower::class.java] as Tower
            if (scaffold.state && scaffold.visualCounterEnabledValue.get() || tower.state && tower.counterEnabledValue.get()) GL11.glTranslatef(0f, 15f, 0f)

            val font = visualVanillaFlightRemainingTimeCounterFontValue.get()
            val remainingTicks = 80 - vanillaRemainingTime.tick.coerceAtMost(80)
            val info = "You can fly ${if (remainingTicks <= 10) "\u00A7c" else ""}${remainingTicks}\u00A7r more ticks"
            val scaledResolution = ScaledResolution(mc)

            RenderUtils.drawBorderedRect((scaledResolution.scaledWidth shr 1) - 2.0f, (scaledResolution.scaledHeight shr 1) + 5.0f, ((scaledResolution.scaledWidth shr 1) + font.getStringWidth(info)) + 2.0f, (scaledResolution.scaledHeight shr 1) + font.FONT_HEIGHT + 7f, 3f, -16777216, -16777216)

            GlStateManager.resetColor()

            font.drawString(info, (scaledResolution.scaledWidth shr 1).toFloat(), (scaledResolution.scaledHeight shr 1) + 7.0f, 0xffffff)

            GL11.glPopMatrix()
        }
    }

    private var lastAbilitiesPacket: C13PacketPlayerAbilities? = null

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (packet is C13PacketPlayerAbilities)
        {
            val thePlayer = mc.thePlayer ?: return

            if (bypassAbilitiesValue.get())
            {
                if (lastAbilitiesPacket == null)
                {
                    lastAbilitiesPacket = C13PacketPlayerAbilities(thePlayer.capabilities)
                    lastAbilitiesPacket!!.isFlying = false
                }

                packet.isFlying = false

                if (lastAbilitiesPacket != null && lastAbilitiesPacket == packet) event.cancelEvent() else lastAbilitiesPacket = packet
            }
        }

        mode?.onPacket(event)
    }

    @EventTarget
    fun onMove(event: MoveEvent)
    {
        mode?.onMove(event)
    }

    @EventTarget
    fun onBB(event: BlockBBEvent)
    {
        mode?.onBlockBB(event)
    }

    @EventTarget
    fun onJump(event: JumpEvent)
    {
        mode?.onJump(event)
    }

    @EventTarget
    fun onStep(event: StepEvent)
    {
        mode?.onStep(event)
    }

    override val tag: String
        get() = modeValue.get()

    val shouldDisableNoFall: Boolean
        get() = mode?.shouldDisableNoFall == true
}
