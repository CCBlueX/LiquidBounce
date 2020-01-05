package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.JumpEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.util.MathHelper

@ModuleInfo(name = "Velocity", description = "Allows you to modify the amount of knockback you take.", category = ModuleCategory.COMBAT)
class Velocity : Module() {
    private val horizontalValue = FloatValue("Horizontal", 0f, 0f, 1f)
    private val verticalValue = FloatValue("Vertical", 0f, 0f, 1f)
    private val modeValue = ListValue("Mode", arrayOf("Simple", "AAC", "AACPush", "AACZero", "Jump", "Reverse", "Reverse2", "Glitch"), "Simple")
    private val reverseStrengthValue = FloatValue("ReverseStrength", 1.0f, 0.1f, 1f)
    private val reverse2StrenghtValue = FloatValue("Reverse2Strength", 0.05f, 0.02f, 0.1f)
    private val aacPushXZReducerValue = FloatValue("AACPushXZReducer", 2f, 1f, 3f)
    private val aacPushYReducerValue = BoolValue("AACPushYReducer", true)
    private var velocityTime: Long = 0
    private var gotVelocity = false
    private var gotHurt = false

    override fun onDisable() {
        if (mc.thePlayer == null)
            return

        mc.thePlayer.speedInAir = 0.02f

        super.onDisable()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent?) {
        if (mc.thePlayer.isInWater) return

        when (modeValue.get().toLowerCase()) {
            "reverse" -> {
                if (!gotVelocity)
                    return

                if (!mc.thePlayer.onGround && !mc.thePlayer.isInWater && !mc.thePlayer.isInLava && !mc.thePlayer.isInWeb) {
                    MovementUtils.strafe(MovementUtils.getSpeed() * reverseStrengthValue.get())
                } else if (System.currentTimeMillis() - velocityTime > 80L) {
                    gotVelocity = false
                }
            }
            "aac" -> {
                if (velocityTime != 0L && System.currentTimeMillis() - velocityTime > 80L) {
                    mc.thePlayer.motionX *= horizontalValue.get()
                    mc.thePlayer.motionZ *= verticalValue.get()

                    velocityTime = 0L
                }
            }
            "jump" -> if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.onGround) {
                val f: Float = Math.toRadians(mc.thePlayer.rotationYaw.toDouble()).toFloat()

                mc.thePlayer.motionX -= MathHelper.sin(f) * 0.2f.toDouble()
                mc.thePlayer.motionZ += MathHelper.cos(f) * 0.2f.toDouble()
                mc.thePlayer.motionY = 0.42
            }
            "aacpush" -> {
                if (mc.thePlayer.movementInput.jump)
                    return

                if (velocityTime != 0L && System.currentTimeMillis() - velocityTime > 80L)
                    velocityTime = 0L

                if (mc.thePlayer.hurtTime > 0 && mc.thePlayer.motionX != 0.0 && mc.thePlayer.motionZ != 0.0)
                    mc.thePlayer.onGround = true

                if (mc.thePlayer.hurtResistantTime > 0 && aacPushYReducerValue.get())
                    mc.thePlayer.motionY -= 0.0144

                if (mc.thePlayer.hurtResistantTime >= 19) {
                    val reduce = aacPushXZReducerValue.get().toDouble()

                    mc.thePlayer.motionX /= reduce
                    mc.thePlayer.motionZ /= reduce
                }
            }
            "glitch" -> {
                mc.thePlayer.noClip = gotVelocity

                if (mc.thePlayer.hurtTime == 7)
                    mc.thePlayer.motionY = 0.4

                gotVelocity = false
            }
            "reverse2" -> {
                if (!gotVelocity) {
                    mc.thePlayer.speedInAir = 0.02f
                    return
                }

                if (mc.thePlayer.hurtTime > 0) {
                    gotHurt = true
                }
                if (!mc.thePlayer.onGround && !mc.thePlayer.isInWater && !mc.thePlayer.isInLava && !mc.thePlayer.isInWeb) {
                    if (gotHurt)
                        mc.thePlayer.speedInAir = reverse2StrenghtValue.get()
                } else if (System.currentTimeMillis() - velocityTime > 80L) {
                    gotVelocity = false
                    gotHurt = false
                }
            }
            "aaczero" -> if (mc.thePlayer.hurtTime > 0) {
                if (!gotVelocity || mc.thePlayer.onGround || mc.thePlayer.fallDistance > 2f)
                    return

                mc.thePlayer.addVelocity(0.0, -1.0, 0.0)
                mc.thePlayer.onGround = true
            } else gotVelocity = false
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet

        if (packet is S12PacketEntityVelocity && mc.thePlayer != null && mc.theWorld != null) {
            if (mc.theWorld.getEntityByID(packet.entityID) === mc.thePlayer) {
                velocityTime = System.currentTimeMillis()

                when (modeValue.get().toLowerCase()) {
                    "simple" -> {
                        val horizontal = horizontalValue.get().toDouble()
                        val vertical = verticalValue.get().toDouble()

                        if (horizontal == 0.0 && vertical == 0.0)
                            event.cancelEvent()

                        packet.motionX = (packet.getMotionX() * horizontal).toInt()
                        packet.motionY = (packet.getMotionY() * vertical).toInt()
                        packet.motionZ = (packet.getMotionZ() * horizontal).toInt()
                    }
                    "reverse", "reverse2", "aaczero" -> {
                        gotVelocity = true
                    }
                    "glitch" -> {
                        if (mc.thePlayer.onGround) {
                            gotVelocity = true
                            event.cancelEvent()
                        }
                    }
                }
            }
        }

        if (packet is S27PacketExplosion)
            event.cancelEvent()
    }

    @EventTarget
    fun onJump(event: JumpEvent) {
        if (mc.thePlayer == null || mc.thePlayer.isInWater) return
        when (modeValue.get().toLowerCase()) {
            "aacpush", "aaczero" -> if (mc.thePlayer.hurtTime > 0) event.cancelEvent()
        }
    }

    override val tag: String
        get() = modeValue.get()
}