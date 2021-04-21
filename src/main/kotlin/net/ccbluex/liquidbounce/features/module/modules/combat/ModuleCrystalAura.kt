package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.client.MC_1_8
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.combat.TargetTracker
import net.ccbluex.liquidbounce.utils.entity.boxedDistanceTo
import net.ccbluex.liquidbounce.utils.entity.eyesPos
import net.minecraft.entity.Entity
import net.minecraft.entity.decoration.EndCrystalEntity
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.util.Hand

object ModuleCrystalAura : Module("CrystalAura", Category.COMBAT) {
    private val range by float("Range", 4f, 1f..8f)
    private val swing by boolean("Swing", true)
    private val rotations = tree(RotationsConfigurable())
    private val targetTracker = tree(TargetTracker())

    override fun disable() {
        targetTracker.cleanup()
    }

    val repeatable = repeatable {
        update()
    }

    private fun update() {
        if (player.isSpectator) {
            return
        }

        targetTracker.validateLock { it.boxedDistanceTo(player) <= range }

        val eyes = player.eyesPos

        // Multi target mode below, I don't think we need Switch
        for (target in world.entities) {
            if (target is EndCrystalEntity) {
                if (target.boxedDistanceTo(player) > range) {
                    continue
                }

                val box = target.boundingBox

                // Find the best spot
                val (rotation, _) = RotationManager.raytraceBox(
                    eyes,
                    box,
                    throughWalls = false,
                    range = range.toDouble()
                ) ?: continue

                // Lock on target tracker
                targetTracker.lock(target)

                // Aim on em
                RotationManager.aimAt(rotation, configurable = rotations)
                break
            }
        }
        // Attack
        val target = targetTracker.lockedOnTarget ?: return

        attackEntity(target)
    }

    private fun attackEntity(entity: Entity) {
        EventManager.callEvent(AttackEvent(entity))

        // Swing before attacking (on 1.8)
        if (swing && protocolVersion == MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        network.sendPacket(PlayerInteractEntityC2SPacket(entity, player.isSneaking))

        // Swing after attacking (on 1.9+)
        if (swing && protocolVersion != MC_1_8) {
            player.swingHand(Hand.MAIN_HAND)
        }

        // Reset cooldown
        player.resetLastAttackedTicks()
    }
}
