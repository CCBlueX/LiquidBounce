package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.AttackEntityEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket

object VelocityIntave : VelocityMode("Intave") {

    private class ReduceOnAttack(parent: EventListener?) : ToggleableConfigurable(
        parent, "ReduceOnAttack",
        true
    ) {
        private val reduceFactor by float("Factor", 0.6f, 0.6f..1f)
        private val hurtTime by intRange("HurtTime", 5..7, 1..10)
        private val lastAttackTimeToReduce by int("LastAttackTimeToReduce", 2000, 1..10000)
        var lastAttackTime = 0L

        @Suppress("unused")
        private val attackHandler = handler<AttackEntityEvent> { event ->
            if (player.hurtTime in hurtTime && System.currentTimeMillis() - lastAttackTime <= lastAttackTimeToReduce) {
                player.velocity.x *= reduceFactor
                player.velocity.z *= reduceFactor
            }
            lastAttackTime = System.currentTimeMillis()
        }
    }

    init {
        tree(ReduceOnAttack(this))
    }

    private class JumpReset(parent: EventListener?) : ToggleableConfigurable(
        parent, "JumpReset",
        true
    ) {

        private val chance by float("Chance", 50f, 0f..100f, "%")

        private inner class Randomize : ToggleableConfigurable(this, "Randomize", false) {
            val delayTicks by intRange("DelayTicks", 0..5, 0..10)
        }

        private val randomize = tree(Randomize())
        private var isFallDamage = false
        private var currentDelay = 0
        private var delayCounter = 0

        @Suppress("unused")
        private val tickJumpHandler = handler<MovementInputEvent> {
            val shouldJump = Math.random() * 100 < chance && player.hurtTime > 5 && !isFallDamage
            val canJump = player.isOnGround && mc.currentScreen !is InventoryScreen
            val shouldFinallyJump = shouldJump && canJump

            if (randomize.enabled) {
                delayCounter++

                if (delayCounter >= currentDelay) {
                    if (shouldFinallyJump) it.jump = true
                    delayCounter = 0
                    currentDelay = randomize.delayTicks.random()
                }
            } else {
                if (shouldFinallyJump) it.jump = true
            }
        }

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet

            if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
                val velocityX = packet.velocityX / 8000.0
                val velocityY = packet.velocityY / 8000.0
                val velocityZ = packet.velocityZ / 8000.0
            }
        }

    }

    init {
        tree(JumpReset(this))
    }
}
