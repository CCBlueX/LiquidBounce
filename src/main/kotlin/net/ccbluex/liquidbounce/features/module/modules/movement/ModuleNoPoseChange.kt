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
import net.ccbluex.liquidbounce.utils.kotlin.emptyEnumSet
import net.minecraft.entity.EntityPose
import java.util.EnumMap

/**
 * Prevents pose changes
 *
 * @see net.ccbluex.liquidbounce.injection.mixins.minecraft.entity.MixinEntity
 */
object ModuleNoPoseChange : ClientModule("NoPoseChange", Category.MOVEMENT, aliases = arrayOf("NoSwim")) {
    private val disabled by multiEnumChoice("Disabled", emptyEnumSet<EntityPoseChoice>())

    private val poseToChoice = EnumMap<_, EntityPoseChoice>(EntityPose::class.java).apply {
        EntityPoseChoice.entries.forEach { put(it.gameEntityPose, it) }
    }

    fun EntityPose.shouldCancel() = running && poseToChoice[this] in disabled

    private enum class EntityPoseChoice(override val choiceName: String, val gameEntityPose: EntityPose) : NamedChoice {
        STANDING("Standing", EntityPose.STANDING),
        GLIDING("Gliding", EntityPose.GLIDING),
        SLEEPING("Sleeping", EntityPose.SLEEPING),
        SWIMMING("Swimming", EntityPose.SWIMMING),
        SPIN_ATTACK("SpinAttack", EntityPose.SPIN_ATTACK),
        CROUCHING("Crouching", EntityPose.CROUCHING),
        LONG_JUMPING("LongJumping", EntityPose.LONG_JUMPING),
        DYING("Dying", EntityPose.DYING),
        CROAKING("Croaking", EntityPose.CROAKING),
        USING_TONGUE("UsingTongue", EntityPose.USING_TONGUE),
        SITTING("Sitting", EntityPose.SITTING),
        ROARING("Roaring", EntityPose.ROARING),
        SNIFFING("Sniffing", EntityPose.SNIFFING),
        EMERGING("Emerging", EntityPose.EMERGING),
        DIGGING("Digging", EntityPose.DIGGING),
        SLIDING("Sliding", EntityPose.SLIDING),
        SHOOTING("Shooting", EntityPose.SHOOTING),
        INHALING("Inhaling", EntityPose.INHALING)
    }
}
