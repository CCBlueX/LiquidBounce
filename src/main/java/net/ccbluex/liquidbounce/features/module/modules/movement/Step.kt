/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.features.module.modules.exploit.Phase
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerRangeValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.stats.StatList
import net.minecraft.world.World

@ModuleInfo(name = "Step", description = "Allows you to step up blocks.", category = ModuleCategory.MOVEMENT)
class Step : Module()
{

    /**
     * OPTIONS
     */

    private val modeValue = ListValue("Mode", arrayOf("Vanilla", "Jump", "NCP", "MotionNCP", "OldNCP", "AAC3.1.5", "AAC3.2.0", "AAC3.3.4", "Spartan127", "Rewinside"), "NCP")

    private val motionNCPBoostValue = object : FloatValue("MotionNCPBoost", 0.7F, 0F, 0.7F, "MotionNCP-Boost")
    {
        override fun showCondition() = modeValue.get().equals("MotionNCP", ignoreCase = true)
    }
    val airStepValue = BoolValue("AirStep", false)
    val airStepHeightValue = object : FloatValue("AirStepHeight", 1F, 0.6F, 10F)
    {
        override fun showCondition() = airStepValue.get()
    }
    private val heightValue = FloatValue("Height", 1F, 0.6F, 10F)
    private val jumpHeightValue = FloatValue("JumpHeight", 0.42F, 0.37F, 0.42F)
    private val delayValue = IntegerRangeValue("Delay", 0, 0, 0, 1000, "MaxDelay" to "MinDelay")
    private val resetSpeedAfterStepConfirmValue = BoolValue("ResetXZAfterStep", false)
    private val checkLiquid = BoolValue("CheckLiquid", true)

    /**
     * VALUES
     */

    private val specialCases = hashSetOf("jump", "motionncp", "aac3.2.0", "aac3.3.4")

    private var isStep = false
    private var aac334step = false

    private var stepX = 0.0
    private var stepY = 0.0
    private var stepZ = 0.0

    private var motionNCPNextStep = 0
    private var spartanSwitch = false

    private val timer = MSTimer()
    private var delay = delayValue.getRandomLong()

    override fun onDisable()
    {
        // Change step height back to default (0.6 is default)
        (mc.thePlayer ?: return).stepHeight = 0.6F
        motionNCPNextStep = 0
    }

    @EventTarget
    fun onUpdate(@Suppress("UNUSED_PARAMETER") event: UpdateEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val canStep = !mc.gameSettings.keyBindJump.isKeyDown && thePlayer.isMoving

        // Motion steps
        when (modeValue.get().lowercase())
        {
            "jump" -> if (thePlayer.isCollidedHorizontally && thePlayer.onGround && canStep && couldStep(theWorld, thePlayer))
            {
                fakeJump(thePlayer)
                thePlayer.motionY = jumpHeightValue.get().toDouble()
            }

            "aac3.2.0" -> if (thePlayer.isCollidedHorizontally && !thePlayer.cantBoostUp && canStep)
            {
                if (thePlayer.onGround && timer.hasTimePassed(delay) && couldStep(theWorld, thePlayer))
                {
                    isStep = true

                    fakeJump(thePlayer)
                    thePlayer.motionY += 0.620000001490116

                    thePlayer.boost(0.2F)
                    resetTimer()
                }

                thePlayer.onGround = true
            }
            else isStep = false

            "aac3.3.4" -> if (thePlayer.isCollidedHorizontally && canStep)
            {
                if (thePlayer.onGround && couldStep(theWorld, thePlayer))
                {
                    thePlayer.multiply(1.26)
                    thePlayer.jump()
                    aac334step = true
                }

                if (aac334step)
                {
                    thePlayer.motionY -= 0.015

                    if (!thePlayer.isUsingItem && thePlayer.movementInput.moveStrafe == 0F) thePlayer.jumpMovementFactor = 0.3F
                }
            }
            else aac334step = false
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent)
    {
        val theWorld = mc.theWorld ?: return
        val thePlayer = mc.thePlayer ?: return

        val mode = modeValue.get()
        val motionNCPBoost = motionNCPBoostValue.get()

        // NCP Motion steps
        if (mode.equals("MotionNCP", ignoreCase = true) && thePlayer.isCollidedHorizontally && !mc.gameSettings.keyBindJump.isKeyDown && thePlayer.isMoving) when
        {
            thePlayer.onGround && couldStep(theWorld, thePlayer) ->
            {
                fakeJump(thePlayer)
                thePlayer.motionY = 0.0
                event.y = 0.41999998688698 // Jump step 1 (0.42)
                motionNCPNextStep = 1
            }

            motionNCPNextStep == 1 ->
            {
                event.y = 0.33319999363422 // Jump step 2 (0.333)
                motionNCPNextStep = 2
            }

            motionNCPNextStep == 2 ->
            {
                event.y = 0.248135998590947 // Jump step 3 (0.248)

                if (motionNCPBoost > 0F) event.forward(motionNCPBoost, thePlayer.moveDirectionDegrees)

                motionNCPNextStep = 0
            }
        }
    }

    @EventTarget
    fun onStep(event: StepEvent)
    {
        val moduleManager = LiquidBounce.moduleManager

        // Phase  should disable step
        if (moduleManager[Phase::class.java].state)
        {
            event.stepHeight = 0F
            return
        }

        val thePlayer = mc.thePlayer ?: return

        // Some fly modes should disable step
        val fly = moduleManager[Fly::class.java] as Fly

        if (fly.state)
        {
            val flyMode = fly.modeValue.get()

            if (flyMode.equals("Hypixel", ignoreCase = true) || flyMode.equals("Rewinside", ignoreCase = true) || flyMode.equals("Mineplex", ignoreCase = true) && thePlayer.inventory.getCurrentItem() == null)
            {
                event.stepHeight = 0F
                return
            }
        }

        val mode = modeValue.get()

        // Set step to default in some cases
        if ((!thePlayer.onGround && !airStepValue.get()) || !timer.hasTimePassed(delay) || specialCases.contains(mode.lowercase()) || checkLiquid.get() && (thePlayer.isInWater || thePlayer.isInLava))
        {
            thePlayer.stepHeight = 0.6F
            event.stepHeight = 0.6F
            return
        }

        // Set step height
        val height = heightValue.get()

        thePlayer.stepHeight = height
        event.stepHeight = height

        // Detect possible step
        if (event.stepHeight > 0.6F)
        {
            isStep = true
            stepX = thePlayer.posX
            stepY = thePlayer.posY
            stepZ = thePlayer.posZ
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onStepConfirm(@Suppress("UNUSED_PARAMETER") event: StepConfirmEvent)
    {
        val thePlayer = mc.thePlayer ?: return

        if (!isStep) // Check if step
            return

        val stepHeight = thePlayer.entityBoundingBox.minY - stepY
        if (stepHeight > 0.6)
        {

            // Check if full block step
            val mode = modeValue.get().lowercase()
            val networkManager = mc.netHandler.networkManager

            when (mode)
            {
                "ncp", "aac3.1.5" ->
                {
                    fakeJump(thePlayer)

                    // Half legit step (1 packet missing) [COULD TRIGGER TOO MANY PACKETS]
                    // networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false)) // 0.42
                    // networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false)) // 0.333

                    performNCPPacketStep(thePlayer, stepX, stepY, stepZ, stepHeight)
                    resetTimer()
                }

                "spartan127" ->
                {
                    fakeJump(thePlayer)

                    if (spartanSwitch)
                    {
                        // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                        networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false)) // 0.42
                        networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false)) // 0.333
                        networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)) // 0.248
                    }
                    else // Force step
                        networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 0.6, stepZ, false))

                    // Spartan b127 allows one unlegit step so just swap between legit and unlegit
                    spartanSwitch = !spartanSwitch

                    // Reset timer
                    resetTimer()
                }

                "rewinside" ->
                {
                    fakeJump(thePlayer)

                    // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false)) // 0.42
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false)) // 0.333
                    networkManager.sendPacketWithoutEvent(C04PacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)) // 0.248

                    // Reset timer
                    resetTimer()
                }
            }

            if (resetSpeedAfterStepConfirmValue.get()) thePlayer.zeroXZ()
        }

        isStep = false
        stepX = 0.0
        stepY = 0.0
        stepZ = 0.0
    }

    private fun performNCPPacketStep(thePlayer: Entity, stepX: Double, stepY: Double, stepZ: Double, stepHeight: Double)
    {
        val networkManager = mc.netHandler.networkManager
        // Values from NCPStep v1.0 by york

        val (motions, resetXZ) = when
        {
            stepHeight > 2.019 -> arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869, 2.019, 1.919) to true
            stepHeight > 1.869 -> arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652, 1.869) to true
            stepHeight > 1.5 -> arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652) to true
            stepHeight > 1.015 -> arrayOf(0.425, 0.821, 0.699, 0.599, 1.022, 1.372, 1.652) to true
            stepHeight > 0.875 -> arrayOf(0.41999998688698, 0.7531999805212) to false
            stepHeight > 0.6 -> arrayOf(0.39, 0.6938) to false
            else -> return
        }

        motions.map { C04PacketPlayerPosition(stepX, stepY + it, stepZ, false) }.forEach(networkManager::sendPacketWithoutEvent)

        if (resetXZ) thePlayer.zeroXZ()
    }

    private fun resetTimer()
    {
        timer.reset()
        delay = delayValue.getRandomLong()
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent)
    {
        val packet = event.packet

        if (packet is C03PacketPlayer && isStep && modeValue.get().equals("OldNCP", ignoreCase = true))
        {
            packet.y += 0.07
            isStep = false
        }
    }

    // There could be some anti cheats which tries to detect step by checking for achievements and stuff (ex: Hypixel Watchdog)
    private fun fakeJump(thePlayer: EntityPlayer)
    {
        thePlayer.isAirBorne = true
        thePlayer.triggerAchievement(StatList.jumpStat)
    }

    private fun couldStep(theWorld: World, thePlayer: EntityPlayer): Boolean
    {
        val (x, z) = ZERO.applyForward(0.4, thePlayer.moveDirectionDegrees)
        return theWorld.getCollisionBoxes(thePlayer.entityBoundingBox.offset(x, 1.001335979112147, z)).isEmpty() && (!checkLiquid.get() || !thePlayer.isInWater && !thePlayer.isInLava)
    }

    fun canAirStep(): Boolean
    {
        val mode = modeValue.get()
        return sequenceOf("Vanilla", "NCP", "OldNCP", "AAC3.1.5", "Spartan127", "Rewinside").any { mode.equals(it, ignoreCase = true) }
    }

    override val tag: String
        get() = modeValue.get()
}
