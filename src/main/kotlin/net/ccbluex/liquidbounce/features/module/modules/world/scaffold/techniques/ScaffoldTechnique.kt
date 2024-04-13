/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.minecraft.entity.EntityPose
import net.minecraft.item.ItemStack
import net.minecraft.util.math.Vec3d

abstract class ScaffoldTechnique(name: String) : Choice(name) {

    override val parent: ChoiceConfigurable<ScaffoldTechnique>
        get() = ModuleScaffold.technique


    abstract fun findPlacementTarget(
        predictedPos: Vec3d,
        predictedPose: EntityPose,
        optimalLine: Line?,
        bestStack: ItemStack
    ): BlockPlacementTarget?

    open fun getRotations(target: BlockPlacementTarget?) = target?.rotation

}
