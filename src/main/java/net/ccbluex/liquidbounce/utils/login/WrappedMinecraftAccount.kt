package net.ccbluex.liquidbounce.utils.login

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.compat.Session
import me.liuli.elixir.utils.array
import me.liuli.elixir.utils.boolean
import me.liuli.elixir.utils.set

val MinecraftAccount.typeName: String
    get() = unwrapped.javaClass.name

val MinecraftAccount.canonicalTypeName: String
    get() = unwrapped.javaClass.canonicalName

val MinecraftAccount.wrapped: WrappedMinecraftAccount
    get() = if (this is WrappedMinecraftAccount) this else WrappedMinecraftAccount(this)

val MinecraftAccount.unwrapped: MinecraftAccount
    get() = if (this is WrappedMinecraftAccount) represented else this

/**
 * Wrapper class of [MinecraftAccount] to provide more features such as Banned Server List, Account Availability Check, TheAltening Account Support, etc.
 */
class WrappedMinecraftAccount(val represented: MinecraftAccount) : MinecraftAccount(represented.type)
{
    override val name: String
        get() = represented.name
    override val session: Session
        get() = represented.session
    var isAvailable = true
    val bannedServers = mutableListOf<String>()

    override fun fromRawJson(json: JsonObject)
    {
        represented.fromRawJson(json)
        isAvailable = json.boolean("is_available") != false
        json.array("banned_servers")?.forEach { bannedServers.add(it.asString) }
    }

    override fun toRawJson(json: JsonObject)
    {
        represented.toRawJson(json)
        json["is_available"] = isAvailable
        json["banned_servers"] = JsonArray().apply { bannedServers.map(::JsonPrimitive).forEach(::add) }
    }

    fun serializeBannedServers(): String = if (bannedServers.isEmpty()) "" else bannedServers.joinToString(separator = ", ")

    override fun update() = represented.update()
}
