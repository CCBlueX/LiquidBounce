/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.chat.client.packet

import com.google.gson.*
import java.lang.reflect.Type

/**
 * Packet Serializer
 *
 * Allows to serialize packets from class to json
 */
class PacketSerializer : JsonSerializer<Packet> {

    private val packetRegistry = hashMapOf<Class<out Packet>, String>()

    /**
     * Register packet
     */
    fun registerPacket(packetName: String, packetClass: Class<out Packet>) {
        packetRegistry[packetClass] = packetName
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
        val serializedPacket = SerializedPacket(packetName, if(src.javaClass.constructors.none { it.parameterCount != 0 }) null else src )

        return Gson().toJsonTree(serializedPacket)
    }

}


/**
 * Packet Deserializer
 *
 * Allows to deserialize packets from json to class
 */
class PacketDeserializer : JsonDeserializer<Packet> {

    private val packetRegistry = hashMapOf<String, Class<out Packet>>()

    /**
     * Register packet
     */
    fun registerPacket(packetName: String, packetClass: Class<out Packet>) {
        packetRegistry[packetName] = packetClass
    }

    /**
     * Gson invokes this call-back method during deserialization when it encounters a field of the
     * specified type.
     *
     * In the implementation of this call-back method, you should consider invoking
     * [JsonDeserializationContext.deserialize] method to create objects
     * for any non-trivial field of the returned object. However, you should never invoke it on the
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

        if(!packetRegistry.containsKey(packetName)) return null

        if(!packetObject.has("c")) packetObject.add("c", JsonObject())

        return Gson().fromJson(packetObject.get("c"), packetRegistry[packetName])

    }

}

