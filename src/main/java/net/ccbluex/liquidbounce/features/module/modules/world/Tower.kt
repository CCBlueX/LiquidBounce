/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.enums.EnumFacingType
import net.ccbluex.liquidbounce.api.enums.StatType
import net.ccbluex.liquidbounce.api.minecraft.client.entity.Entity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.EntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.EntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.WorldClient
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.BlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WMathHelper
import net.ccbluex.liquidbounce.api.minecraft.util.Vec3
import net.ccbluex.liquidbounce.api.minecraft.world.World
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.combat.AutoUse
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.render.BlockOverlay
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.block.PlaceInfo
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.timer.TickTimer
import net.ccbluex.liquidbounce.value.*
import org.lwjgl.input.Keyboard
import org.lwjgl.opengl.GL11
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.truncate

@ModuleInfo(name = "Tower", description = "Automatically builds a tower beneath you.", category = ModuleCategory.WORLD, defaultKeyBinds = [Keyboard.KEY_O])
class Tower : Module()
{
    /**
     * OPTIONS
     */
    private val modeValue = ListValue("Mode", arrayOf("Jump", "Motion", "ConstantMotion", "MotionTP", "Packet", "Teleport", "AAC3.3.9", "AAC3.6.4", "AAC4.4-Constant", "AAC4-Jump"), "Motion")

    private val autoBlockGroup = ValueGroup("AutoBlock")
    private val autoBlockModeValue = ListValue("Mode", arrayOf("Off", "Pick", "Spoof", "Switch"), "Spoof", "AutoBlock")
    private val autoBlockSwitchKeepTimeValue = object : IntegerValue("SwitchKeepTime", -1, -1, 10, "AutoBlockSwitchKeepTime")
    {
        override fun showCondition() = !autoBlockModeValue.get().equals("None", ignoreCase = true)
    }
    private val autoBlockFullCubeOnlyValue = object : BoolValue("FullCubeOnly", false, "AutoBlockFullCubeOnly")
    {
        override fun showCondition() = !autoBlockModeValue.get().equals("None", ignoreCase = true)
    }

    private val swingValue = BoolValue("Swing", true)
    private val stopWhenBlockAbove = BoolValue("StopWhenBlockAbove", false)

    // Rotation
    private val rotationGroup = ValueGroup("Rotation")
    private val rotationEnabledValue = BoolValue("Enabled", true, "Rotations")
    private val rotationResetSpeedValue = FloatRangeValue("RotationResetSpeed", 180f, 180f, 10f, 180f, "MaxRotationResetSpeed" to "MinRotationResetSpeed")

    private val rotationKeepRotationGroup = ValueGroup("KeepRotation")
    private val rotationKeepRotationEnabledValue = BoolValue("Enabled", false, "KeepRotation")
    private val rotationKeepRotationLockValue = BoolValue("Lock", false, "LockRotation")
    private val rotationKeepRotationTicksValue = object : IntegerRangeValue("Ticks", 20, 30, 0, 60, "MinKeepRotationTicks" to "MaxKeepRotationTicks")
    {
        override fun showCondition() = !rotationKeepRotationLockValue.get()
    }

    // OnJump
    private val onJumpGroup = ValueGroup("OnJump")
    private val onJumpValue = BoolValue("Enabled", false, "OnJump")
    private val onJumpDelayValue = object : IntegerValue("Delay", 500, 0, 1000, "OnJumpDelay")
    {
        override fun showCondition() = !(onJumpNoDelayIfNotMovingValue.get() && onJumpDisableWhileMoving.get())
    }
    private val onJumpNoDelayIfNotMovingValue = BoolValue("NoDelayIfNotMoving", true, "OnJumpNoDelayIfNotMoving")
    private val onJumpDisableWhileMoving: BoolValue = BoolValue("DisableWhileMoving", true, "DisableOnJumpWhileMoving")

    private val placeModeValue = ListValue("PlaceTiming", arrayOf("Pre", "Post"), "Post")

    private val timerValue = FloatValue("Timer", 1f, 0.01f, 10f)

    private val jumpGroup = object : ValueGroup("Jump")
    {
        override fun showCondition() = modeValue.get().equals("Jump", ignoreCase = true)
    }
    private val jumpMotionValue = FloatValue("Motion", 0.42f, 0.3681289f, 0.79f, "JumpMotion")
    private val jumpDelayValue = IntegerValue("Delay", 0, 0, 20, "JumpDelay")

    private val constantMotionGroup = object : ValueGroup("ConstantMotion")
    {
        override fun showCondition() = modeValue.get().equals("ConstantMotion", ignoreCase = true)
    }
    private val constantMotionMotionValue = FloatValue("Motion", 0.42f, 0.1f, 1f, "ConstantMotion")
    private val constantMotionJumpGroundValue = FloatValue("JumpGround", 0.79f, 0.76f, 1f, "ConstantMotionJumpGround")

    private val teleportGroup = object : ValueGroup("Teleport")
    {
        override fun showCondition() = modeValue.get().equals("Teleport", ignoreCase = true)
    }
    private val teleportHeightValue = FloatValue("Height", 1.15f, 0.1f, 5f, "TeleportHeight")
    private val teleportDelayValue = IntegerValue("Delay", 0, 0, 20, "TeleportDelay")
    private val teleportGroundValue = BoolValue("Ground", true, "TeleportGround")
    private val teleportNoMotionValue = BoolValue("NoMotion", false, "TeleportNoMotion")

    // KillAura bypass (Other settings are same as scaffold's)
    private val suspendKillAuraDuration = IntegerValue("SuspendKillAuraDuration", 500, 250, 1000)

    private val stopConsumingBeforePlaceValue = BoolValue("StopConsumingBeforePlace", true)

    private val counterGroup = ValueGroup("Counter")
    val counterEnabledValue = BoolValue("Enabled", true, "Counter")
    private val counterFontValue = FontValue("Font", Fonts.font40)

    init
    {
        autoBlockGroup.addAll(autoBlockModeValue, autoBlockSwitchKeepTimeValue, autoBlockFullCubeOnlyValue)
        rotationKeepRotationGroup.addAll(rotationKeepRotationEnabledValue, rotationKeepRotationLockValue, rotationKeepRotationTicksValue)
        rotationGroup.addAll(rotationEnabledValue, rotationResetSpeedValue, rotationKeepRotationGroup)

        onJumpGroup.addAll(onJumpValue, onJumpDelayValue, onJumpNoDelayIfNotMovingValue, onJumpDisableWhileMoving)
        jumpGroup.addAll(jumpMotionValue, jumpDelayValue)
        constantMotionGroup.addAll(constantMotionMotionValue, constantMotionJumpGroundValue)
        teleportGroup.addAll(teleportHeightValue, teleportDelayValue, teleportGroundValue, teleportNoMotionValue)

        counterGroup.addAll(counterEnabledValue, counterFontValue)
    }

    private val noCustomTimer = arrayOf("aac3.3.9", "aac4.4-constant", "aac4-jump")

    /**
     * MODULE
     */

    // Target block
    private var placeInfo: PlaceInfo? = null

    // Rotation lock
    var lockRotation: Rotation? = null

    // Mode stuff
    private val delayTimer = TickTimer()
    private val onJumpTimer = MSTimer()
    private var jumpGround = 0.0

    override fun onDisable()
    {
        val thePlayer = mc.thePlayer ?: return

        mc.timer.timerSpeed = 1.0F
        lockRotation = null
        active = false

        // Restore to original slot
        if (InventoryUtils.targetSlot != thePlayer.inventory.currentItem) InventoryUtils.resetSlot(thePlayer)
    }

    @EventTarget
    fun onMotion(event: MotionEvent)
    {
        // Lock Rotation
        if (rotationEnabledValue.get() && rotationKeepRotationEnabledValue.get() && rotationKeepRotationLockValue.get() && lockRotation != null) RotationUtils.setTargetRotation(lockRotation)

        active = false

        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return
        val timer = mc.timer

        // OnJump
        if (onJumpValue.get() && !mc.gameSettings.keyBindJump.isKeyDown)
        {
            // Skip if jump key isn't pressed
            if (onJumpDelayValue.get() > 0) onJumpTimer.reset()

            return
        }
        else if (onJumpValue.get() && onJumpDelayValue.get() > 0 && (!onJumpTimer.hasTimePassed(onJumpDelayValue.get().toLong()) || onJumpDisableWhileMoving.get()) && (thePlayer.isMoving || !onJumpNoDelayIfNotMovingValue.get())) // Skip if onjump delay aren't over yet.
            return

        active = true

        if (modeValue.get().toLowerCase() !in noCustomTimer) timer.timerSpeed = timerValue.get()

        val eventState = event.eventState

        // Place
        if (placeModeValue.get().equals(eventState.stateName, ignoreCase = true)) place(theWorld, thePlayer)

        // Update
        if (eventState == EventState.PRE)
        {
            placeInfo = null
            delayTimer.update()

            val provider = classProvider

            val heldItem = thePlayer.heldItem

            if (if (!autoBlockModeValue.get().equals("Off", ignoreCase = true)) thePlayer.inventoryContainer.findAutoBlockBlock(theWorld, autoBlockFullCubeOnlyValue.get()) != -1 || heldItem != null && heldItem.item is ItemBlock else heldItem != null && heldItem.item is ItemBlock)
            {
                if (!stopWhenBlockAbove.get() || provider.isBlockAir(theWorld.getBlock(BlockPos(thePlayer.posX, thePlayer.posY + 2, thePlayer.posZ)))) move()

                val blockPos = BlockPos(thePlayer.posX, thePlayer.posY - 1.0, thePlayer.posZ)
                if (provider.isBlockAir(theWorld.getBlockState(blockPos).block) && search(theWorld, thePlayer, blockPos) && rotationEnabledValue.get())
                {
                    val vecRotation = RotationUtils.faceBlock(theWorld, thePlayer, blockPos)
                    if (vecRotation != null)
                    {
                        RotationUtils.setTargetRotation(vecRotation.rotation, keepRotationTicks)
                        RotationUtils.setNextResetTurnSpeed(rotationResetSpeedValue.getMin().coerceAtLeast(10F), rotationResetSpeedValue.getMax().coerceAtLeast(10F))

                        placeInfo!!.vec3 = vecRotation.vec // Is this redundant?
                    }
                }
            }
        }
    }

    /**
     * Send jump packets, bypasses stat-based cheat detections like Hypixel watchdog.
     */
    private fun fakeJump()
    {
        val thePlayer = mc.thePlayer ?: return

        thePlayer.isAirBorne = true
        thePlayer.triggerAchievement(classProvider.getStatEnum(StatType.JUMP_STAT))
    }

    /**
     * Move player
     */
    private fun move()
    {
        val thePlayer = mc.thePlayer ?: return

        val posX = thePlayer.posX
        val posY = thePlayer.posY
        val posZ = thePlayer.posZ
        val onGround = thePlayer.onGround
        val timer = mc.timer

        when (modeValue.get().toLowerCase())
        {
            "jump" -> if (onGround && delayTimer.hasTimePassed(jumpDelayValue.get()))
            {
                fakeJump()
                thePlayer.motionY = jumpMotionValue.get().toDouble()
                delayTimer.reset()
            }

            "motion" -> if (onGround)
            {
                fakeJump()
                thePlayer.motionY = 0.42
            }
            else if (thePlayer.motionY < 0.1) thePlayer.motionY = -0.3

            "motiontp" -> if (onGround)
            {
                fakeJump()
                thePlayer.motionY = 0.42
            }
            else if (thePlayer.motionY < 0.23) thePlayer.setPosition(posX, truncate(posY), posZ)

            "packet" -> if (onGround && delayTimer.hasTimePassed(2))
            {
                val netHandler = mc.netHandler

                fakeJump()

                val provider = classProvider

                netHandler.addToSendQueue(CPacketPlayerPosition(posX, posY + 0.42, posZ, false))
                netHandler.addToSendQueue(CPacketPlayerPosition(posX, posY + 0.753, posZ, false))
                thePlayer.setPosition(posX, posY + 1.0, posZ)

                delayTimer.reset()
            }

            "teleport" ->
            {
                if (teleportNoMotionValue.get()) thePlayer.motionY = 0.0

                if ((onGround || !teleportGroundValue.get()) && delayTimer.hasTimePassed(teleportDelayValue.get()))
                {
                    fakeJump()
                    thePlayer.setPositionAndUpdate(posX, posY + teleportHeightValue.get(), posZ)
                    delayTimer.reset()
                }
            }

            "constantmotion" ->
            {
                val constantMotion = constantMotionMotionValue.get().toDouble()

                if (onGround)
                {
                    fakeJump()
                    jumpGround = posY
                    thePlayer.motionY = constantMotion
                }

                if (posY > jumpGround + constantMotionJumpGroundValue.get())
                {
                    fakeJump()
                    thePlayer.setPosition(posX, truncate(posY), posZ) // TODO: toInt() required?
                    thePlayer.motionY = constantMotion
                    jumpGround = posY
                }
            }

            "aac3.3.9" ->
            {
                if (onGround)
                {
                    fakeJump()
                    thePlayer.motionY = 0.4001
                }

                timer.timerSpeed = 1f

                if (thePlayer.motionY < 0.0)
                {
                    // Fast down
                    thePlayer.motionY -= 0.00000945
                    timer.timerSpeed = 1.6f
                }
            }

            "aac3.6.4" -> when (thePlayer.ticksExisted % 4)
            {
                0 ->
                {
                    thePlayer.motionY = -0.5
                    thePlayer.setPosition(posX + 0.035, posY, posZ)
                }

                1 ->
                {
                    thePlayer.motionY = 0.4195464
                    thePlayer.setPosition(posX - 0.035, posY, posZ)
                }
            }

            "aac4.4-constant" ->
            {
                if (thePlayer.onGround)
                {
                    fakeJump()
                    jumpGround = thePlayer.posY
                    thePlayer.motionY = 0.42
                }

                thePlayer.motionX = 0.0
                thePlayer.motionZ = -0.00000001
                thePlayer.jumpMovementFactor = 0.000F
                timer.timerSpeed = 0.60f

                if (thePlayer.posY > jumpGround + 0.99)
                {
                    fakeJump()
                    thePlayer.setPosition(thePlayer.posX, thePlayer.posY - 0.001335979112146, thePlayer.posZ)
                    thePlayer.motionY = 0.42
                    jumpGround = thePlayer.posY
                    timer.timerSpeed = 0.75f
                }
            }

            "aac4-jump" ->
            {
                timer.timerSpeed = 0.97f

                if (thePlayer.onGround)
                {
                    fakeJump()
                    thePlayer.motionY = 0.387565
                    timer.timerSpeed = 1.05f
                }
            }
        }
    }

    /**
     * Place target block
     */
    private fun place(theWorld: WorldClient, thePlayer: EntityPlayerSP)
    {
        val placeInfo = placeInfo ?: return

        val moduleManager = LiquidBounce.moduleManager

        val killAura = moduleManager[KillAura::class.java] as KillAura
        val scaffold = moduleManager[Scaffold::class.java] as Scaffold

        if (scaffold.killauraBypassModeValue.get().equals("SuspendKillAura", true)) killAura.suspend(suspendKillAuraDuration.get().toLong())

        val netHandler = mc.netHandler
        val controller = mc.playerController
        val inventory = thePlayer.inventory

        val provider = classProvider

        (LiquidBounce.moduleManager[AutoUse::class.java] as AutoUse).endEating(thePlayer, classProvider, netHandler)

        // AutoBlock
        val slot = InventoryUtils.targetSlot ?: inventory.currentItem
        var itemStack = inventory.mainInventory[slot]

        val switchKeepTime = autoBlockSwitchKeepTimeValue.get()

        if (itemStack == null || itemStack.item !is ItemBlock || provider.isBlockBush(itemStack.item? as ItemBlock?.block))
        {
            if (autoBlockModeValue.get().equals("Off", true)) return

            val blockSlot = thePlayer.inventoryContainer.findAutoBlockBlock(theWorld, autoBlockFullCubeOnlyValue.get())
            if (blockSlot == -1) return

            when (val autoBlockMode = autoBlockModeValue.get().toLowerCase())
            {
                "pick" ->
                {
                    inventory.currentItem = blockSlot - 36
                    controller.updateController()
                }

                "spoof", "switch" -> if (blockSlot - 36 != slot)
                {
                    if (!InventoryUtils.tryHoldSlot(thePlayer, blockSlot - 36, if (autoBlockMode.equals("spoof", ignoreCase = true)) -1 else switchKeepTime, false)) return
                }
                else InventoryUtils.resetSlot(thePlayer)
            }

            itemStack = thePlayer.inventoryContainer.getSlot(blockSlot).stack
        }

        // CPSCounter support
        CPSCounter.registerClick(CPSCounter.MouseButton.RIGHT)

        if (thePlayer.isUsingItem && stopConsumingBeforePlaceValue.get()) mc.playerController.onStoppedUsingItem(thePlayer)

        // Place block
        if (controller.onPlayerRightClick(thePlayer, theWorld, itemStack, placeInfo.blockPos, placeInfo.enumFacing, placeInfo.vec3)) if (swingValue.get()) thePlayer.swingItem() else netHandler.addToSendQueue(CPacketAnimation())

        // Switch back to original slot after place on AutoBlock-Switch mode
        if (autoBlockModeValue.get().equals("Switch", true) && switchKeepTime < 0) InventoryUtils.resetSlot(thePlayer)

        this.placeInfo = null
    }

    /**
     * Search for placeable block
     *
     * @param blockPosition pos
     * @return
     */
    private fun search(theWorld: World, thePlayer: Entity, blockPosition: BlockPos): Boolean
    {
        if (blockPosition !is Replaceable) return false

        val eyesPos = Vec3(thePlayer.posX, thePlayer.entityBoundingBox.minY + thePlayer.eyeHeight, thePlayer.posZ)
        var placeRotation: PlaceRotation? = null

        val provider = classProvider

        EnumFacingType.values().map(provider::getEnumFacing).forEach { side ->
            val neighbor = blockPosition.offset(side)

            if (!theWorld.canBeClicked(neighbor)) return@forEach

            val dirVec = Vec3(side.directionVec)

            var xSearch = 0.1
            while (xSearch < 0.9)
            {
                var ySearch = 0.1
                while (ySearch < 0.9)
                {
                    var zSearch = 0.1
                    while (zSearch < 0.9)
                    {
                        val posVec = Vec3(blockPosition).plus(xSearch, ySearch, zSearch)
                        val distanceSqPosVec = eyesPos.squareDistanceTo(posVec)
                        val hitVec = posVec + Vec3(dirVec.xCoord, dirVec.yCoord, dirVec.zCoord) * 0.5
                        if (eyesPos.squareDistanceTo(hitVec) > 18.0 || distanceSqPosVec > eyesPos.squareDistanceTo(posVec + dirVec) || theWorld.rayTraceBlocks(eyesPos, hitVec, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = true, returnLastUncollidableBlock = false) != null)
                        {
                            zSearch += 0.1
                            continue
                        }

                        // face block
                        val diffX = hitVec.xCoord - eyesPos.xCoord
                        val diffY = hitVec.yCoord - eyesPos.yCoord
                        val diffZ = hitVec.zCoord - eyesPos.zCoord
                        val diffXZ = hypot(diffX, diffZ)

                        val rotation = Rotation(WMathHelper.wrapAngleTo180_float(WMathHelper.toDegrees(atan2(diffZ, diffX).toFloat()) - 90f), WMathHelper.wrapAngleTo180_float((-WMathHelper.toDegrees(atan2(diffY, diffXZ).toFloat()))))
                        val rotationVector = RotationUtils.getVectorForRotation(rotation)
                        val vector = eyesPos + rotationVector * 4.0
                        val rayTrace = theWorld.rayTraceBlocks(eyesPos, vector, stopOnLiquid = false, ignoreBlockWithoutBoundingBox = false, returnLastUncollidableBlock = true)

                        if (rayTrace != null && (rayTrace.typeOfHit != IMovingObjectPosition.WMovingObjectType.BLOCK || rayTrace.blockPos != neighbor))
                        {
                            zSearch += 0.1
                            continue
                        }

                        if (placeRotation == null || RotationUtils.getRotationDifference(rotation) < RotationUtils.getRotationDifference(placeRotation!!.rotation)) placeRotation = PlaceRotation(PlaceInfo(neighbor, side.opposite, hitVec), rotation)

                        zSearch += 0.1
                    }
                    ySearch += 0.1
                }
                xSearch += 0.1
            }
        }

        if (placeRotation == null) return false

        if (rotationEnabledValue.get())
        {
            // Rotate
            RotationUtils.setTargetRotation(placeRotation!!.rotation, keepRotationTicks)
            RotationUtils.setNextResetTurnSpeed(rotationResetSpeedValue.getMin().coerceAtLeast(10F), rotationResetSpeedValue.getMax().coerceAtLeast(10F))

            // Lock Rotation
            (LiquidBounce.moduleManager[Scaffold::class.java] as Scaffold).lockRotation = null // Prevent to lockRotation confliction
            lockRotation = placeRotation!!.rotation
        }

        placeInfo = placeRotation!!.placeInfo

        return true
    }

    /**
     * Tower visuals
     */
    @EventTarget
    fun onRender2D(@Suppress("UNUSED_PARAMETER") event: Render2DEvent)
    {
        if (counterEnabledValue.get())
        {
            val theWorld = mc.theWorld ?: return
            val thePlayer = mc.thePlayer ?: return

            GL11.glPushMatrix()
            val blockOverlay = LiquidBounce.moduleManager[BlockOverlay::class.java] as BlockOverlay

            val blocksAmount = getBlocksAmount(thePlayer)
            val info = "Blocks: \u00A7${if (blocksAmount <= 16) "c" else if (blocksAmount <= 64) "e" else "7"}$blocksAmount"

            val scaledResolution = ScaledResolution(mc)

            val middleScreenX = scaledResolution.scaledWidth shr 1
            val middleScreenY = scaledResolution.scaledHeight shr 1
            val yoffset = if (blockOverlay.state && blockOverlay.infoEnabledValue.get() && blockOverlay.getCurrentBlock(theWorld) != null) 15f else 0f
            val font = counterFontValue.get()

            RenderUtils.drawBorderedRect(middleScreenX - 2.0f, middleScreenY + yoffset + 5.0f, ((scaledResolution.scaledWidth shr 1) + font.getStringWidth(info)) + 2.0f, middleScreenY + yoffset + font.fontHeight + 7.0f, 3f, -16777216, -16777216)

            classProvider.GlStateManager.resetColor()

            font.drawString(info, middleScreenX.toFloat(), middleScreenY + yoffset + 7.0f, 0xffffff)
            GL11.glPopMatrix()
        }
    }

    @EventTarget
    fun onJump(event: JumpEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (!onJumpValue.get()) return

        val onJumpDelay = onJumpDelayValue.get()

        if (onJumpDelay > 0 && onJumpTimer.hasTimePassed(onJumpDelay.toLong()) && !onJumpDisableWhileMoving.get() || !thePlayer.isMoving && onJumpNoDelayIfNotMovingValue.get()) event.cancelEvent()
    }

    /**
     * @return hotbar blocks amount
     */
    private fun getBlocksAmount(thePlayer: EntityPlayer): Int
    {
        val provider = classProvider

        val inventoryContainer = thePlayer.inventoryContainer

        return (36..44).mapNotNull { inventoryContainer.getSlot(it).stack }.filter { it.item is ItemBlock }.filter { thePlayer.heldItem == it || it.item? as ItemBlock?.block.canAutoBlock }.sumBy(IItemStack::stackSize)
    }

    override val tag: String
        get() = modeValue.get()

    var active = false

    private val keepRotationTicks: Int
        get() = if (rotationKeepRotationEnabledValue.get()) rotationKeepRotationTicksValue.getRandom() else 0
}
