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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.minecraft.entity.EntityAttachmentType
import net.minecraft.entity.EntityAttachments
import net.minecraft.entity.EntityDimensions
import net.minecraft.entity.EntityPose
import net.minecraft.entity.player.PlayerEntity

/**
 * Prevents pose changes for low version of server protocol
 *
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinEntity
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinPlayerEntity
 */
object ModuleNoPose : ClientModule("NoPose", Category.MOVEMENT, aliases = listOf("NoSwim", "NoCrawl")) {

    val noSwim by boolean("NoSwim", false)
    val sneakHeightChoice by enumChoice("SneakHeight", SneakHeights.ONEFIFTEEN)

    fun shouldCancelPose(pose: EntityPose): Boolean {
        if (!running) return false
        /* If the entity in question is mc.player, only cancel EntityPose.SWIMMING */
        return pose == EntityPose.SWIMMING && noSwim
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
    return EntityDimensions.changing(width, height)
        .withEyeHeight(eyeHeight)
        .withAttachments(EntityAttachments.builder().add(
            EntityAttachmentType.VEHICLE,
            PlayerEntity.VEHICLE_ATTACHMENT_POS))
}

@Suppress("unused") /* Used as settings */
enum class SneakHeights(override val choiceName: String, val dimensions: EntityDimensions): NamedChoice {
    ONEEIGHT("1.8", getDimensions(0.6f, 1.8f, 1.54f)),
    ONENINE("1.9", getDimensions(0.6f, 1.65f, 1.54f)),
    ONEFIFTEEN("1.15", getDimensions(0.6f, 1.5f, 1.27f));
}
