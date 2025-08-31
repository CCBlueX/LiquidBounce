package net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.autobow

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.KeybindIsPressedEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.aimbot.ModuleAutoBow
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.client.toRadians
import net.ccbluex.liquidbounce.utils.combat.shouldBeAttacked
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.entity.SimulatedArrow
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayerCache
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.render.trajectory.TrajectoryInfo
import net.minecraft.client.network.AbstractClientPlayerEntity
import net.minecraft.item.BowItem
import net.minecraft.item.TridentItem
import net.minecraft.util.math.Box
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d

object AutoBowAutoShootFeature : ToggleableConfigurable(ModuleAutoBow, "AutoShoot", true) {

    val charged by int("Charged", 15, 3..20, suffix = "ticks")

    val chargedRandom by floatRange(
        "ChargedRandom",
        0.0F..0.0F,
        -10.0F..10.0F,
        suffix = "ticks"
    )
    val delayBetweenShots by float("DelayBetweenShots", 0.0F, 0.0F..5.0F, suffix = "s")
    val aimThreshold by float("AimThreshold", 1.5F, 1.0F..4.0F, suffix = "°")
    val requiresHypotheticalHit by boolean("RequiresHypotheticalHit", false)

    var currentChargeRandom: Int? = null

    fun updateChargeRandom() {
        val lenHalf = (chargedRandom.endInclusive - chargedRandom.start) / 2.0F
        val mid = chargedRandom.start + lenHalf

        currentChargeRandom =
            (mid + ModuleAutoBow.random.nextGaussian() * lenHalf).toInt()
                .coerceIn(chargedRandom.start.toInt()..chargedRandom.endInclusive.toInt())
    }

    fun getChargedRandom(): Int {
        if (currentChargeRandom == null) {
            updateChargeRandom()
        }

        return currentChargeRandom!!
    }

    private var forceUncharged = false

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        forceUncharged = false

        val currentItem = player.activeItem?.item

        // Should check if player is using bow
        if (currentItem !is BowItem && currentItem !is TridentItem) {
            return@handler
        }

        if (player.itemUseTime < charged + getChargedRandom()) { // Wait until the bow is fully charged
            return@handler
        }

        if (!ModuleAutoBow.lastShotTimer.hasElapsed((delayBetweenShots * 1000.0F).toLong())) {
            return@handler
        }

        if (requiresHypotheticalHit) {
            val hypotheticalHit = getHypotheticalHit()

            if (hypotheticalHit == null || !hypotheticalHit.shouldBeAttacked()) {
                return@handler
            }
        } else if (AutoBowAimbotFeature.enabled) {
            if (AutoBowAimbotFeature.targetTracker.target == null) {
                return@handler
            }

            val targetRotation = RotationManager.activeRotationTarget ?: return@handler

            val aimDifference = RotationManager.serverRotation.angleTo(targetRotation.rotation)

            if (aimDifference > aimThreshold) {
                return@handler
            }
        }

        forceUncharged = true
        updateChargeRandom()
    }

    @Suppress("unused")
    private val keybindHandler = handler<KeybindIsPressedEvent> { event ->
        if (event.keyBinding == mc.options.useKey && forceUncharged) {
            event.isPressed = false
        }
    }

    fun getHypotheticalHit(): AbstractClientPlayerEntity? {
        val rotation = RotationManager.serverRotation
        val yaw = rotation.yaw
        val pitch = rotation.pitch

        val velocity = (TrajectoryInfo.bowWithUsageDuration() ?: return null).initialVelocity

        val vX = -MathHelper.sin(yaw.toRadians()) * MathHelper.cos(pitch.toRadians()) * velocity
        val vY = -MathHelper.sin(pitch.toRadians()) * velocity
        val vZ = MathHelper.cos(yaw.toRadians()) * MathHelper.cos(pitch.toRadians()) * velocity

        val arrow = SimulatedArrow(
            world,
            player.eyePos,
            Vec3d(vX, vY, vZ),
            collideEntities = false
        )

        val players = findAndBuildSimulatedPlayers()

        for (i in 0 until 40) {
            val lastPos = arrow.pos

            arrow.tick()

            players.forEach { (entity, player) ->
                val playerSnapshot = player.getSnapshotAt(i)

                val playerHitBox =
                    Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3)
                        .expand(0.3)
                        .offset(playerSnapshot.pos)

                val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)

                raycastResult.orElse(null)?.let {
                    return entity
                }
            }
        }

        return null
    }

    private fun findAndBuildSimulatedPlayers(): List<Pair<AbstractClientPlayerEntity, SimulatedPlayerCache>> {
        return world.players.filter {
            it != player &&
                Line(player.pos, player.rotationVector).squaredDistanceTo(it.pos) < 10.0 * 10.0
        }.map {
            Pair(it, PlayerSimulationCache.getSimulationForOtherPlayers(it))
        }
    }

    override fun onDisabled() {
        forceUncharged = false
        super.onDisabled()
    }

}
