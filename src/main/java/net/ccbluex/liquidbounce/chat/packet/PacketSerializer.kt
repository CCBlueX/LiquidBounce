/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.chat.packet

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import net.ccbluex.liquidbounce.chat.packet.packets.Packet
import java.lang.reflect.Type

/**
 * Packet Serializer
 *
 * Allows to serialize packets from class to json
 */
class PacketSerializer : JsonSerializer<Packet> {

    private val packetRegistry = HashMap<Class<out Packet>, String>()

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