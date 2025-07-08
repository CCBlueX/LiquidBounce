package net.ccbluex.liquidbounce.config.gson.util

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import net.ccbluex.liquidbounce.config.gson.adapter.OptionalAdapter
import java.lang.reflect.ParameterizedType
import java.util.*

object OptionalTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (!Optional::class.java.isAssignableFrom(rawType)) {
            return null
        }

        val parameterizedType = type.type as? ParameterizedType
            ?: return null

        val actualType = parameterizedType.actualTypeArguments[0]
        val valueAdapter = gson.getAdapter(TypeToken.get(actualType))

        @Suppress("UNCHECKED_CAST")
        return OptionalAdapter(valueAdapter) as TypeAdapter<T>
    }
}
