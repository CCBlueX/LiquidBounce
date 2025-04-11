package net.ccbluex.liquidbounce.features.module.modules.movement.longjump.modes

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.movement.longjump.ModuleLongJump
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import kotlin.math.*

/**
 * @author liangzaihua(https://space.bilibili.com/3493274998278662)
 * @antiCheat Matrix
 * @antiCheatVersion 7.14.5
 * @testedOn mc.loyisa.cn
 */
internal object MatrixFlagLongJump : Choice("MatrixFlag") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleLongJump.mode

    val speed by float("MatrixSpeed", 1.98f, 0.1f..5f)
    val motionY by float("MatrixMotionY", 0.42f, 0.0f..5.0f)
    val delay by int("MatrixDelay", 0, 0..3)

    var yaw: Double = 0.0
    var flagTicks = 0
    var jumped = false
    var fly = false
    var offGroundTicks = 0

    override fun enable() {
        if (!player.isOnGround) {
            jumped = true
            yaw = Math.toRadians(player.yaw.toDouble())
        }
    }

    override fun disable() {
        flagTicks = 0
        jumped = false
        fly = false
    }

    @Suppress("unused")
    val repeatable = tickHandler {
        if (player.isOnGround) offGroundTicks = 0
        else offGroundTicks++

        if (offGroundTicks >= delay) fly = true
        if ((flagTicks > 1 && !jumped && player.isOnGround)) flagTicks = 0
        if (player.isOnGround) {
            yaw = Math.toRadians(player.yaw.toDouble())
            jumped = true
        }
        if ((!player.isOnGround && jumped) && fly) {
            if (flagTicks <= 1) {
                player.movement.x = -sin(yaw) * speed
                player.movement.z = cos(yaw) * speed
                player.movement.y = motionY.toDouble()
            }
            if (flagTicks >= 1) {
                jumped = false
                val longJump = (ModuleManager["LongJump"] as ModuleLongJump)
                if (longJump.autoDisable) longJump.enabled = false
            }
        }
    }


    @Suppress("unused")
    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is PlayerPositionLookS2CPacket) flagTicks++
    }
}
