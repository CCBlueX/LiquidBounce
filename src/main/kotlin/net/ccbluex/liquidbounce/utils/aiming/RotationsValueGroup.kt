/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.utils.aiming

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.features.addon.AddonApi
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import net.ccbluex.liquidbounce.utils.aiming.aim.AiAim
import net.ccbluex.liquidbounce.utils.aiming.aim.Aim
import net.ccbluex.liquidbounce.utils.aiming.aim.ConstantAim
import net.ccbluex.liquidbounce.utils.aiming.aim.HumanAim
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.features.MovementCorrection
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.AABB

/**
 * The rotation settings of a module. [combatSpecific] also offers the AI aim, which aims at living entities.
 */
@AddonApi
open class RotationsValueGroup(
    owner: EventListener,
    movementCorrection: MovementCorrection = MovementCorrection.SILENT,
    combatSpecific: Boolean = false,
) : ValueGroup("Rotations") {

    private val aim = modes<Aim>(owner, "Aim", listOf("AngleSmooth"), { 0 }) {
        val human = HumanAim(it)
        listOfNotNull(ConstantAim(it), human, if (combatSpecific) AiAim(it, human) else null).toTypedArray()
    }

    private val movementCorrection by enumChoice("MovementCorrection", movementCorrection)
    private val ticksUntilReset by int("TicksUntilReset", 5, 1..30, "ticks")

    @UnstableAddonApi
    val aiAim: AiAim?
        get() = aim.modes.firstNotNullOfOrNull { it as? AiAim }

    internal val usesAiAim: Boolean
        get() = aim.activeMode is AiAim

    /**
     * A target on both axes, with these settings.
     */
    @AddonApi
    @JvmOverloads
    @Suppress("LongParameterList")
    fun toRotationTarget(
        rotation: Rotation,
        entity: Entity? = null,
        considerInventory: Boolean = false,
        box: AABB? = null,
        tolerance: Float = 0f,
        whenReady: Readiness<*>? = null,
    ) = toRotationTarget(rotation.yaw, rotation.pitch, entity, considerInventory, box, tolerance, whenReady)

    /**
     * A target with these settings; leave out the axis the module does not care about.
     */
    @AddonApi
    @JvmOverloads
    @Suppress("LongParameterList")
    fun toRotationTarget(
        yaw: Float? = null,
        pitch: Float? = null,
        entity: Entity? = null,
        considerInventory: Boolean = false,
        box: AABB? = null,
        tolerance: Float = 0f,
        whenReady: Readiness<*>? = null,
    ) = RotationTarget(
        yaw,
        pitch,
        entity,
        aim.activeMode,
        ticksUntilReset,
        considerInventory,
        movementCorrection,
        box,
        tolerance,
        whenReady,
    )

    /**
     * Ticks it takes at least to turn from the rotation the server has to [rotation].
     */
    fun calculateTicks(rotation: Rotation) = aim.activeMode.ticksToReach(RotationManager.actualServerRotation, rotation)

}
