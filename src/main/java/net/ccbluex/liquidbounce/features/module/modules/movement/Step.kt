/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.exploit.Phase
import net.ccbluex.liquidbounce.utils.MovementUtils.direction
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.PacketUtils.sendPackets
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.stats.StatList
import kotlin.math.cos
import kotlin.math.sin

object Step : Module("Step", Category.MOVEMENT, gameDetecting = false, hideModule = false) {

    /**
     * OPTIONS
     */

    private val mode by ListValue("Mode",
        arrayOf("Vanilla", "Jump", "NCP", "MotionNCP", "OldNCP", "AAC", "LAAC", "AAC3.3.4", "Spartan", "Rewinside"), "NCP")

        private val height by FloatValue("Height", 1F, 0.6F..10F)
            { mode !in arrayOf("Jump", "MotionNCP", "LAAC", "AAC3.3.4") }
        private val jumpHeight by FloatValue("JumpHeight", 0.42F, 0.37F..0.42F)
            { mode == "Jump" }

    private val delay by IntegerValue("Delay", 0, 0..500)

    /**
     * VALUES
     */

    private var isStep = false
    private var stepX = 0.0
    private var stepY = 0.0
    private var stepZ = 0.0

    private var ncpNextStep = 0
    private var spartanSwitch = false
    private var isAACStep = false

    private val timer = MSTimer()

    override fun onDisable() {
        val player = mc.thePlayer ?: return

        // Change step height back to default (0.6 is default)
        player.stepHeight = 0.6F
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val mode = mode
        val player = mc.thePlayer ?: return

        // Motion steps
        when (mode) {
            "Jump" ->
                if (player.isCollidedHorizontally && player.onGround && !mc.gameSettings.keyBindJump.isKeyDown) {
                    fakeJump()
                    player.motionY = jumpHeight.toDouble()
                }
            "LAAC" ->
                if (player.isCollidedHorizontally && !thePlayer.isOnLadder && !thePlayer.isInWater && !thePlayer.isInLava && !thePlayer.isInWeb) {
                    if (player.onGround && timer.hasTimePassed(delay)) {
                        isStep = true

                        fakeJump()
                        player.motionY += 0.620000001490116

                        val yaw = direction
                        player.motionX -= sin(yaw) * 0.2
                        player.motionZ += cos(yaw) * 0.2
                        timer.reset()
                    }

                    player.onGround = true
                } else isStep = false
            "AAC3.3.4" ->
                if (player.isCollidedHorizontally && isMoving) {
                    if (player.onGround && couldStep()) {
                        player.motionX *= 1.26
                        player.motionZ *= 1.26
                        player.tryJump()
                        isAACStep = true
                    }

                    if (isAACStep) {
                        player.motionY -= 0.015

                        if (!thePlayer.isUsingItem && player.movementInput.moveStrafe == 0F)
                            player.jumpMovementFactor = 0.3F
                    }
                } else isAACStep = false
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        val player = mc.thePlayer ?: return

        if (mode != "MotionNCP" || !thePlayer.isCollidedHorizontally || mc.gameSettings.keyBindJump.isKeyDown)
            return

        // Motion steps
        when {
            player.onGround && couldStep() -> {
                fakeJump()
                player.motionY = 0.0
                event.y = 0.41999998688698
                ncpNextStep = 1
            }

            ncpNextStep == 1 -> {
                event.y = 0.7531999805212 - 0.41999998688698
                ncpNextStep = 2
            }

            ncpNextStep == 2 -> {
                val yaw = direction

                event.y = 1.001335979112147 - 0.7531999805212
                event.x = -sin(yaw) * 0.7
                event.z = cos(yaw) * 0.7

                ncpNextStep = 0
            }
        }
    }

    @EventTarget
    fun onStep(event: StepEvent) {
        val player = mc.thePlayer ?: return

        // Phase should disable step
        if (Phase.handleEvents()) {
            event.stepHeight = 0F
            return
        }

        // Some fly modes should disable step
        if (Fly.handleEvents() && Fly.mode in arrayOf("Hypixel", "OtherHypixel", "LatestHypixel", "Rewinside", "Mineplex")
            && player.inventory.getCurrentItem() == null) {
            event.stepHeight = 0F
            return
        }

        val mode = mode

        // Set step to default in some cases
        if (!thePlayer.onGround || !timer.hasTimePassed(delay) ||
                mode in arrayOf("Jump", "MotionNCP", "LAAC", "AAC3.3.4")) {
            player.stepHeight = 0.6F
            event.stepHeight = 0.6F
            return
        }

        // Set step height
        val height = height
        player.stepHeight = height
        event.stepHeight = height

        // Detect possible step
        if (event.stepHeight > 0.6F) {
            isStep = true
            stepX = player.posX
            stepY = player.posY
            stepZ = player.posZ
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onStepConfirm(event: StepConfirmEvent) {
        val player = mc.thePlayer

        if (player == null || !isStep) // Check if step
            return

        if (player.entityBoundingBox.minY - stepY > 0.6) { // Check if full block step

            when (mode) {
                "NCP", "AAC" -> {
                    fakeJump()

                    // Half legit step (1 packet missing) [COULD TRIGGER TOO MANY PACKETS]
                    sendPackets(
                        C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false),
                        C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false)
                    )
                    timer.reset()
                }
                "Spartan" -> {
                    fakeJump()

                    if (spartanSwitch) {
                        // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                        sendPackets(
                            C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false),
                            C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false),
                            C04PacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)
                        )
                    } else // Force step
                        sendPacket(C04PacketPlayerPosition(stepX, stepY + 0.6, stepZ, false))

                    // Spartan allows one unlegit step so just swap between legit and unlegit
                    spartanSwitch = !spartanSwitch

                    // Reset timer
                    timer.reset()
                }
                "Rewinside" -> {
                    fakeJump()

                    // Vanilla step (3 packets) [COULD TRIGGER TOO MANY PACKETS]
                    sendPackets(
                        C04PacketPlayerPosition(stepX, stepY + 0.41999998688698, stepZ, false),
                        C04PacketPlayerPosition(stepX, stepY + 0.7531999805212, stepZ, false),
                        C04PacketPlayerPosition(stepX, stepY + 1.001335979112147, stepZ, false)
                    )

                    // Reset timer
                    timer.reset()
                }
            }
        }

        isStep = false
        stepX = 0.0
        stepY = 0.0
        stepZ = 0.0
    }

    @EventTarget(ignoreCondition = true)
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is C03PacketPlayer && isStep && mode == "OldNCP") {
            packet.y += 0.07
            isStep = false
        }
    }

    // There could be some anti cheats which tries to detect step by checking for achievements and stuff
    private fun fakeJump() {
        val player = mc.thePlayer ?: return

        player.isAirBorne = true
        player.triggerAchievement(StatList.jumpStat)
    }

    private fun couldStep(): Boolean {
        val yaw = direction
        val x = -sin(yaw) * 0.4
        val z = cos(yaw) * 0.4

        return mc.theWorld.getCollisionBoxes(mc.thePlayer.entityBoundingBox.offset(x, 1.001335979112147, z))
                .isEmpty()
    }

    override val tag
        get() = mode
}