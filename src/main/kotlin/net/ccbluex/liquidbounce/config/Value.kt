/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.config

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName
import net.ccbluex.liquidbounce.config.util.Exclude
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.ValueChangedEvent
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.render.Fonts
import java.util.*
import kotlin.reflect.KProperty

typealias ValueListener<T> = (T) -> T

/**
 * Value based on generics and support for readable names and description
 */
open class Value<T : Any>(
    @SerializedName("name")
    open val name: String,
    @SerializedName("value")
    internal var value: T,
    @Exclude
    internal val listType: ListValueType = ListValueType.None,
) {

    @Exclude
    private val type: String = value.javaClass.typeName

    @Exclude
    private val listeners = mutableListOf<ValueListener<T>>()

    /**
     * Support for delegated properties
     * example:
     *  var autoaim by boolean(name = "autoaim", default = true)
     *  if(!autoaim)
     *    autoaim = true
     *
     * Important: To use values a class has to be configurable
     *
     * @docs https://kotlinlang.org/docs/reference/delegated-properties.html
     */

    operator fun getValue(u: Any?, property: KProperty<*>) = value

    operator fun setValue(u: Any?, property: KProperty<*>, t: T) {
        // temporary set value
        value = t

        // check if value is really accepted
        var currT = t
        runCatching {
            listeners.forEach {
                currT = it(t)
            }
        }.onSuccess {
            value = currT
            EventManager.callEvent(ValueChangedEvent(this))
        }
    }

    fun listen(listener: ValueListener<T>): Value<T> {
        listeners += listener
        return this
    }

    /**
     * Deserialize value from JSON
     */
    open fun deserializeFrom(gson: Gson, element: JsonElement) {
        val currValue = this.value

        this.value = if (currValue is List<*>) {
            @Suppress("UNCHECKED_CAST")
            element.asJsonArray.mapTo(mutableListOf(), { gson.fromJson(it, this.listType.type!!) }) as T
        } else if (currValue is Set<*>) {
            @Suppress("UNCHECKED_CAST")
            element.asJsonArray.mapTo(TreeSet(), { gson.fromJson(it, this.listType.type!!) }) as T
        } else {
            gson.fromJson(element, currValue.javaClass)
        }
    }

}

/**
 * Ranged value adds support for closed ranges
 */
class RangedValue<T : Any>(
    name: String,
    value: T,
    @Exclude
    val range: ClosedRange<*>
) : Value<T>(name, value)

class ChooseListValue<T : NamedChoice>(
    name: String,
    selected: T,
    @Exclude
    val choices: Array<T>
) : Value<T>(name, selected) {

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val name = element.asString

        this.value = choices.first { it.choiceName == name }
    }

}

interface NamedChoice {
    val choiceName: String
}

enum class ListValueType(val type: Class<*>?) {
    Block(net.minecraft.block.Block::class.java),
    Item(net.minecraft.item.Item::class.java),
    String(kotlin.String::class.java),
    Friend(FriendManager.Friend::class.java),
    FontDetail(Fonts.FontDetail::class.java),
    None(null)
}
