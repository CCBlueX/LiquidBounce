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

package net.ccbluex.liquidbounce.render.engine.font

import org.intellij.lang.annotations.MagicConstant
import java.awt.Font

/**
 * Marks an integer as a valid font style mask.
 *
 * Valid values are [Font.PLAIN], [Font.BOLD], [Font.ITALIC], and
 * `Font.BOLD | Font.ITALIC`.
 */
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.FIELD,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.TYPE,
)
@MagicConstant(flags = [Font.PLAIN.toLong(), Font.BOLD.toLong(), Font.ITALIC.toLong()])
annotation class FontStyle
