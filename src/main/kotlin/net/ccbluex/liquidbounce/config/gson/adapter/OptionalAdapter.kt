package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.Gson
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.lang.reflect.ParameterizedType
import java.util.*

class OptionalAdapter<E>(private val adapter: TypeAdapter<E>) : TypeAdapter<Optional<E>?>() {
    override fun write(out: JsonWriter?, value: Optional<E>?) {
        if (value!!.isPresent) {
            adapter.write(out, value.get())
        } else {
            out!!.nullValue()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun read(`in`: JsonReader): Optional<E> {
        val peek = `in`.peek()
        if (peek != JsonToken.NULL) {
            return Optional.ofNullable(adapter.read(`in`)) as Optional<E>
        }

        `in`.nextNull()
        return Optional.empty<E>() as Optional<E>
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        val FACTORY: TypeAdapterFactory = object : TypeAdapterFactory {
            override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
                val rawType = type.getRawType() as Class<T>?
                if (rawType != Optional::class.java) {
                    return null
                }

                val parameterizedType = type.type as? ParameterizedType
                    ?: return null

                val actualType = parameterizedType.actualTypeArguments[0]
                val adapter = gson.getAdapter(TypeToken.get(actualType))
                return OptionalAdapter(adapter) as TypeAdapter<T>?
            }
        }
    }
}
