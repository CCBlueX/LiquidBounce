package net.ccbluex.liquidbounce.features.module.modules.combat.autobow

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.autobow.aimbot.AimPlanner
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleMurderMystery
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.QuickAccess.interaction
import net.ccbluex.liquidbounce.utils.client.QuickAccess.player
import net.ccbluex.liquidbounce.utils.client.QuickAccess.world
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.SimulatedArrow
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.entity.EntityDimensions
import net.minecraft.item.BowItem
import net.minecraft.item.TridentItem
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import java.util.*

/**
 * Automatically shoots with your bow when you aim correctly at an enemy or when the bow is fully charged.
 */
object ModuleAutoBowAutoShoot : ToggleableConfigurable(ModuleAutoBow, "AutoShoot", true) {

    val charged by int("Charged", 15, 3..20)

    val chargedRandom by floatRange("ChargedRandom", 0.0F..0.0F, -10.0F..10.0F)
    val delayBetweenShots by int("DelayBetweenShots", 0, 0..5000)

    val aimThreshold by float("AimThreshold", 1.5F, 1.0F..4.0F)
    val requiresHypotheticalHit by boolean("RequiresHypotheticalHit", false)

    private val random = Random()
    var currentChargeRandom: Int? = null

    /**
     * Keeps track of the last bow shot that has taken place
     */
    private val lastShotTimer = Chronometer()

    @JvmStatic
    fun onStopUsingItem() {
        if (player.activeItem.item is BowItem) {
            lastShotTimer.reset()
        }
    }

    fun updateChargeRandom() {
        val lenHalf = (chargedRandom.endInclusive - chargedRandom.start) / 2.0F
        val mid = chargedRandom.start + lenHalf

        currentChargeRandom =
            (mid + this.random.nextGaussian() * lenHalf).toInt()
                .coerceIn(chargedRandom.start.toInt()..chargedRandom.endInclusive.toInt())
    }

    fun getChargedRandom(): Int {
        if (currentChargeRandom == null) {
            updateChargeRandom()
        }

        return currentChargeRandom!!
    }

    val tickRepeatable =
        handler<GameTickEvent>(priority = 100) {
            val currentItem = player.activeItem?.item

            // Should check if player is using bow
            if (currentItem !is BowItem && currentItem !is TridentItem) {
                return@handler
            }

            if (player.itemUseTime < charged + getChargedRandom()) { // Wait until the bow is fully charged
                return@handler
            }
            if (!lastShotTimer.hasElapsed(delayBetweenShots.toLong())) {
                return@handler
            }

            if (!wouldBeAHit())
                return@handler

            interaction.stopUsingItem(player)
            updateChargeRandom()
        }

    @Suppress("ReturnCount")
    private fun wouldBeAHit(): Boolean {
        if (requiresHypotheticalHit) {
            val bowaimbotTarget = ModuleAutoBowAimbot.targetTracker.lockedOnTarget

            val hypotheticalHit = if (!ModuleAutoBowAimbot.enabled) {
                getHypotheticalHit(findPlayersForUnselectiveHypotheticalHit(), maxTicks = 2 * 20)
            } else if (bowaimbotTarget is AbstractClientPlayerEntity) {
                println(bowaimbotTarget.velocity.horizontalLength())

                getHypotheticalHit(listOf(bowaimbotTarget), maxTicks = 5 * 20)
            } else {
                return false
            }

            if (hypotheticalHit == null || !hypotheticalHit.shouldBeAttacked()) {
                return false
            }

            if (ModuleMurderMystery.enabled && !ModuleMurderMystery.shouldAttack(hypotheticalHit)) {
                return false
            }
        }

        if (ModuleAutoBowAimbot.enabled) {
            if (ModuleAutoBowAimbot.targetTracker.lockedOnTarget == null) {
                return false
            }

            val targetRotation = RotationManager.targetRotation ?: return false

            val aimDifference = RotationManager.rotationDifference(
                RotationManager.currentRotation
                    ?: player.rotation, targetRotation
            )

            if (aimDifference > aimThreshold) {
                return false
            }
        }
        return true
    }

    fun getHypotheticalHit(players: List<AbstractClientPlayerEntity>, maxTicks: Int): AbstractClientPlayerEntity? {
        val rotation = RotationManager.currentRotation ?: player.rotation
        val yaw = rotation.yaw
        val pitch = rotation.pitch

        val velocity = AimPlanner.getHypotheticalArrowVelocity(player) * 3.0

        val vX = -MathHelper.sin(yaw.toRadians()) * MathHelper.cos(pitch.toRadians()) * velocity
        val vY = -MathHelper.sin(pitch.toRadians()) * velocity
        val vZ = MathHelper.cos(yaw.toRadians()) * MathHelper.cos(pitch.toRadians()) * velocity

        val arrow = SimulatedArrow(
            world,
            player.eyes,
            Vec3d(vX.toDouble(), vY.toDouble(), vZ.toDouble()),
            collideEntities = false
        )

        val players = buildSimulatedPlayers(players)

        val boxes = ArrayList<ModuleDebug.DebuggedBox>()

        for (ignored in 0 until maxTicks) {
            val lastPos = arrow.pos

            arrow.tick()

            players.forEach { (entity, player) ->
                player.tick()

                val playerHitBox =
                    Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3)
                        .expand(0.3)
                        .offset(player.pos)

                val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)

                boxes.add(ModuleDebug.DebuggedBox(playerHitBox, Color4b(0, 255, 0, 26)))

                raycastResult.orElse(null)?.let {
                    return entity
                }
            }
            boxes.add(
                ModuleDebug.DebuggedBox(
                    EntityDimensions(0.3F, 0.3F, false).getBoxAt(arrow.pos),
                    Color4b(255, 0, 0, 26)
                )
            )
        }

        ModuleDebug.debugGeometry(ModuleAutoBow, "HypotheticalHit", ModuleDebug.DebugCollection(boxes))

        return null
    }


    fun findPlayersForUnselectiveHypotheticalHit(): List<AbstractClientPlayerEntity> {
        return world.players.filter {
            it != player &&
                Line(player.pos, player.rotationVector).squaredDistanceTo(it.pos) < 10.0 * 10.0
        }
    }

    fun buildSimulatedPlayers(
        players: List<AbstractClientPlayerEntity>
    ): List<Pair<AbstractClientPlayerEntity, SimulatedPlayer>> {
        return players.map {
            Pair(it, SimulatedPlayer.fromOtherPlayer(it, SimulatedPlayer.SimulatedPlayerInput.guessInput(it)))
        }
    }
}
