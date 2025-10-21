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
package net.ccbluex.liquidbounce.features.module.modules.render.trajectories

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.events.WorldRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.entity.interpolateCurrentPosition
import net.ccbluex.liquidbounce.utils.math.toFixed
import net.ccbluex.liquidbounce.utils.render.WorldToScreen
import net.ccbluex.liquidbounce.utils.render.trajectory.*
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.Ownable
import net.minecraft.util.math.Vec3d
import java.text.DecimalFormat
import java.util.function.BiFunction
import java.util.function.IntFunction

/**
 * Trajectories module
 *
 * Allows you to see where projectile items will land.
 */
@Suppress("MagicNumber")
object ModuleTrajectories : ClientModule("Trajectories", Category.RENDER) {
    val maxSimulatedTicks by int("MaxSimulatedTicks", 120, 1..320, "ticks")
    private val show by multiEnumChoice(
        "Show",
        Show.OTHER_PLAYERS,
        Show.ACTIVE_HOLDING,
        Show.ACTIVE_THROWN_INFO,
        Show.ACTIVE_HOLDING,
        Show.ACTIVE_THROWN_INFO
    )

    val alwaysShowBow get() = Show.ALWAYS_SHOW_BOW in show

    private val otherPlayers get() = Show.OTHER_PLAYERS in show
    private val activeThrown get() = Show.ACTIVE_THROWN in show
    private val activeHolding get() = Show.ACTIVE_HOLDING in show
    private val activeThrownInfo get() = Show.ACTIVE_THROWN_INFO in show
    private val activeHoldingInfo get() = Show.ACTIVE_HOLDING_INFO in show

    sealed class ProjectileType(name: String, defaultColor: Color4b, enabled: Boolean) : ToggleableConfigurable(
        this, name, enabled = enabled
    ) {
        val color by color("Color", defaultColor)
        val blockHitESP by boolean("BlockHitESP", true)
        val entityHitESP by boolean("EntityHitESP", true)
        val showDetailedInfo by boolean("ShowDetailedInfo", false)

        object Arrow : ProjectileType(
            "Arrow",
            Color4b(255, 0, 0, 100), true
        )

        object Potion : ProjectileType(
            "Potion",
            Color4b(255, 192, 203, 100), true
        )

        object EnderPearl : ProjectileType(
            "EnderPearl",
            Color4b(255, 0, 255, 100), true
        )

        object FishingBobber : ProjectileType(
            "FishingBobber",
            Color4b(64, 64, 64, 100), true
        )

        object Trident : ProjectileType(
            "Trident",
            Color4b(0, 255, 255, 100), true
        )

        object Snowball : ProjectileType(
            "Snowball",
            Color4b(255, 255, 255, 100), true
        )

        object Egg : ProjectileType(
            "Egg",
            Color4b(255, 255, 255, 100), true
        )

        object ExpBottle : ProjectileType(
            "ExpBottle",
            Color4b(0, 255, 0, 100), false
        )

        object Fireball : ProjectileType(
            "Fireball",
            Color4b(255, 165, 0, 100), true
        )

        object WindCharge : ProjectileType(
            "WindCharge",
            Color4b(192, 192, 192, 100), true
        )
    }

    private object ShowDetailedInfo : Configurable("ShowDetailedInfo") {
        private val ownerName by boolean("OwnerName", false)
        private val distance by boolean("Distance", false)
        private val showAt by enumChoice("ShowAt", ShowAt.ENTITY)
        private val durationUnit by enumChoice("DurationUnit", DurationUnit.TICKS)
        private val TICK_FORMATTER = DecimalFormat("0.#s")
        private val scale by float("Scale", 1F, 0.25F..4F)
        private val renderOffset by vec3d("RenderOffset", Vec3d.ZERO)

        private enum class ShowAt(
            override val choiceName: String,
        ) : NamedChoice, BiFunction<TrajectoryInfoRenderer, TrajectoryInfoRenderer.SimulationResult, Vec3d> {
            OWNER("Owner"),
            ENTITY("Entity"),
            LANDING("Landing");

            override fun apply(
                renderer: TrajectoryInfoRenderer,
                result: TrajectoryInfoRenderer.SimulationResult,
            ): Vec3d = when (this) {
                OWNER -> renderer.owner.interpolateCurrentPosition(
                    mc.renderTickCounter.getTickDelta(true)
                )

                ENTITY -> result.positions.firstOrNull()
                LANDING -> result.positions.lastOrNull()
            } ?: renderer.owner.pos
        }

        private enum class DurationUnit(
            override val choiceName: String,
        ) : NamedChoice, IntFunction<String> {
            TICKS("Ticks"),
            SECONDS("Seconds");

            override fun apply(ticks: Int): String = when (this) {
                TICKS -> ticks.toString()
                SECONDS -> TICK_FORMATTER.format(ticks * 0.05)
            }
        }

        @Suppress("unused")
        private val overlayRenderHandler = handler<OverlayRenderEvent> { event ->
            fun Vec3d.calcScreenPosWithOffset(): Vec3? {
                return WorldToScreen.calculateScreenPos(add(renderOffset))
            }

            val context = event.context
            val fontHeight = mc.textRenderer.fontHeight + 1
            val centerX = mc.window.scaledWidth * 0.5F
            val centerY = mc.window.scaledHeight * 0.5F
            val centerOffset = Vec3(centerX + 50F, centerY, 0F)

            val filteredResults = simulationResults.filter { (renderer, _) ->
                when (renderer.type) {
                    TrajectoryInfoRenderer.Type.REAL -> activeThrownInfo
                    TrajectoryInfoRenderer.Type.HYPOTHETICAL -> activeHoldingInfo
                }.takeIf { it }?.let {
                    renderer.trajectoryInfo.categorize()?.showDetailedInfo == true
                } ?: false
            }

            filteredResults.forEachIndexed { index, (renderer, result) ->
                val screenPos = when {
                    (showAt === ShowAt.OWNER || (showAt === ShowAt.ENTITY && renderer.type ===
                        TrajectoryInfoRenderer.Type.HYPOTHETICAL)) && renderer.owner ===
                        player && renderer.type === TrajectoryInfoRenderer.Type.HYPOTHETICAL -> {
                        centerOffset.add(Vec3(0.0F, (index * fontHeight).toFloat(), 0.0F))
                    }

                    showAt === ShowAt.OWNER && renderer.owner === player -> {
                        centerOffset.add(Vec3(0.0F, (index * fontHeight).toFloat(), 0.0F))
                    }

                    else -> {
                        var position = showAt.apply(renderer, result)
                        if (showAt === ShowAt.LANDING && renderer.type ===
                            TrajectoryInfoRenderer.Type.HYPOTHETICAL && renderer.owner === player) {
                            val partialTicks = mc.renderTickCounter.getTickDelta(true)
                            val playerPrevPos = Vec3d(player.prevX, player.prevY, player.prevZ)
                            val playerInterpolated = player.interpolateCurrentPosition(partialTicks)
                            val posOffset = playerInterpolated.subtract(playerPrevPos)
                            position = position.add(posOffset)
                        }
                        position.calcScreenPosWithOffset()
                    }
                } ?: return@forEachIndexed

                context.matrices.push()
                context.matrices.translate(screenPos.x, screenPos.y, screenPos.z)
                context.matrices.scale(scale, scale, 1.0F)

                val text = durationUnit.apply(result.positions.size).asText()
                if (ownerName && renderer.owner !== player) {
                    text.append(" ").append(renderer.owner.name)
                }
                if (distance && result.positions.isNotEmpty()) {
                    val useLast = when {
                        showAt == ShowAt.LANDING -> true
                        renderer.type == TrajectoryInfoRenderer.Type.HYPOTHETICAL -> true
                        else -> false
                    }
                    val dist = player.pos.distanceTo(
                        if (useLast) result.positions.last() else result.positions.first()
                    )
                    text.append(" ${dist.toFixed(1)}m")
                }


                var y = 0

                context.drawCenteredTextWithShadow(
                    mc.textRenderer,
                    text,
                    0,
                    y,
                    Color4b.WHITE.toARGB(),
                )
                y += mc.textRenderer.fontHeight + 1

                context.matrices.pop()
            }
        }
    }

    init {
        tree(ProjectileType.Arrow)
        tree(ProjectileType.Potion)
        tree(ProjectileType.EnderPearl)
        tree(ProjectileType.FishingBobber)
        tree(ProjectileType.Trident)
        tree(ProjectileType.Snowball)
        tree(ProjectileType.Egg)
        tree(ProjectileType.ExpBottle)
        tree(ProjectileType.Fireball)
        tree(ProjectileType.WindCharge)
        tree(ShowDetailedInfo)
    }

    val simulationResults = mutableListOf<Pair
    <TrajectoryInfoRenderer, TrajectoryInfoRenderer.SimulationResult>>()

    override fun onDisabled() {
        simulationResults.clear()
    }

    val renderHandler = handler<WorldRenderEvent> { event ->
        simulationResults.clear()

        if (activeThrown || activeThrownInfo) {
            world.entities.forEach { entity ->
                val trajectoryInfo = TrajectoryData.getRenderTrajectoryInfoForOtherEntity(entity) ?: return@forEach

                val type = entity.categorize() ?: return@forEach
                if (!type.enabled) return@forEach

                val pos = entity.interpolateCurrentPosition(event.partialTicks)

                val renderer = TrajectoryInfoRenderer(
                    owner = (entity as? Ownable)?.owner ?: entity,
                    velocity = entity.velocity,
                    pos = pos,
                    trajectoryInfo = trajectoryInfo,
                    type = TrajectoryInfoRenderer.Type.REAL,
                    renderOffset = Vec3d.ZERO
                )

                val simulationResult = renderer.runSimulation(maxSimulatedTicks)
                simulationResults += renderer to simulationResult

                if (activeThrown) {
                    renderSimulationResult(
                        renderer, simulationResult, type, trajectoryInfo, event
                    )
                }
            }
        }

        if (activeHolding || activeHoldingInfo) {
            if (otherPlayers) {
                world.players.forEach { drawHypotheticalTrajectoryWithSimulation(it, event, activeHolding) }
            } else {
                drawHypotheticalTrajectoryWithSimulation(player, event, activeHolding)
            }
        }
    }


    private enum class Show(override val choiceName: String) : NamedChoice {
        ALWAYS_SHOW_BOW("AlwaysShowBow"),
        OTHER_PLAYERS("OtherPlayers"),
        ACTIVE_THROWN("Thrown"),
        ACTIVE_HOLDING("Holding"),
        ACTIVE_THROWN_INFO("ThrownInfo"),
        ACTIVE_HOLDING_INFO("HoldingInfo"),
    }

    // ---------------- Categorize ----------------
    @JvmStatic
    fun Entity.categorize(): ProjectileType? = when (type) {
        EntityType.ARROW -> ProjectileType.Arrow
        EntityType.POTION -> ProjectileType.Potion
        EntityType.ENDER_PEARL -> ProjectileType.EnderPearl
        EntityType.FISHING_BOBBER -> ProjectileType.FishingBobber
        EntityType.TRIDENT -> ProjectileType.Trident
        EntityType.SNOWBALL -> ProjectileType.Snowball
        EntityType.EGG -> ProjectileType.Egg
        EntityType.EXPERIENCE_BOTTLE -> ProjectileType.ExpBottle
        EntityType.FIREBALL -> ProjectileType.Fireball
        EntityType.WIND_CHARGE -> ProjectileType.WindCharge
        else -> null
    }

}
