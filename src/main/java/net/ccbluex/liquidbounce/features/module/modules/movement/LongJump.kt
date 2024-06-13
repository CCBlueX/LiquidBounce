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
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.aac.AACv1
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.aac.AACv2
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.aac.AACv3
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.ncp.NCP
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other.Hycraft
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other.Redesky
import net.ccbluex.liquidbounce.features.module.modules.movement.longjumpmodes.other.Buzz
import net.ccbluex.liquidbounce.utils.MovementUtils.isMoving
import net.ccbluex.liquidbounce.utils.MovementUtils.speed
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue

object LongJump : Module("LongJump", Category.MOVEMENT) {

    private val longJumpModes = arrayOf(
        // NCP
        NCP,

        // AAC
        AACv1, AACv2, AACv3,

        // Other
        Redesky, Hycraft, Buzz,
    )

    private val modes = longJumpModes.map { it.modeName }.toTypedArray()

    val mode by ListValue("Mode", modes, "NCP")
        val ncpBoost by FloatValue("NCPBoost", 4.25f, 1f..10f) { mode == "NCP" }

    private val autoJump by BoolValue("AutoJump", false)

    var jumped = false
    var canBoost = false
    var teleported = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (LadderJump.jumped) speed *= 1.08f

        if (jumped) {
            val mode = mode

            if (mc.thePlayer.onGround || mc.thePlayer.capabilities.isFlying) {
                jumped = false

                if (mode == "NCP") {
                    mc.thePlayer.motionX = 0.0
                    mc.thePlayer.motionZ = 0.0
                }
                return
            }

            modeModule.onUpdate()
        }
        if (autoJump && mc.thePlayer.onGround && isMoving) {
            jumped = true
            mc.thePlayer.tryJump()
        }
    }

    @EventTarget
    fun onMove(event: MoveEvent) {
        modeModule.onMove(event)
    }

    @EventTarget(ignoreCondition = true)
    fun onJump(event: JumpEvent) {
        jumped = true
        canBoost = true
        teleported = false

        if (handleEvents()) {
            modeModule.onJump(event)
        }
    }

    override val tag
        get() = mode

    private val modeModule
        get() = longJumpModes.find { it.modeName == mode }!!
}
