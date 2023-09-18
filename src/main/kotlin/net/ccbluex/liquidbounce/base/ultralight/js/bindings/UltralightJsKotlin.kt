/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.base.ultralight.js.bindings
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.utils.client.chat
/**
 * An easy way to create kotlin things in JS
 */
object UltralightJsKotlin {

    fun intRange(from: Int, to: Int) = from..to

    fun floatRange(from: Float, to: Float) = from..to

    fun color(r:Int, g: Int, b: Int, a: Int): Color4b{
        chat(Color4b(r, g, b, 255).toString())
        return Color4b(r, g, b, 255)

    }

    fun log(message: String){
        chat(message)
    }

}
