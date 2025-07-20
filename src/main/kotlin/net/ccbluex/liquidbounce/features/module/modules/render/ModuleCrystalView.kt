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

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinEndCrystalEntityModel
import net.ccbluex.liquidbounce.injection.mixins.minecraft.render.MixinEndCrystalEntityRenderer

/**
 * Module CrystalView
 *
 * Tweaks how crystal models behave.
 *
 * Mixins: [MixinEndCrystalEntityModel], [MixinEndCrystalEntityRenderer]
 *
 * @author ccetl
 */
object ModuleCrystalView : ClientModule("CrystalView", Category.RENDER) {

    val size by float("Size", 0.3f, 0.1f..1.5f)
    val yTranslate by float("YTranslate", -0.5f, -2f..2f)
    val spinSpeed by float("SpinSpeed", 0f, 0f..5f)
    val bounce by float("Bounce", 0.25f, -1f..1f)

}
