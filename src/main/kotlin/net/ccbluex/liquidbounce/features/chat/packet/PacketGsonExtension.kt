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
 *
 *
 */
package net.ccbluex.liquidbounce.features.chat.packet

import com.google.gson.*
import net.ccbluex.liquidbounce.config.gson.publicGson
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import java.lang.reflect.Type

/**
 * Packet Serializer
 *
 * Allows serializing packets from class to json
 */
class PacketSerializer : JsonSerializer<Packet> {

    private val packetRegistry = hashMapOf<Class<out Packet>, String>()

    /**
     * Register packet
     */
    fun registerPacket(packetName: String, packetClass: Class<out Packet>) {
        packetRegistry[packetClass] = packetName
    }

    inline fun <reified T : Packet> register(name: String) {
        registerPacket(name, T::class.java)
    }

    /**
     * Gson invokes this call-back method during serialization when it encounters a field of the
     * specified type.
     *
     *
     * In the implementation of this call-back method, you should consider invoking
     * [JsonSerializationContext.serialize] method to create JsonElements for any
     * non-trivial field of the `src` object. However, you should never invoke it on the
     * `src` object itself since that will cause an infinite loop (Gson will call your
     * call-back method again).
     *
     * @param src the object that needs to be converted to Json.
     * @param typeOfSrc the actual type (fully genericized version) of the source object.
     * @return a JsonElement corresponding to the specified object.
     */
    override fun serialize(src: Packet, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        val packetName = packetRegistry.getOrDefault(src.javaClass, "UNKNOWN")
        val serializedPacket =
            SerializedPacket(packetName, if (src.javaClass.constructors.none { it.parameterCount != 0 }) null else src)

        return publicGson.toJsonTree(serializedPacket)
    }

}

/**
 * Packet Deserializer
 *
 * Allows deserializing packets from json to class
 */
class PacketDeserializer : JsonDeserializer<Packet> {

    private val packetRegistry = hashMapOf<String, Class<out Packet>>()

    /**
     * Register packet
     */
    fun registerPacket(packetName: String, packetClass: Class<out Packet>) {
        packetRegistry[packetName] = packetClass
    }

    inline fun <reified T : Packet> register(name: String) {
        registerPacket(name, T::class.java)
    }

    /**
     * Gson invokes this call-back method during deserialization when it encounters a field of the
     * specified type.
     *
     * In the implementation of this call-back method, you should consider invoking
     * [JsonDeserializationContext.deserialize] method to create objects
     * for any non-trivial field of the returned object. However, you should never invoke it on
     * the same type passing `json` since that will cause an infinite loop (Gson will call your
     * call-back method again).
     *
     * @param json The Json data being deserialized
     * @param typeOfT The type of the Object to deserialize to
     * @return a deserialized object of the specified type typeOfT which is a subclass of `T`
     * @throws JsonParseException if json is not in the expected format of `typeofT`
     */
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext?): Packet? {
        val packetObject = json.asJsonObject
        val packetName = packetObject.get("m").asString

        if (!packetRegistry.containsKey(packetName)) return null

        if (!packetObject.has("c")) packetObject.add("c", emptyJsonObject())

        return publicGson.fromJson(packetObject.get("c"), packetRegistry[packetName])

    }

}
