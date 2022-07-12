package net.ccbluex.liquidbounce.utils.login

import com.google.gson.JsonObject
import me.liuli.elixir.account.CrackedAccount
import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.account.MojangAccount
import me.liuli.elixir.utils.set
import me.liuli.elixir.utils.string

/**
 * Copy of AccountSerializer in Elixir to support [WrappedMinecraftAccount]
 */
object WrappedAccountSerializer
{
    /**
     * write [account] to [JsonObject]
     */
    fun toJson(account: MinecraftAccount): JsonObject
    {
        val json = JsonObject()
        account.toRawJson(json)
        json["type"] = account.canonicalTypeName
        return json
    }

    /**
     * read [WrappedMinecraftAccount] from [json]
     */
    fun fromJson(json: JsonObject): WrappedMinecraftAccount
    {
        val account = WrappedMinecraftAccount(Class.forName(json.string("type")!!).newInstance() as MinecraftAccount)
        account.fromRawJson(json)
        return account
    }

    /**
     * get an instance of [WrappedMinecraftAccount] from [name] and [password]
     */
    fun accountInstance(name: String, password: String): WrappedMinecraftAccount
    {
        return WrappedMinecraftAccount(if (name.startsWith("ms@"))
        {
            val realName = name.substring(3) // drop 'ms@'
            if (password.isEmpty()) MicrosoftAccount.buildFromAuthCode(realName, MicrosoftAccount.AuthMethod.MICROSOFT) else MicrosoftAccount.buildFromPassword(realName, password)
        }
        else if (password.isEmpty()) CrackedAccount().also { it.name = name }
        else MojangAccount().also { it.name = name; it.password = password })
    }
}
