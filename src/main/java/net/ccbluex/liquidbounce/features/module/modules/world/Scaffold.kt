/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */


package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoUse
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.CPSCounter
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.PlaceRotation
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.block.SearchInfo
import net.ccbluex.liquidbounce.utils.extensions.BLACKLISTED_BLOCKS
import net.ccbluex.liquidbounce.utils.extensions.boost
import net.ccbluex.liquidbounce.utils.extensions.canAutoBlock
import net.ccbluex.liquidbounce.utils.extensions.canBeClicked
import net.ccbluex.liquidbounce.utils.extensions.drawString
import net.ccbluex.liquidbounce.utils.extensions.equalTo
import net.ccbluex.liquidbounce.utils.extensions.findAutoBlockBlock
import net.ccbluex.liquidbounce.utils.extensions.getBlock
import net.ccbluex.liquidbounce.utils.extensions.getBlockCollisionBox
import net.ccbluex.liquidbounce.utils.extensions.isReplaceable
import net.ccbluex.liquidbounce.utils.extensions.moveDirectionDegrees
import net.ccbluex.liquidbounce.utils.extensions.plus
import net.ccbluex.liquidbounce.utils.extensions.serialize
import net.ccbluex.liquidbounce.utils.extensions.strafe
import net.ccbluex.liquidbounce.utils.extensions.times
import net.ccbluex.liquidbounce.utils.extensions.toDegrees
import net.ccbluex.liquidbounce.utils.extensions.withParentheses
import net.ccbluex.liquidbounce.utils.extensions.wrapAngleTo180
import net.ccbluex.liquidbounce.utils.misc.StringUtils.DECIMALFORMAT_6
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatRangeValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.FontValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.ccbluex.liquidbounce.value.RGBAColorValue
import net.ccbluex.liquidbounce.value.ValueGroup
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.client.entity.EntityPlayerSP
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.EnumFacing.Axis
import net.minecraft.util.EnumFacing.UP
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.world.World
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max

@ModuleInfo(name = "Scaffold", description = "Automatically places blocks beneath your feet.", category = ModuleCategory.WORLD, defaultKeyBinds = [Keyboard.KEY_I])
class Scaffold : Module()
{
    private val modeValue = ListValue("Mode", arrayOf("Normal", "Rewinside", "Expand"), "Normal")

    private val delayGroup = ValueGroup("Delay")
    private val delayValue = IntegerRangeValue("Delay", 0, 0, 0, 1000, "MaxDelay" to "MinDelay")
    private val delaySwitchValue = IntegerRangeValue("SwitchSlotDelay", 0, 0, 0, 1000, "MaxSwitchSlotDelay" to "MinSwitchSlotDelay")
    private val delayPlaceableDelayValue = BoolValue("PlaceableDelay", true)

    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post")

    private val autoBlockGroup = ValueGroup("AutoBlock")
    private val autoBlockModeValue = ListValue("Mode", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof", "AutoBlock")
    private val autoBlockSwitchKeepTimeValue = object : IntegerValue("SwitchKeepTime", -1, -1, 10, "AutoBlockSwitchKeepTime", "-1 : Return to original slot immediately after block place, 0~ : Put a small tick delay between returning back to original slot")
    {
        override fun showCondition() = !autoBlockModeValue.get().equals("None", ignoreCase = true)
    }
    private val autoBlockFullCubeOnlyValue = object : BoolValue("FullCubeOnly", false, "AutoBlockFullCubeOnly")
    {
        override fun showCondition() = !autoBlockModeValue.get().equals("None", ignoreCase = true)
    }

    private val expandLengthValue = object : IntegerValue("ExpandLength", 1, 1, 6)
    {
        override fun showCondition() = modeValue.get().equals("Expand", ignoreCase = true)
    }

    private val disableWhileTowering: BoolValue = BoolValue("DisableWhileTowering", true)

    private val rotationGroup = ValueGroup("Rotation")

    private val rotationModeValue = ListValue("Mode", arrayOf("Off", "Normal", "Static", "StaticPitch", "StaticYaw"), "Normal", "RotationMode")

    private val rotationSearchGroup = ValueGroup("Search")
    private val rotationSearchSearchValue = BoolValue("Search", true, "Search")
    private val rotationSearchSearchRangeValue = object : IntegerValue("SearchRange", 1, 1, 3)
    {
        override fun showCondition(): Boolean = rotationSearchSearchValue.get()
    }
    private val rotationSearchYSearchValue = BoolValue("YSearch", false, "YSearch")
    private val rotationSearchYSearchRangeValue = object : IntegerValue("YSearchRange", 1, 1, 3)
    {
        override fun showCondition(): Boolean = rotationSearchSearchValue.get()
    }
    private val rotationSearchXZRangeValue = FloatValue("XZRange", 0.8f, 0f, 1f, "XZRange")
    private val rotationSearchYRangeValue = FloatValue("YRange", 0.8f, 0f, 1f, "YRange")
    private val rotationSearchMinDiffValue = FloatValue("MinDiff", 0f, 0f, 0.2f, "MinDiff")
    private val rotationSearchStepsValue = IntegerValue("Steps", 8, 1, 16, "SearchAccuracy")
    private val rotationSearchCheckVisibleValue = BoolValue("CheckVisible", true, "CheckVisible")

    private val rotationSearchStaticYawValue = object : FloatValue("StaticYawOffSet", 0f, 0f, 90f, "StaticYawOffSet")
    {
        override fun showCondition(): Boolean
        {
            val rotationMode = rotationModeValue.get()
            return rotationMode.equals("Static", ignoreCase = true) || rotationMode.equals("StaticYaw", ignoreCase = true)
        }
    }
    private val rotationSearchStaticPitchValue = object : FloatValue("StaticPitchOffSet", 86f, 70f, 90f, "StaticPitchOffSet")
    {
        override fun showCondition(): Boolean
        {
            val rotationMode = rotationModeValue.get()
            return rotationMode.equals("Static", ignoreCase = true) || rotationMode.equals("StaticPitch", ignoreCase = true)
        }
    }

    private val rotationTurnSpeedValue = FloatRangeValue("TurnSpeed", 180f, 180f, 0f, 180f, "MaxTurnSpeed" to "MinTurnSpeed")
    private val rotationResetSpeedValue = FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")

    private val rotationSilentValue = BoolValue("SilentRotation", true, "SilentRotation")
    private val rotationStrafeValue = BoolValue("Strafe", false, "RotationStrafe")

    private val rotationKeepRotationGroup = ValueGroup("KeepRotation")
    private val rotationKeepRotationEnabledValue = BoolValue("Enabled", true, "KeepRotation")
    private val rotationKeepRotationLockValue = object : BoolValue("Lock", false, "LockRotation")
    {
        override fun showCondition() = rotationKeepRotationEnabledValue.get()
    }
    private val rotationKeepRotationTicksValue = object : IntegerRangeValue("Ticks", 20, 30, 0, 60, "MinKeepRotationTicks" to "MaxKeepRotationTicks")
    {
        override fun showCondition() = rotationKeepRotationEnabledValue.get() && !rotationKeepRotationLockValue.get()
    }

    private val swingValue = BoolValue("Swing", true)

    private val movementGroup = ValueGroup("Movement")

    val movementSprintValue: BoolValue = object : BoolValue("Sprint", false, "Sprint")
    {
        override fun onChanged(oldValue: Boolean, newValue: Boolean)
        {
            if (newValue) movementSlowEnabledValue.set(false)
        }
    }

    private val movementEagleGroup = ValueGroup("Eagle")
    private val movementEagleModeValue = ListValue("Mode", arrayOf("Normal", "EdgeDistance", "Silent", "SilentEdgeDistance", "Off"), "Normal", "Eagle")
    private val movementEagleBlocksToEagleValue = object : IntegerValue("BlocksToEagle", 0, 0, 10)
    {
        override fun showCondition() = !movementEagleModeValue.get().equals("Off", ignoreCase = true)
    }
    private val movementEagleEdgeDistanceValue = object : FloatValue("EagleEdgeDistance", 0.2f, 0f, 0.5f)
    {
        override fun showCondition() = movementEagleModeValue.get().endsWith("EdgeDistance", ignoreCase = true)
    }

    private val movementZitterGroup = ValueGroup("Zitter")
    private val movementZitterModeValue = ListValue("Mode", arrayOf("Off", "Teleport", "Smooth"), "Off", "ZitterMode")
    private val movementZitterIntensityValue = object : FloatValue("Intensity", 0.05f, 0f, 0.2f, "ZitterStrength")
    {
        override fun showCondition() = movementZitterModeValue.get().equals("Teleport", ignoreCase = true)
    }
    private val movementZitterSpeedValue = object : FloatValue("Speed", 0.13f, 0.05f, 0.4f, "ZitterSpeed")
    {
        override fun showCondition() = movementZitterModeValue.get().equals("Teleport", ignoreCase = true)
    }

    private val movementSlowGroup = ValueGroup("Slow")
    private val movementSlowEnabledValue = object : BoolValue("Enabled", false, "Slow")
    {
        override fun onChanged(oldValue: Boolean, newValue: Boolean)
        {
            if (newValue) movementSprintValue.set(false)
        }
    }
    private val movementSlowSpeedValue = FloatValue("Speed", 0.6f, 0.2f, 0.8f, "SlowSpeed")

    private val movementSafeWalkValue = BoolValue("SafeWalk", true, "SafeWalk")
    private val movementAirSafeValue = object : BoolValue("AirSafe", false, "AirSafe")
    {
        override fun showCondition(): Boolean = movementSafeWalkValue.get()
    }

    private val timerValue = FloatValue("Timer", 1f, 0.1f, 10f)
    private val speedModifierValue = FloatValue("SpeedModifier", 1f, 0f, 2f)

    private val sameYValue: BoolValue = object : BoolValue("SameY", false)
    {
        override fun onChanged(oldValue: Boolean, newValue: Boolean)
        {
            if (newValue) downValue.set(false)
        }
    }
    private val downValue: BoolValue = object : BoolValue("Downward", true, "Down")
    {
        override fun onChanged(oldValue: Boolean, newValue: Boolean)
        {
            if (newValue) sameYValue.set(false)
        }
    }

    private val killAuraBypassGroup = ValueGroup("KillAuraBypass")
    val killauraBypassModeValue = ListValue("Mode", arrayOf("None", "SuspendKillAura", "WaitForKillAuraEnd"), "SuspendKillAura", "KillAuraBypassMode")
    private val killAuraBypassKillAuraSuspendDurationValue = object : IntegerValue("Duration", 300, 100, 1000, "SuspendKillAuraDuration")
    {
        override fun showCondition() = killauraBypassModeValue.get().equals("SuspendKillAura", ignoreCase = true)
    }

    private val stopConsumingBeforePlaceValue = BoolValue("StopConsumingBeforePlace", true)

    private val visualGroup = ValueGroup("Visual")

    private val visualCounterGroup = ValueGroup("Counter")
    val visualCounterEnabledValue = BoolValue("Enabled", true, "Counter")
    private val visualCounterFontValue = FontValue("Font", Fonts.font40)

    private val visualMarkValue = BoolValue("Mark", false, "Mark")

    private val visualDebugGroup = ValueGroup("Debug")
    private val visualDebugFindBlockValue = BoolValue("Search", false, "SearchDebugChat")
    private val visualDebugPlaceValue = BoolValue("Place", false)

    private val visualDebugRayGroup = object : ValueGroup("Ray")
    {
        override fun showCondition(): Boolean = rotationSearchCheckVisibleValue.get()
    }
    private val visualDebugRayEnabledValue = BoolValue("Enabled", false)

    private val visualDebugRayColorGroup = object : ValueGroup("Color")
    {
        override fun showCondition(): Boolean = visualDebugRayEnabledValue.get()
    }
    private val visualDebugRayColorReachValue = RGBAColorValue("Reach", 255, 0, 0, 80)
    private val visualDebugRayColorExpectValue = RGBAColorValue("Expect", 255, 0, 0, 80)
    private val visualDebugRayColorActualValue = RGBAColorValue("Actual", 255, 0, 0, 80)

    // MODULE

    // Target block
    private var targetPlace: PlaceInfo? = null
    private var lastSearchBound: SearchBounds? = null
    private var targetSearchInfo: SearchInfo? = null

    // Rotation lock
    var lockRotation: Rotation? = null
    private var limitedRotation: Rotation? = null

    // Launch position
    private var launchY = -999
    private var facesBlock = false

    // Zitter Direction
    private var zitterDirection = false

    // Delay
    private val delayTimer = MSTimer()
    private val zitterTimer = MSTimer()
    private val switchTimer = MSTimer()
    private val flagTimer = MSTimer()
    private var delay = 0L
    private var switchDelay = 0L

    // Eagle
    private var placedBlocksWithoutEagle = 0
    private var eagleSneaking: Boolean = false

    // Downwards
    private var shouldGoDown: Boolean = false

    // Last Ground Block
    private var lastGroundBlockState: IBlockState? = null

    private var lastSearchPosition: BlockPos? = null

    // Falling Started On YPosition
    private var fallStartY = 0.0

    private var findBlockDebug: String? = null
    private var placeDebug: String? = null

    private fun setFindBlockDebug(supplier: () -> String?)
    {
        if (visualDebugFindBlockValue.get()) findBlockDebug = supplier()
    }

    private fun setPlaceDebug(supplier: () -> String?)
    {
        if (visualDebugPlaceValue.get()) placeDebug = supplier()
    }

    private val searchRanges = hashMapOf<Pair<Int, Int>, Pair<List<Pair<Int, Int>>, List<Int>>>()

    init
    {
        delayGroup.addAll(delayValue, delaySwitchValue, delayPlaceableDelayValue)
        autoBlockGroup.addAll(autoBlockModeValue, autoBlockSwitchKeepTimeValue, autoBlockFullCubeOnlyValue)
        rotationSearchGroup.addAll(rotationSearchSearchValue, rotationSearchSearchRangeValue, rotationSearchYSearchValue, rotationSearchYSearchRangeValue, rotationSearchXZRangeValue, rotationSearchYRangeValue, rotationSearchMinDiffValue, rotationSearchStepsValue, rotationSearchCheckVisibleValue)
        rotationKeepRotationGroup.addAll(rotationKeepRotationEnabledValue, rotationKeepRotationLockValue, rotationKeepRotationTicksValue)
        rotationGroup.addAll(rotationModeValue, rotationSearchGroup, rotationSearchStaticYawValue, rotationSearchStaticPitchValue, rotationTurnSpeedValue, rotationResetSpeedValue, rotationSilentValue, rotationStrafeValue, rotationKeepRotationGroup)
        movementEagleGroup.addAll(movementEagleModeValue, movementEagleBlocksToEagleValue, movementEagleEdgeDistanceValue)
        movementZitterGroup.addAll(movementZitterModeValue, movementZitterIntensityValue, movementZitterSpeedValue)
        movementSlowGroup.addAll(movementSlowEnabledValue, movementSlowSpeedValue)
        movementGroup.addAll(movementSprintValue, movementEagleGroup, movementZitterGroup, movementSlowGroup, movementSafeWalkValue, movementAirSafeValue)
        killAuraBypassGroup.addAll(killauraBypassModeValue, killAuraBypassKillAuraSuspendDurationValue)
        visualCounterGroup.addAll(visualCounterEnabledValue, visualCounterFontValue)
        visualDebugRayColorGroup.addAll(visualDebugRayColorReachValue, visualDebugRayColorExpectValue, visualDebugRayColorActualValue)
        visualDebugRayGroup.addAll(visualDebugRayEnabledValue, visualDebugRayColorGroup)
        visualDebugGroup.addAll(visualDebugFindBlockValue, visualDebugPlaceValue, visualDebugRayGroup)
        visualGroup.addAll(visualCounterGroup, visualMarkValue, visualDebugGroup)
    }

    // ENABLING MODULE
    override fun onEnable()
    {
        val thePlayer = mc.thePlayer ?: return

        launchY = thePlayer.posY.toInt()

        findBlockDebug = null
        placeDebug = null

        val rotationMode = rotationModeValue.get()
        if (modeValue.get().equals("Expand", ignoreCase = true) && (rotationMode.equals("Static", ignoreCase = true) || rotationMode.equals("StaticPitch", ignoreCase = true))) ClientUtils.displayChatMessage(thePlayer, "\u00A78[\u00A7aScaffold\u00A78] \u00A7aUsing Expand scaffold with Static/StaticPitch rotation mode can decrease your expand length!")
    }

    // UPDATE EVENTS

    @EventTarget
    private fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val gameSettings = mc.gameSettings

        mc.timer.timerSpeed = timerValue.get()

        shouldGoDown = downValue.get() && !sameYValue.get() && GameSettings.isKeyDown(gameSettings.keyBindSneak) && getBlocksAmount(thePlayer) > 1
        if (shouldGoDown) gameSettings.keyBindSneak.pressed = false

        // Slow
        if (movementSlowEnabledValue.get())
        {
            thePlayer.motionX = thePlayer.motionX * movementSlowSpeedValue.get()
            thePlayer.motionZ = thePlayer.motionZ * movementSlowSpeedValue.get()
        }

        // Sprint
        // This can cause compatibility issue with other mods which tamper the sprinting state (example: BetterSprinting)
        //		if (sprintValue.get())
        //		{
        //			if (mc.gameSettings.keyBindSprint !is KeyDown) mc.gameSettings.keyBindSprint.pressed = false
        //			if (mc.gameSettings.keyBindSprint is KeyDown) mc.gameSettings.keyBindSprint.pressed = true
        //			if (mc.gameSettings.keyBindSprint.isKeyDown) thePlayer.sprinting = true
        //			if (!mc.gameSettings.keyBindSprint.isKeyDown) thePlayer.sprinting = false
        //		}

        if (thePlayer.onGround)
        {
            if (modeValue.get().equals("Rewinside", ignoreCase = true))
            {
                thePlayer.strafe(0.2F)
                thePlayer.motionY = 0.0
            }

            // Eagle
            if (!movementEagleModeValue.get().equals("Off", true) && !shouldGoDown)
            {
                var dif = 0.5
                val blockPos = BlockPos(mc.thePlayer!!.posX, mc.thePlayer!!.posY - 1.0, mc.thePlayer!!.posZ)
                if (movementEagleEdgeDistanceValue.get() > 0)
                {
                    for (side in EnumFacing.values())
                    {
                        if (side.axis == Axis.Y) continue
                        val neighbor = blockPos.offset(side)
                        if (theWorld.isReplaceable(neighbor))
                        {
                            val calcDif = ((if (side.axis == Axis.Z) abs((neighbor.z + 0.5) - mc.thePlayer!!.posZ)
                            else abs((neighbor.x + 0.5) - mc.thePlayer!!.posX))) - 0.5

                            if (calcDif < dif) dif = calcDif
                        }
                    }
                }
                if (placedBlocksWithoutEagle >= movementEagleBlocksToEagleValue.get())
                {
                    val shouldEagle = theWorld.isReplaceable(blockPos) || (movementEagleEdgeDistanceValue.get() > 0 && dif < movementEagleEdgeDistanceValue.get())
                    if (movementEagleModeValue.get().equals("Silent", true))
                    {
                        if (eagleSneaking != shouldEagle)
                        {
                            mc.netHandler.addToSendQueue(C0BPacketEntityAction(mc.thePlayer!!, if (shouldEagle) C0BPacketEntityAction.Action.START_SNEAKING
                            else C0BPacketEntityAction.Action.STOP_SNEAKING))
                        }
                        eagleSneaking = shouldEagle
                    }
                    else mc.gameSettings.keyBindSneak.pressed = shouldEagle
                    placedBlocksWithoutEagle = 0
                }
                else placedBlocksWithoutEagle++
            }

            // Smooth Zitter
            when (movementZitterModeValue.get().toLowerCase())
            {
                "smooth" ->
                {
                    val keyBindRight = gameSettings.keyBindRight
                    val keyBindLeft = gameSettings.keyBindLeft

                    if (!GameSettings.isKeyDown(keyBindRight)) keyBindRight.pressed = false
                    if (!GameSettings.isKeyDown(keyBindLeft)) keyBindLeft.pressed = false

                    if (zitterTimer.hasTimePassed(100))
                    {
                        zitterDirection = !zitterDirection
                        zitterTimer.reset()
                    }

                    if (zitterDirection)
                    {
                        keyBindRight.pressed = true
                        keyBindLeft.pressed = false
                    }
                    else
                    {
                        keyBindRight.pressed = false
                        keyBindLeft.pressed = true
                    }
                }

                "teleport" ->
                {
                    thePlayer.strafe(movementZitterSpeedValue.get())
                    thePlayer.boost(movementZitterIntensityValue.get(), thePlayer.rotationYaw + if (zitterDirection) 90f else -90f)
                    zitterDirection = !zitterDirection
                }
            }
        }
    }

    @EventTarget
    fun onStrafe(event: StrafeEvent)
    {
        if (!rotationStrafeValue.get()) return
        RotationUtils.serverRotation.applyStrafeToPlayer(event)
        event.cancelEvent()
    }

    @EventTarget(ignoreCondition = true)
    fun onMotion(event: MotionEvent)
    {
        val eventState: EventState = event.eventState

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (eventState == EventState.PRE)
        {
            if (!thePlayer.onGround && thePlayer.motionY < 0)
            {
                if (fallStartY < thePlayer.posY) fallStartY = thePlayer.posY
            }
            else fallStartY = 0.0

            updateGroundBlock(theWorld, thePlayer)
        }

        if (!state) return

        val tower = LiquidBounce.moduleManager[Tower::class.java] as Tower
        if (disableWhileTowering.get() && tower.active)
        {
            launchY = thePlayer.posY.toInt() // Compatibility between SameY and Tower
            return
        }

        val currentLockRotation = lockRotation

        // Lock Rotation
        if (!rotationModeValue.get().equals("Off", true) && rotationKeepRotationEnabledValue.get() && rotationKeepRotationLockValue.get() && currentLockRotation != null) setRotation(thePlayer, currentLockRotation)

        // Place block
        if ((facesBlock || rotationModeValue.get().equals("Off", true)) && placeModeValue.get().equals(eventState.stateName, true)) place(theWorld, thePlayer)

        // Update and search for a new block
        if (eventState == EventState.PRE) update(theWorld, thePlayer)

        // Reset placeable delay
        if (targetPlace == null && delayPlaceableDelayValue.get()) delayTimer.reset()
    }

    @EventTarget
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (packet is S08PacketPlayerPosLook)
        {
            flagTimer.reset()
            launchY = packet.y.toInt()
        }
    }

    private fun update(theWorld: World, thePlayer: EntityPlayerSP)
    {
        val heldItem = thePlayer.heldItem
        val heldItemIsNotBlock: Boolean = heldItem == null || heldItem.item !is ItemBlock

        if (if (autoBlockModeValue.get().equals("Off", true)) heldItemIsNotBlock else thePlayer.inventoryContainer.findAutoBlockBlock(theWorld, autoBlockFullCubeOnlyValue.get()) == -1 && heldItemIsNotBlock) return
        findBlock(theWorld, thePlayer, modeValue.get().equals("expand", true))
    }

    private fun updateGroundBlock(theWorld: World, thePlayer: EntityPlayerSP)
    {
        val groundSearchDepth = 0.2

        val pos = BlockPos(thePlayer.posX, thePlayer.posY - groundSearchDepth, thePlayer.posZ)
        val bs: IBlockState = theWorld.getBlockState(pos)
        if (!theWorld.isReplaceable(bs)) lastGroundBlockState = bs
        else if (lastGroundBlockState == null) lastGroundBlockState = Blocks.stone.defaultState // Failsafe
    }

    private fun setRotation(thePlayer: EntityPlayer, rotation: Rotation, keepRotation: Int)
    {
        if (rotationSilentValue.get())
        {
            RotationUtils.setTargetRotation(rotation, keepRotation)
            RotationUtils.setNextResetTurnSpeed(rotationResetSpeedValue.getMin().coerceAtLeast(10F), rotationResetSpeedValue.getMax().coerceAtLeast(10F))
        }
        else
        {
            thePlayer.rotationYaw = rotation.yaw
            thePlayer.rotationPitch = rotation.pitch
        }
    }

    private fun setRotation(thePlayer: EntityPlayer, rotation: Rotation)
    {
        setRotation(thePlayer, rotation, 0)
    }

    // Search for new target block
    private fun findBlock(theWorld: World, thePlayer: EntityPlayerSP, expand: Boolean)
    {
        val failedResponce = { reason: String ->
            setFindBlockDebug(listOf("result".equalTo("FAILED", "\u00A74"), "reason" equalTo reason.withParentheses("\u00A74"))::serialize)
        }

        val groundBlockState = lastGroundBlockState ?: run {
            failedResponce("lastGroundBlockState = null")
            return@findBlock
        }

        val groundBlockBB = theWorld.getBlockCollisionBox(groundBlockState) ?: run {
            failedResponce("lastGroundBlockState.blockCollisionBox = null")
            return@findBlock
        }

        // get the block that will be automatically placed
        var autoBlock: ItemStack? = thePlayer.heldItem

        if (autoBlock == null || autoBlock.item !is ItemBlock || autoBlock.stackSize <= 0 || autoBlock.item?.let { !(it as ItemBlock).block.canAutoBlock } != false)
        {
            if (autoBlockModeValue.get().equals("Off", true))
            {
                failedResponce("(AutoBlock) Insufficient block")
                return
            }

            val inventoryContainer = thePlayer.inventoryContainer

            val autoBlockSlot = inventoryContainer.findAutoBlockBlock(theWorld, autoBlockFullCubeOnlyValue.get())
            if (autoBlockSlot == -1)
            {
                failedResponce("(Hand) Insufficient block")
                return
            }

            autoBlock = inventoryContainer.getSlot(autoBlockSlot).stack
        }

        val autoBlockBlock = ((autoBlock?.item ?: run {
            failedResponce("autoBlock = null OR autoBlock.item = null")
            return@findBlock
        }) as ItemBlock).block

        val abCollisionBB = theWorld.getBlockCollisionBox(if (Block.isEqualTo(groundBlockState.block, autoBlockBlock)) groundBlockState
        else autoBlockBlock.defaultState ?: run {
            failedResponce("BlockState unavailable")
            return@findBlock
        }) ?: run {
            failedResponce("BlockCollisionBox unavailable")
            return@findBlock
        }

        val groundMinX = groundBlockBB.minX
        val groundMinY = groundBlockBB.minY
        val groundMaxY = groundBlockBB.maxY
        val groundMinZ = groundBlockBB.minZ

        // These delta variable has in range 0.0625 ~ 1.0
        val deltaX = groundBlockBB.maxX - groundMinX
        val deltaY = groundMaxY - groundMinY
        val deltaZ = groundBlockBB.maxZ - groundMinZ

        // Search Ranges
        val xzRange = rotationSearchXZRangeValue.get()
        val yRange = rotationSearchYRangeValue.get()

        val xSteps = calcStepSize(xzRange) * deltaX
        val ySteps = calcStepSize(yRange) * deltaY
        val zSteps = calcStepSize(xzRange) * deltaZ

        val sMinX = (0.5 - xzRange * 0.5) * deltaX + groundMinX
        val sMaxX = (0.5 + xzRange * 0.5) * deltaX + groundMinX
        val sMinY = (0.5 - yRange * 0.5) * deltaY + groundMinY
        val sMaxY = (0.5 + yRange * 0.5) * deltaY + groundMinY
        val sMinZ = (0.5 - xzRange * 0.5) * deltaZ + groundMinZ
        val sMaxZ = (0.5 + xzRange * 0.5) * deltaZ + groundMinZ

        val searchBounds = SearchBounds(sMinX, sMaxX, xSteps, sMinY, sMaxY, ySteps, sMinZ, sMaxZ, zSteps)

        lastSearchBound = searchBounds

        val state: String
        var clutching = false
        val flagged = !flagTimer.hasTimePassed(500L)
        val searchPosition: BlockPos

        if (flagged) launchY = thePlayer.posY.toInt()

        if (fallStartY - thePlayer.posY > 2)
        {
            searchPosition = BlockPos(thePlayer).down(2)
            state = "\u00A7cCLUTCH"
            clutching = true
            launchY = thePlayer.posY.toInt()
        }
        else
        {
            var pos = BlockPos(thePlayer)

            var sameY = false
            if (sameYValue.get() && launchY != -999)
            {
                pos = BlockPos(thePlayer.posX, launchY - 1.0, thePlayer.posZ).up()
                sameY = true
            }

            if (!sameY && abCollisionBB.maxY - abCollisionBB.minY < 1.0 && groundMaxY < 1.0 && abCollisionBB.maxY - abCollisionBB.minY < groundMaxY - groundMinY)
            {
                searchPosition = pos

                // Failsafe for slab: Limits maxY to 0.5 to place slab safely.
                if (searchBounds.maxY >= 0.5)
                {
                    searchBounds.minY = 0.125 - yRange * 0.25
                    searchBounds.maxY = 0.125 + yRange * 0.25
                }

                state = "\u00A76NON-FULLBLOCK-SLAB-FAILSAFE"
            }
            else if (!sameY && abCollisionBB.maxY - abCollisionBB.minY < 1.0 && groundMaxY < 1.0 && abCollisionBB.maxY - abCollisionBB.minY == groundMaxY - groundMinY)
            {
                searchPosition = pos
                state = "\u00A7eNON-FULLBLOCK"
            }
            else if (shouldGoDown)
            {
                searchPosition = pos.add(0.0, -0.6, 0.0).down() // Default full-block only scaffold
                state = "\u00A7bDOWNWARDS"
            }
            else
            {
                searchPosition = pos.down() // Default full-block only scaffold
                state = "\u00A7aFULLBLOCK"
            }
        }

        setFindBlockDebug(listOf("result".equalTo("SUCCESS", "\u00A7a"), "state" equalTo "$state${if (flagged) " \u00A78+ \u00A7cFLAGGED\u00A7r" else ""}", "searchRange".equalTo(searchBounds, "\u00A7b"), "autoBlockCollisionBox".equalTo("$abCollisionBB", "\u00A73"), "groundBlockCollisionBB".equalTo("$groundBlockBB", "\u00A72"))::serialize)

        lastSearchPosition = searchPosition

        if (!expand && (!theWorld.isReplaceable(searchPosition) || search(theWorld, thePlayer, searchPosition, rotationSearchCheckVisibleValue.get() && !shouldGoDown, searchBounds, "\u00A7aDefault"))) return

        val ySearch = rotationSearchYSearchValue.get() || clutching || flagged
        if (expand)
        {
            val hFacing = RotationUtils.getHorizontalFacing(thePlayer.moveDirectionDegrees)

            repeat(expandLengthValue.get()) { i ->
                if (search(theWorld, thePlayer, searchPosition.add(when (hFacing)
                    {
                        EnumFacing.WEST -> -i
                        EnumFacing.EAST -> i
                        else -> 0
                    }, 0, when (hFacing)
                    {
                        EnumFacing.NORTH -> -i
                        EnumFacing.SOUTH -> i
                        else -> 0
                    }), false, searchBounds, "\u00A7cExpand")) return@findBlock
            }
        }
        else if (rotationSearchSearchValue.get())
        {
            val urgent = clutching || flagged
            val rangeValue = if (urgent) max(2, rotationSearchSearchRangeValue.get()) else rotationSearchSearchRangeValue.get()
            val yrangeValue = if (ySearch) if (urgent) max(2, rotationSearchYSearchRangeValue.get()) else rotationSearchYSearchRangeValue.get() else 0

            // Cache the ranges
            val listPair = searchRanges.computeIfAbsent(rangeValue to yrangeValue) { (range, yrange) ->
                // Closest first
                val xzList = mutableListOf<Pair<Int, Int>>()
                var i = 0
                while (i <= range)
                {
                    (-i..i).forEach { x -> (-i..i).forEach { z -> if (x to z !in xzList) xzList.add(x to z) } }
                    i++
                }

                val yList = mutableListOf<Int>()
                var j = 0
                while (j <= yrange)
                {
                    (-j..0).forEach { y -> if (y !in yList) yList.add(y) }
                    j++
                }

                xzList to yList
            }

            val yList = listPair.second
            listPair.first.forEach { (x, z) ->
                if (yList.any { y -> search(theWorld, thePlayer, searchPosition.add(x, y, z), !shouldGoDown, searchBounds, "\u00A7bSearch") }) return@findBlock
            }
        }
    }

    private fun place(theWorld: WorldClient, thePlayer: EntityPlayerSP)
    {
        val killAura = LiquidBounce.moduleManager[KillAura::class.java] as KillAura

        val placeableDelay = { if (delayPlaceableDelayValue.get()) delayTimer.reset() }

        if (targetPlace == null)
        {
            placeableDelay()
            return
        }

        val failedResponce = { reason: String ->
            setPlaceDebug(listOf("result".equalTo("FAILED", "\u00A74"), "reason" equalTo reason.withParentheses("\u00A74"))::serialize)
        }

        if (BLACKLISTED_BLOCKS.contains(theWorld.getBlock((targetPlace ?: return).blockPos)))
        {
            placeableDelay()
            failedResponce("Blacklisted block in targetPlace.blockPos")
            return
        }
        if (killauraBypassModeValue.get().equals("WaitForKillAuraEnd", true) && killAura.hasTarget)
        {
            placeableDelay()
            setPlaceDebug(listOf("result".equalTo("WAITING", "\u00A7e"), "reason" equalTo "Waiting for KillAura end".withParentheses("\u00A74"))::serialize)
            return
        }

        val targetPlace = targetPlace ?: return

        val autoBlockMode = autoBlockModeValue.get().toLowerCase()
        val switchKeepTime = autoBlockSwitchKeepTimeValue.get()

        // Delay & SameY check
        if (!delayTimer.hasTimePassed(delay) || sameYValue.get() && launchY - 1 != targetPlace.vec3.yCoord.toInt() && fallStartY - thePlayer.posY <= 2) return

        if (killauraBypassModeValue.get().equals("SuspendKillAura", true)) killAura.suspend(killAuraBypassKillAuraSuspendDurationValue.get().toLong())

        val controller = mc.playerController
        val netHandler = mc.netHandler
        val inventory = thePlayer.inventory

        (LiquidBounce.moduleManager[AutoUse::class.java] as AutoUse).endEating(thePlayer)

        // Check if the player is holding block
        val slot = InventoryUtils.targetSlot ?: inventory.currentItem
        var itemStack = inventory.mainInventory[slot]
        var switched = false

        if (itemStack == null || itemStack.item !is ItemBlock || !(itemStack.item as ItemBlock).block.canAutoBlock || itemStack.stackSize <= 0)
        {
            if (autoBlockMode == "off")
            {
                failedResponce("(Hand) Insufficient block")
                return
            }

            // Auto-Block
            val blockSlot = thePlayer.inventoryContainer.findAutoBlockBlock(theWorld, autoBlockFullCubeOnlyValue.get(), lastSearchPosition?.let(theWorld::getBlockState)?.let { state -> theWorld.getBlockCollisionBox(state)?.maxY } ?: 0.0) // Default boundingBoxYLimit it 0.0

            // If there is no autoblock-able blocks in your inventory, we can't continue.
            if (blockSlot == -1)
            {
                failedResponce("(AutoBlock) Insufficient block")
                return
            }

            switched = slot + 36 != blockSlot

            when (autoBlockMode)
            {
                "pick" ->
                {
                    inventory.currentItem = blockSlot - 36
                    controller.updateController()
                }

                "spoof", "switch" -> if (blockSlot - 36 != slot)
                {
                    if (!InventoryUtils.tryHoldSlot(thePlayer, blockSlot - 36, if (autoBlockMode == "spoof") -1 else switchKeepTime, false)) return
                }
                else InventoryUtils.resetSlot(thePlayer)
            }

            itemStack = thePlayer.inventoryContainer.getSlot(blockSlot).stack
        }

        // Switch Delay reset
        if (switched)
        {
            switchTimer.reset()
            switchDelay = delaySwitchValue.getRandomLong()
            if (switchDelay > 0)
            {
                failedResponce("SwitchDelay reset")
                return
            }
        }

        // Switch Delay wait
        if (!switchTimer.hasTimePassed(switchDelay))
        {
            failedResponce("SwitchDelay")
            return
        }

        // CPSCounter support
        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

        if (thePlayer.isUsingItem && stopConsumingBeforePlaceValue.get()) mc.playerController.onStoppedUsingItem(thePlayer)

        // Place block
        val pos = targetPlace.blockPos
        if (controller.onPlayerRightClick(thePlayer, theWorld, itemStack, pos, targetPlace.enumFacing, targetPlace.vec3))
        {
            setPlaceDebug { targetSearchInfo?.let { searchInfo -> listOf("searchType".equalTo(searchInfo.type), "position".equalTo(pos, "\u00A7a"), "side".equalTo(targetPlace.enumFacing, "\u00A7b"), "hitVec".equalTo(targetPlace.vec3.plus(-pos.x.toDouble(), -pos.y.toDouble(), -pos.z.toDouble()), "\u00A7e"), "delta".equalTo(DECIMALFORMAT_6.format(searchInfo.delta))).serialize() } }

            // Reset delay
            delayTimer.reset()
            delay = delayValue.getRandomLong()

            // Apply SpeedModifier
            if (thePlayer.onGround)
            {
                val modifier: Float = speedModifierValue.get()
                thePlayer.motionX = thePlayer.motionX * modifier
                thePlayer.motionZ = thePlayer.motionZ * modifier
            }

            if (swingValue.get()) thePlayer.swingItem()
            else netHandler.addToSendQueue(C0APacketAnimation())
        }
        else setPlaceDebug { "result".equalTo("FAILED_TO_PLACE", "\u00A74") }

        // Switch back to original slot after place on AutoBlock-Switch mode
        if (autoBlockMode == "switch" && switchKeepTime < 0) InventoryUtils.resetSlot(thePlayer)

        this.targetPlace = null
    }

    // DISABLING MODULE
    override fun onDisable()
    {
        val thePlayer = mc.thePlayer ?: return
        val gameSettings = mc.gameSettings
        val netHandler = mc.netHandler

        // Reset Seaking (Eagle)
        if (!GameSettings.isKeyDown(gameSettings.keyBindSneak))
        {
            gameSettings.keyBindSneak.pressed = false
            if (eagleSneaking) netHandler.addToSendQueue(C0BPacketEntityAction(thePlayer, C0BPacketEntityAction.Action.STOP_SNEAKING))
        }

        if (!GameSettings.isKeyDown(gameSettings.keyBindRight)) gameSettings.keyBindRight.pressed = false
        if (!GameSettings.isKeyDown(gameSettings.keyBindLeft)) gameSettings.keyBindLeft.pressed = false

        // Reset rotations
        lockRotation = null
        limitedRotation = null

        facesBlock = false
        mc.timer.timerSpeed = 1f
        shouldGoDown = false

        targetSearchInfo = null

        InventoryUtils.resetSlot(thePlayer)
    }

    @EventTarget
    fun onMove(event: MoveEvent)
    {
        if (!movementSafeWalkValue.get() || shouldGoDown) return
        if (movementAirSafeValue.get() || (mc.thePlayer ?: return).onGround) event.isSafeWalk = true
    }

    /**
     * Scaffold visuals
     */
    @EventTarget
    fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
    {
        val counter = visualCounterEnabledValue.get()
        if (!counter/* && !debug*/) return

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val blockOverlay = LiquidBounce.moduleManager[BlockOverlay::class.java] as BlockOverlay

        val scaledResolution = ScaledResolution(mc)

        val middleScreenX = scaledResolution.scaledWidth shr 1
        val middleScreenY = scaledResolution.scaledHeight shr 1

        if (counter)
        {
            GL11.glPushMatrix()

            val blocksAmount = getBlocksAmount(thePlayer)
            val info = "Blocks: \u00A7${if (blocksAmount <= 16) "c" else if (blocksAmount <= 64) "e" else "7"}$blocksAmount${if (downValue.get() && blocksAmount == 1) " (You need at least 2 blocks to go down)" else ""}"

            val yoffset = if (blockOverlay.state && blockOverlay.infoEnabledValue.get() && blockOverlay.getCurrentBlock(theWorld) != null) 15f else 0f

            val font = visualCounterFontValue.get()
            RenderUtils.drawBorderedRect(middleScreenX - 2f, middleScreenY + yoffset + 5f, middleScreenX + font.getStringWidth(info) + 2f, middleScreenY + yoffset + font.FONT_HEIGHT + 7f, 3f, -16777216, -16777216)

            GlStateManager.resetColor()

            font.drawString(info, middleScreenX.toFloat(), middleScreenY + yoffset + 7f, 0xffffff)
            GL11.glPopMatrix()
        }

        val font = Fonts.minecraftFont

        arrayOf(findBlockDebug?.let { it to "findBlock()" }, placeDebug?.let { it to "place()" }).filterNotNull().forEachIndexed { index, (info, infoType) ->
            val offset = -60f + index * 10f
            val yPos = middleScreenY + offset
            val infoWidth = font.getStringWidth(info) shr 2
            val infoTypeWidth = font.getStringWidth(infoType) shr 1

            GL11.glPushMatrix()

            RenderUtils.drawBorderedRect(middleScreenX - infoWidth - 2f, yPos, middleScreenX + infoWidth + 2f, yPos + font.FONT_HEIGHT * 0.5f, 3f, -16777216, -16777216)

            GlStateManager.resetColor()

            GL11.glScalef(0.5f, 0.5f, 0.5f)
            font.drawString(infoType, (middleScreenX.toFloat() - infoWidth - infoTypeWidth) * 2f, (yPos - font.FONT_HEIGHT * 0.2f) * 2f, 0xffffff, true)
            font.drawString(info, (middleScreenX.toFloat() - infoWidth) * 2f, yPos * 2f, 0xffffff)
            GL11.glScalef(2f, 2f, 2f)

            GL11.glPopMatrix()
        }
    }

    @EventTarget
    fun onRender3D(@Suppress("UNUSED_PARAMETER") event: Render3DEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        if (visualDebugRayEnabledValue.get() && rotationSearchCheckVisibleValue.get() && !rotationModeValue.get().equals("Off", ignoreCase = true)) targetSearchInfo?.let { (_, rayStart, rayEnd, expectedRayEnd, actualRayEnd, _, expectedPlacePos) ->
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
            GL11.glEnable(GL11.GL_BLEND)
            GL11.glEnable(GL11.GL_LINE_SMOOTH)
            GL11.glLineWidth(3.0f)
            GL11.glDisable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(false)

            val renderManager = mc.renderManager
            val renderPosX = renderManager.renderPosX
            val renderPosY = renderManager.renderPosY
            val renderPosZ = renderManager.renderPosZ

            val vertex = { vec: Vec3 -> vec.plus(-renderPosX, -renderPosY, -renderPosZ).let { GL11.glVertex3d(it.xCoord, it.yCoord, it.zCoord) } }
            val drawLine = { start: Vec3, end: Vec3, color: Int ->
                GL11.glBegin(GL11.GL_LINES)
                RenderUtils.glColor(color)
                vertex(start)
                vertex(end)
                GL11.glEnd()
            }

            drawLine(rayStart, rayEnd, visualDebugRayColorReachValue.get()) // Display reach distance
            if (expectedRayEnd != null) drawLine(rayStart, expectedRayEnd, visualDebugRayColorExpectValue.get()) // Display EXPECTED raytrace result (if available)
            drawLine(rayStart, actualRayEnd, visualDebugRayColorActualValue.get()) // Display ACTUAL raytrace result

            GL11.glEnable(GL11.GL_TEXTURE_2D)
            GL11.glDisable(GL11.GL_LINE_SMOOTH)
            GL11.glEnable(GL11.GL_DEPTH_TEST)
            GL11.glDepthMask(true)
            GL11.glDisable(GL11.GL_BLEND)
            RenderUtils.resetColor()

            RenderUtils.drawBlockBox(theWorld, thePlayer, expectedPlacePos, ColorUtils.applyAlphaChannel(Color.red.rgb, 60), 0, false, event.partialTicks)
        }

        if (!visualMarkValue.get()) return

        run searchLoop@{
            repeat(if (modeValue.get().equals("Expand", true)) expandLengthValue.get() + 1 else 2) {
                val horizontalFacing = RotationUtils.getHorizontalFacing(thePlayer.moveDirectionDegrees)
                val blockPos = BlockPos(thePlayer.posX + when (horizontalFacing)
                {
                    EnumFacing.WEST -> -it.toDouble()
                    EnumFacing.EAST -> it.toDouble()
                    else -> 0.0
                }, if (sameYValue.get() && launchY <= thePlayer.posY && fallStartY - thePlayer.posY <= 2) launchY - 1.0 else thePlayer.posY - (if (thePlayer.posY > floor(thePlayer.posY).toInt()) 0.0 else 1.0) - if (shouldGoDown) 1.0 else 0.0, thePlayer.posZ + when (horizontalFacing)
                {
                    EnumFacing.NORTH -> -it.toDouble()
                    EnumFacing.SOUTH -> it.toDouble()
                    else -> 0.0
                })

                val placeInfo = PlaceInfo[theWorld, blockPos]
                if (theWorld.isReplaceable(blockPos) && placeInfo != null)
                {
                    RenderUtils.drawBlockBox(theWorld, thePlayer, blockPos, 1682208255, 0, false, event.partialTicks)
                    return@searchLoop
                }
            }
        }
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @param checkVisible        visible
     * @return
     */

    private fun search(theWorld: World, thePlayer: EntityPlayer, blockPosition: BlockPos, checkVisible: Boolean, data: SearchBounds, type: String): Boolean
    {
        if (!theWorld.isReplaceable(blockPosition)) return false

        // Static Modes
        val staticMode = rotationModeValue.get().equals("Static", ignoreCase = true)
        val staticYaw = staticMode || rotationModeValue.get().equals("StaticYaw", ignoreCase = true)
        val staticPitch = staticMode || rotationModeValue.get().equals("StaticPitch", ignoreCase = true)
        val staticPitchOffset = rotationSearchStaticPitchValue.get()
        val staticYawOffset = rotationSearchStaticYawValue.get()

        var searchFace = Vec3(0.0, 0.0, 0.0)

        val eyesPos = Vec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
        var placeRotation: PlaceRotation? = null

        val searchMinX = data.minX
        val searchMaxX = data.maxX
        val searchMinY = data.minY
        val searchMaxY = data.maxY
        val searchMinZ = data.minZ
        val searchMaxZ = data.maxZ

        val xSteps = data.xSteps
        val ySteps = data.ySteps
        val zSteps = data.zSteps

        val minDelta = rotationSearchMinDiffValue.get()

        EnumFacing.values().forEach { side ->
            val neighbor = blockPosition.offset(side)

            if (!theWorld.canBeClicked(neighbor)) return@forEach

            val dirVec = Vec3(side.directionVec)
            val dirVecHalf = dirVec * 0.5

            var xSearch = searchMinX
            while (xSearch <= searchMaxX)
            {
                var ySearch = searchMinY
                while (ySearch <= searchMaxY)
                {
                    var zSearch = searchMinZ
                    while (zSearch <= searchMaxZ)
                    {
                        val searchVec = Vec3(blockPosition).plus(xSearch, ySearch, zSearch)
                        val sqDistanceToSearchVec = eyesPos.squareDistanceTo(searchVec)
                        val searchSideVec = searchVec + dirVecHalf

                        // Visibility checks
                        if (checkVisible && (eyesPos.distanceTo(searchSideVec) > 4.25 // Distance Check - (distance(eyes, searchSideVec) > 4.25 blocks)
                                || sqDistanceToSearchVec > eyesPos.squareDistanceTo(searchVec + dirVec) // Against Distance Check - (distance(eyes, searchVec) > distance(eyes, searchDirVec))
                                || theWorld.rayTraceBlocks(eyesPos, searchSideVec, false, true, false) != null)) // Raytrace Check - (rayTrace hit between eye position and block side)
                        {
                            zSearch += zSteps // Skip current step
                            continue
                        }

                        // Face block
                        repeat(if (staticYaw) 2 /* Separated X search and Z search */ else 1) { i ->
                            var delta = -1.0
                            val deltaX = if (staticYaw && i == 0) 0.0 else searchSideVec.xCoord - eyesPos.xCoord
                            val deltaZ = if (staticYaw && i == 1) 0.0 else searchSideVec.zCoord - eyesPos.zCoord

                            if (side != UP)
                            {
                                delta = abs(if (side.axis == Axis.Z) deltaZ else deltaX)
                                if (delta < minDelta) return@repeat
                            }

                            // Calculate the rotation from vector
                            val rotation = Rotation(((atan2(deltaZ, deltaX).toFloat()).toDegrees - 90f + if (staticYaw) staticYawOffset else 0f).wrapAngleTo180, if (staticPitch) staticPitchOffset else ((-(atan2(searchSideVec.yCoord - eyesPos.yCoord, hypot(deltaX, deltaZ)).toFloat()).toDegrees)).wrapAngleTo180)
                            val vectorForRotation = RotationUtils.getVectorForRotation(rotation)
                            val rayEnd = eyesPos + vectorForRotation * 4.25

                            var rayTrace: MovingObjectPosition? = null
                            if (!checkVisible || run {
                                    rayTrace = theWorld.rayTraceBlocks(eyesPos, rayEnd, false, false, true)
                                    rayTrace?.let { it.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || it.blockPos != neighbor } == true // Raytrace Check - rayTrace hit between eye position and block reach position
                                }) return@repeat

                            if (placeRotation?.let { RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(it.rotation) } != false) // If the current rotation is better than the previous one
                            {
                                val expectedHitVec = if (staticYaw || staticPitch) null else searchSideVec
                                placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, rayEnd /* FIXME: 여기 rayEnd가 왜 들어가있음? actualHitVec가 들어가야 되는 곳 아님? */), rotation) { SearchInfo(type, eyesPos, rayEnd, expectedHitVec, rayTrace?.hitVec ?: expectedHitVec ?: rayEnd, delta, neighbor.offset(side.opposite)) }
                                searchFace = Vec3(xSearch, ySearch, zSearch)
                            }
                        }

                        zSearch += zSteps
                    }
                    ySearch += ySteps
                }
                xSearch += xSteps
            }
        }

        placeRotation ?: return false

        // Rotate
        if (!rotationModeValue.get().equals("Off", ignoreCase = true) && rotationTurnSpeedValue.getMax() > 0)
        {
            val targetRotation = placeRotation!!.rotation

            val keepRotationTicks = if (rotationKeepRotationEnabledValue.get()) rotationKeepRotationTicksValue.getRandom() else 0

            if (rotationTurnSpeedValue.getMin() < 180)
            {
                // Limit rotation speed
                val limitedRotation = RotationUtils.limitAngleChange(RotationUtils.serverRotation, targetRotation, rotationTurnSpeedValue.getRandomStrict(), 0f).also { limitedRotation = it }
                setRotation(thePlayer, limitedRotation, keepRotationTicks)

                lockRotation = limitedRotation
                facesBlock = false

                // Check player is faced the target block
                run searchLoop@{
                    EnumFacing.values().forEach sideLoop@{ side ->
                        val neighbor = blockPosition.offset(side)

                        if (!theWorld.canBeClicked(neighbor)) return@sideLoop

                        val dirVec = Vec3(side.directionVec)
                        val posVec = Vec3(blockPosition) + searchFace
                        val sqDistanceToPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec + dirVec * 0.5

                        // Visibility checks
                        if (checkVisible && (eyesPos.squareDistanceTo(hitVec) > 18.0 || sqDistanceToPosVec > eyesPos.squareDistanceTo(posVec + dirVec) || theWorld.rayTraceBlocks(eyesPos, hitVec, false, true, false) != null)) return@sideLoop

                        val rotationVector = RotationUtils.getVectorForRotation(limitedRotation)
                        val rayEndPos = eyesPos + rotationVector * 4.0
                        val rayTrace = theWorld.rayTraceBlocks(eyesPos, rayEndPos, false, false, true)

                        if (rayTrace != null && (rayTrace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK || rayTrace.blockPos != neighbor)) return@sideLoop

                        facesBlock = true

                        return@searchLoop
                    }
                }
            }
            else
            {
                // Rotate instantly
                setRotation(thePlayer, targetRotation, keepRotationTicks)

                lockRotation = targetRotation
                facesBlock = true
            }

            (LiquidBounce.moduleManager[Tower::class.java] as Tower).lockRotation = null // Prevents conflict
        }

        targetPlace = placeRotation!!.placeInfo
        targetSearchInfo = placeRotation!!.searchInfo?.invoke()
        return true
    }

    private fun calcStepSize(range: Float): Double
    {
        var accuracy = rotationSearchStepsValue.get().toDouble()
        accuracy += accuracy % 2 // If it is set to uneven it changes it to even. Fixes a bug
        return (range / accuracy).coerceAtLeast(0.01)
    }

    /**
     * @return Amount of blocks in hotbar
     */
    private fun getBlocksAmount(thePlayer: EntityPlayer): Int
    {
        val inventory = thePlayer.inventory
        val heldItem = thePlayer.heldItem

        return (0..8).asSequence().map(inventory::getStackInSlot).filter { it?.item is ItemBlock }.mapNotNull { (it ?: return@mapNotNull null) to (it.item ?: return@mapNotNull null) as ItemBlock }.filter { (stack, itemBlock) -> heldItem == stack || itemBlock.block.canAutoBlock }.sumBy { it.first.stackSize }
    }

    override val tag: String?
        get()
        {
            val thePlayer = mc.thePlayer ?: return null
            return "${modeValue.get()}${if (fallStartY - thePlayer.posY > 2) " CLUTCH" else if (sameYValue.get()) " SameY=${launchY - 1.0}" else ""}"
        }

    class SearchBounds(x: Double, x2: Double, xsteps: Double, y: Double, y2: Double, ysteps: Double, z: Double, z2: Double, zsteps: Double)
    {
        val minX = x.coerceAtMost(x2)
        val maxX = x.coerceAtLeast(x2)
        var minY = y.coerceAtMost(y2)
        var maxY = y.coerceAtLeast(y2)
        val minZ = z.coerceAtMost(z2)
        val maxZ = z.coerceAtLeast(z2)

        val xSteps = xsteps
        val ySteps = ysteps
        val zSteps = zsteps

        override fun toString(): String = "SearchBounds[X: %.2f~%.2f (%.3f), Y: %.2f~%.2f (%.3f), Z: %.2f~%.2f (%.3f)]".format(minX, maxX, xSteps, minY, maxY, ySteps, minZ, maxZ, zSteps)
    }
}
