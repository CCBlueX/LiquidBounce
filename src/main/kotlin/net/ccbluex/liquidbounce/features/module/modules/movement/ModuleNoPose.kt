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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinEntity
import net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinPlayer
import net.minecraft.world.entity.EntityAttachment
import net.minecraft.world.entity.EntityAttachments
import net.minecraft.world.entity.EntityDimensions
import net.minecraft.world.entity.Pose
import net.minecraft.world.entity.player.Player

/**
 * Prevents pose changes for low version of server protocol
 *
 * @see MixinEntity
 * @see MixinPlayer
 */
object ModuleNoPose : ClientModule("NoPose", ModuleCategories.MOVEMENT, aliases = listOf("NoSwim", "NoCrawl")) {

    val noSwim by boolean("NoSwim", false)
    val sneakHeightChoice by enumChoice("SneakHeight", SneakHeights.ONEFIFTEEN)

    fun shouldCancelPose(pose: Pose): Boolean {
        if (!running) return false
        /* If the entity in question is mc.player, only cancel EntityPose.SWIMMING */
        return pose == Pose.SWIMMING && noSwim
    }

    /**
     * Returns an instance of `EntityDimensions` if this module is enabled
     * and the sneak setting is modified
     * @return `@Nullable EntityDimensions`
     */
    fun modifySneakHeight(): EntityDimensions? {
        if (!running) return null

        val modified = sneakHeightChoice.dimensions
        return modified
    }
}

/**
 * Helper method to add vehicle attachment
 */
private fun getDimensions(width: Float, height: Float, eyeHeight: Float): EntityDimensions {
    return EntityDimensions.scalable(width, height)
        .withEyeHeight(eyeHeight)
        .withAttachments(
            EntityAttachments.builder().attach(
            EntityAttachment.VEHICLE,
            Player.DEFAULT_VEHICLE_ATTACHMENT
            ))
}

@Suppress("unused") /* Used as settings */
enum class SneakHeights(override val tag: String, val dimensions: EntityDimensions): Tagged {
    ONEEIGHT("1.8", getDimensions(0.6f, 1.8f, 1.54f)),
    ONENINE("1.9", getDimensions(0.6f, 1.65f, 1.54f)),
    ONEFIFTEEN("1.15", getDimensions(0.6f, 1.5f, 1.27f));
}
