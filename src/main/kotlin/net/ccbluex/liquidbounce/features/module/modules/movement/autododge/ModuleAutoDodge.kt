/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
@file:Suppress("WildcardImport")
package net.ccbluex.liquidbounce.features.module.modules.movement.autododge

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.once
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleBlink
import net.ccbluex.liquidbounce.features.module.modules.render.murdermystery.ModuleMurderMystery
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.*
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.world.ClientWorld
import net.minecraft.entity.Entity
import net.minecraft.entity.projectile.ArrowEntity
import net.minecraft.entity.projectile.SpectralArrowEntity
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d

@Suppress("MagicNumber")
object ModuleAutoDodge : ClientModule("AutoDodge", Category.COMBAT) {
    private object AllowRotationChange : ToggleableConfigurable(this, "AllowRotationChange", false) {
        val allowJump by boolean("AllowJump", true)
    }

    private object AllowTimer : ToggleableConfigurable(this, "AllowTimer", false) {
        val timerSpeed by float("TimerSpeed", 2.0F, 1.0F..10.0F, suffix = "x")
    }

    private val ignore by multiEnumChoice("Ignore", Ignore.entries)

    init {
        tree(AllowRotationChange)
        tree(AllowTimer)
    }

    override val running: Boolean get() =
        super.running
           // We aren't where we are because of blink. So this module shall not cause any disturbance in that case.
        && !ModuleBlink.running
        && !ModuleMurderMystery.disallowsArrowDodge()
        && !(Ignore.OPEN_INVENTORY !in ignore
            && (InventoryManager.isInventoryOpen || mc.currentScreen is GenericContainerScreen))
        && !(Ignore.USING_ITEM !in ignore && player.isUsingItem)
        && !(Ignore.USING_SCAFFOLD !in ignore && ModuleScaffold.running)

    @Suppress("unused")
    val tickRep = handler<MovementInputEvent> { event ->
        val arrows = world.findFlyingArrows()

        val simulatedPlayer = CachedPlayerSimulation(PlayerSimulationCache.getSimulationForLocalPlayer())

        val inflictedHit =
            getInflictedHits(simulatedPlayer, arrows, hitboxExpansion = DodgePlanner.SAFE_DISTANCE_WITH_PADDING)
                ?: return@handler

        val dodgePlan =
            planEvasion(DodgePlannerConfig(allowRotations = AllowRotationChange.enabled), inflictedHit)
                ?: return@handler

        event.directionalInput = dodgePlan.directionalInput

        dodgePlan.yawChange?.let { yawChange ->
            player.yaw = yawChange
        }

        if (dodgePlan.shouldJump && AllowRotationChange.allowJump && player.isOnGround) {
            once<MovementInputEvent> { movementInputEvent ->
                movementInputEvent.jump = true
            }
        }

        if (AllowTimer.enabled && dodgePlan.useTimer) {
            Timer.requestTimerSpeed(AllowTimer.timerSpeed, Priority.IMPORTANT_FOR_PLAYER_LIFE, this@ModuleAutoDodge)
        }
    }

    private fun ClientWorld.findFlyingArrows() = entities.filter { entity ->
        (entity is ArrowEntity || entity is SpectralArrowEntity) && !entity.isInGround
    }

    private fun <T : PlayerSimulation> getInflictedHits(
        simulatedPlayer: T,
        arrows: List<Entity>,
        maxTicks: Int = 80,
        hitboxExpansion: Double = 0.7,
    ): HitInfo? {
        val simulatedArrows = arrows.map { SimulatedArrow(world, it.pos, it.velocity, false) }

        for (i in 0 until maxTicks) {
            simulatedPlayer.tick()

            simulatedArrows.forEachIndexed { arrowIndex, arrow ->
                if (arrow.inGround) {
                    return@forEachIndexed
                }

                val lastPos = arrow.pos

                arrow.tick()

                val playerHitBox =
                    Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3)
                        .expand(hitboxExpansion)
                        .offset(simulatedPlayer.pos)
                val raycastResult = playerHitBox.raycast(lastPos, arrow.pos)

                raycastResult.orElse(null)?.let { hitPos ->
                    return HitInfo(i, arrows[arrowIndex], hitPos, lastPos, arrow.velocity)
                }
            }
        }

        return null
    }

    data class EvadingPacket(
        val idx: Int,
        /**
         * Ticks until impact. Null if evaded
         */
        val ticksToImpact: Int?
    )

    /**
     * Returns the index of the first position packet that avoids all arrows in the next X seconds
     */
    @Suppress("ReturnCount")
    fun findAvoidingArrowPosition(): EvadingPacket? {
        var packetIndex = 0

        var lastPosition: Vec3d? = null

        var bestPacketPosition: Vec3d? = null
        var bestPacketIdx: Int? = null
        var bestTimeToImpact = 0

        for (position in PacketQueueManager.positions) {
            packetIndex += 1

            // Process packets only if they are at least some distance away from each other
            if (lastPosition != null) {
                if (lastPosition.squaredDistanceTo(position) < 0.9 * 0.9) {
                    continue
                }
            }

            lastPosition = position

            val inflictedHit = getInflictedHit(position)

            if (inflictedHit == null) {
                return EvadingPacket(packetIndex - 1, null)
            } else if (inflictedHit.tickDelta > bestTimeToImpact) {
                bestTimeToImpact = inflictedHit.tickDelta
                bestPacketIdx = packetIndex - 1
                bestPacketPosition = position
            }
        }

        // If the evading packet is less than one player hitbox away from the current position, we should rather
        // call the evasion a failure
        if (bestPacketIdx != null && bestPacketPosition!!.squaredDistanceTo(lastPosition!!) > 0.9) {
            return EvadingPacket(bestPacketIdx, bestTimeToImpact)
        }

        return null
    }

    fun getInflictedHit(pos: Vec3d): HitInfo? {
        val arrows = world.findFlyingArrows()
        val playerSimulation = RigidPlayerSimulation(pos)

        return getInflictedHits(playerSimulation, arrows, maxTicks = 40)
    }

    data class HitInfo(
        val tickDelta: Int,
        val arrowEntity: Entity,
        val hitPos: Vec3d,
        val prevArrowPos: Vec3d,
        val arrowVelocity: Vec3d,
    )

    private enum class Ignore(
        override val choiceName: String
    ) : NamedChoice {
        OPEN_INVENTORY("OpenInventory"),
        USING_ITEM("UsingItem"),
        USING_SCAFFOLD("UsingScaffold")
    }
}
