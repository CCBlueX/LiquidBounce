/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.MovementUtils.strafe
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.util.EnumFacing

object LongJump : Module("LongJump", ModuleCategory.MOVEMENT) {

    private val mode by ListValue("Mode", arrayOf("NCP", "AACv1", "AACv2", "AACv3", "Mineplex", "Mineplex2", "Mineplex3", "Redesky", "Hycraft"), "NCP")
    private val ncpBoost by FloatValue("NCPBoost", 4.25f, 1f..10f) { mode == "NCP" }
    private val autoJump by BoolValue("AutoJump", false)

    private var jumped = false
    private var canBoost = false
    private var teleported = false
    private var canMineplexBoost = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (LadderJump.jumped)
            speed *= 1.08f

        val thePlayer = mc.thePlayer ?: return

        if (jumped) {
            val mode = mode

            if (thePlayer.onGround || thePlayer.capabilities.isFlying) {
                jumped = false
                canMineplexBoost = false

                if (mode == "NCP") {
                    thePlayer.motionX = 0.0
                    thePlayer.motionZ = 0.0
                }
                return
            }
            run {
                when (mode.lowercase()) {
                    "ncp" -> {
                        speed *= if (canBoost) ncpBoost else 1f
                        canBoost = false
                    }
                    "aacv1" -> {
                        thePlayer.motionY += 0.05999
                        speed *= 1.08f
                    }
                    "aacv2", "mineplex3" -> {
                        thePlayer.jumpMovementFactor = 0.09f
                        thePlayer.motionY += 0.01320999999999999
                        thePlayer.jumpMovementFactor = 0.08f
                        strafe()
                    }
                    "aacv3" -> {
                        if (thePlayer.fallDistance > 0.5f && !teleported) {
                            val value = 3.0
                            val horizontalFacing = thePlayer.horizontalFacing
                            var x = 0.0
                            var z = 0.0

                            when(horizontalFacing) {
                                EnumFacing.NORTH -> z = -value
                                EnumFacing.EAST -> x = +value
                                EnumFacing.SOUTH -> z = +value
                                EnumFacing.WEST -> x = -value
                                else -> {
                                }
                            }

                            thePlayer.setPosition(thePlayer.posX + x, thePlayer.posY, thePlayer.posZ + z)
                            teleported = true
                        }
                    }
                    "mineplex" -> {
                        thePlayer.motionY += 0.01320999999999999
                        thePlayer.jumpMovementFactor = 0.08f
                        strafe()
                    }
                    "mineplex2" -> {
                        if (!canMineplexBoost)
                            return@run

                        thePlayer.jumpMovementFactor = 0.1f
                        if (thePlayer.fallDistance > 1.5f) {
                            thePlayer.jumpMovementFactor = 0f
                            thePlayer.motionY = (-10f).toDouble()
                        }

                        strafe()
                    }
                    "redesky" -> {
                        thePlayer.jumpMovementFactor = 0.15f
                        thePlayer.motionY += 0.05f
                    }
                    "hycraft" -> {
                        if(thePlayer.motionY < 0) {
                            thePlayer.motionY *= 0.75f
                            thePlayer.jumpMovementFactor = 0.055f
                        } else {
                            thePlayer.motionY += 0.02f
                            thePlayer.jumpMovementFactor = 0.08f
                        }
                    }
                }
            }
        }
        if (autoJump && thePlayer.onGround && isMoving) {
            jumped = true
            thePlayer.jump()
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        val thePlayer = mc.thePlayer ?: return
        val mode = mode

        if (mode == "Mineplex3") {
            if (thePlayer.fallDistance != 0f)
                thePlayer.motionY += 0.037
        } else if (mode == "NCP" && !isMoving && jumped) {
            thePlayer.motionX = 0.0
            thePlayer.motionZ = 0.0
            event.zeroXZ()
        }
    }

    @EventTarget(ignoreCondition = true)
    fun onJump(event: JumpEvent) {
        jumped = true
        canBoost = true
        teleported = false

        if (state) {
            when (mode.lowercase()) {
                "mineplex" -> event.motion = event.motion * 4.08f
                "mineplex2" -> {
                    if (mc.thePlayer.isCollidedHorizontally) {
                        event.motion = 2.31f
                        canMineplexBoost = true
                        mc.thePlayer.onGround = false
                    }
                }
            }
        }
    }

    override val tag
        get() = mode
}
