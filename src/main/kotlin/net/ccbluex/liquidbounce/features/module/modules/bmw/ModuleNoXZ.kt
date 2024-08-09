package net.ccbluex.liquidbounce.features.module.modules.bmw

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.AttackEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.killaura.ModuleKillAura.range as killAuraRange
import net.minecraft.entity.Entity
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

object ModuleNoXZ : Module("NoXZ", Category.BMW) {

    private val xzMultiple by float("XZMultiple", 0.6f, 0f..1f)
    private val noRange by boolean("NoRange", true)
    private val rangeAsKillAura by boolean("RangeAsKillAura", true)
    private val range by float("Range", 3f, 0f..8f)

    private var velocityInput = false

    val repeatHandler = repeatable {
        if (velocityInput && player.hurtTime == 0) {
            velocityInput = false
        }
    }

    private fun isInRange(entity: Entity): Boolean =
        noRange || player.distanceTo(entity) <= if (rangeAsKillAura) {
            killAuraRange
        } else {
            range
        }

    val attackEventHandler = handler<AttackEvent> { event ->
        if (velocityInput &&
            event.enemy.isPlayer &&
            mc.options.forwardKey.isPressed &&
            !player.isInFluid &&
            !player.isHoldingOntoLadder &&
            isInRange(event.enemy)) {

            player.isSprinting = true
            player.movement.x *= xzMultiple
            player.movement.z *= xzMultiple
        }
    }

    val packetEventHandler = handler<PacketEvent> { event ->
        if (event.packet is EntityVelocityUpdateS2CPacket) {
            velocityInput = true
        }
        if (event.packet is ExplosionS2CPacket) {
            event.cancelEvent()
        }
    }

    override fun enable() {
        velocityInput = false
    }
}
