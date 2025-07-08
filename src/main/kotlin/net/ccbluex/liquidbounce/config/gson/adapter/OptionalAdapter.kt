package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.util.*

class OptionalAdapter<T>(
    private val valueAdapter: TypeAdapter<T>
) : TypeAdapter<Optional<T>>() {
    override fun write(out: JsonWriter, value: Optional<T>?) {
        if (value == null || !value.isPresent) {
            out.nullValue();
        } else {
            valueAdapter.write(out, value.get())
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun read(`in`: JsonReader): Optional<T> {
        val value: T? = valueAdapter.read(`in`)
        return Optional.ofNullable(value) as Optional<T>
    }
}
