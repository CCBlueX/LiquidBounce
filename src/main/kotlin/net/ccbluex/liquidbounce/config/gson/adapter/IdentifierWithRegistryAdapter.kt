package net.ccbluex.liquidbounce.config.gson.adapter

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import net.minecraft.registry.Registries
import net.minecraft.registry.Registry
import net.minecraft.util.Identifier

class IdentifierWithRegistryAdapter<T : Any>(val registry: Registry<T>) : TypeAdapter<T>() {

    override fun read(source: JsonReader): T? {
        return registry[Identifier.tryParse(source.nextString())]
    }

    override fun write(sink: JsonWriter, value: T?) {
        val id = value?.let { registry.getId(it) }
        if (id == null) {
            sink.nullValue()
        } else {
            sink.value(id.toString())
        }
    }

    companion object {
        @JvmField
        val ENTITY_TYPE = IdentifierWithRegistryAdapter(Registries.ENTITY_TYPE)

        @JvmField
        val ITEM = IdentifierWithRegistryAdapter(Registries.ITEM)

        @JvmField
        val BLOCK = IdentifierWithRegistryAdapter(Registries.BLOCK)

        @JvmField
        val SOUND_EVENT = IdentifierWithRegistryAdapter(Registries.SOUND_EVENT)

        @JvmField
        val STATUS_EFFECT = IdentifierWithRegistryAdapter(Registries.STATUS_EFFECT)

        @JvmField
        val SCREEN_HANDLER = IdentifierWithRegistryAdapter(Registries.SCREEN_HANDLER)
    }

}
