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
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.render.esp.ModuleESP
import net.ccbluex.liquidbounce.interfaces.EntityRenderStateAddition
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.render.entity.state.LivingEntityRenderState
import net.minecraft.entity.LivingEntity

/**
 * TrueSight module
 *
 * Allows you to see invisible objects and entities.
 */
@Suppress("MagicNumber")
object ModuleTrueSight : ClientModule("TrueSight", Category.RENDER) {
    private val sight by multiEnumChoice("Sight", Sight.entries)

    val barriers get() = Sight.BARRIERS in sight
    val entities get() = Sight.ENTITIES in sight

    val entityColor by color("EntityColor", Color4b(255, 255, 255, 100))
    val entityFeatureLayerColor by color("EntityFeatureLayerColor", Color4b(255, 255, 255, 120))

    @JvmStatic
    @Suppress("ComplexCondition")
    fun canRenderEntities(state: LivingEntityRenderState): Boolean {
        val enabled = this.running && entities;

        val entity = (state as EntityRenderStateAddition).`liquid_bounce$getEntity`()
        val livingEntity = entity as? LivingEntity

        return ((enabled
                || livingEntity != null
                && ModuleESP.running
                && ModuleESP.requiresTrueSight(livingEntity))
                && entity.isInvisible)
    }

    private enum class Sight(
        override val choiceName: String
    ) : NamedChoice {
        BARRIERS("Barriers"),
        ENTITIES("Entities")
    }
}
