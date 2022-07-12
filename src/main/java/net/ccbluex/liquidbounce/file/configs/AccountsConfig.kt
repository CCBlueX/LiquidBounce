/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.*
import me.liuli.elixir.account.CrackedAccount
import me.liuli.elixir.account.MinecraftAccount
import me.liuli.elixir.account.MojangAccount
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.login.WrappedAccountSerializer
import net.ccbluex.liquidbounce.utils.login.WrappedMinecraftAccount
import net.ccbluex.liquidbounce.utils.login.typeName
import net.ccbluex.liquidbounce.utils.login.wrapped
import net.minecraft.util.*
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class AccountsConfig(file: File) : FileConfig(file)
{
    val accounts: MutableList<WrappedMinecraftAccount> = ArrayList()

    /**
     * Load config from file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun loadConfig()
    {
        val jsonElement = JsonParser().parse(BufferedReader(FileReader(file)))

        if (jsonElement is JsonNull) return

        for (accountElement in jsonElement.asJsonArray)
        {
            val accountObject = accountElement.asJsonObject
            try
            {
                // Import Elixir account format
                accounts.add(WrappedAccountSerializer.fromJson(accountElement.asJsonObject))
            }
            catch (e: JsonSyntaxException)
            {
                // Import old account format
                val name = accountObject["name"]
                val password = accountObject["password"]
                val inGameName = accountObject["inGameName"]
                if (inGameName.isJsonNull && password.isJsonNull)
                {
                    val mojangAccount = MojangAccount()
                    mojangAccount.email = name.asString
                    mojangAccount.name = inGameName.asString
                    mojangAccount.password = password.asString
                    accounts.add(mojangAccount.wrapped)
                }
                else
                {
                    val crackedAccount = CrackedAccount()
                    crackedAccount.name = name.asString
                    accounts.add(crackedAccount.wrapped)
                }
            }
            catch (e: Exception)
            {
                val account: MinecraftAccount
                val type = accountObject["type"] ?: continue
                val name = accountObject["name"] ?: continue
                val password = accountObject["password"]
                val inGameName = accountObject["inGameName"]
                val bannedServers = accountObject["bannedServers"]
                if (inGameName?.isJsonNull == false && password?.isJsonNull == false)
                {
                    account = MojangAccount()
                    account.email = name.asString
                    account.name = inGameName.asString
                    account.password = password.asString
                }
                else
                {
                    account = CrackedAccount()
                    account.name = name.asString
                }

                val wrappedAccount = WrappedMinecraftAccount(account)
                if (type.asString?.contains("invalid", ignoreCase = true) == true) wrappedAccount.isAvailable = false
                if (bannedServers?.isJsonNull == false) bannedServers.asJsonArray.mapTo(wrappedAccount.bannedServers) { it.asString }

                accounts.add(wrappedAccount)
            }
        }
    }

    /**
     * Save config to file
     *
     * @throws IOException
     */
    @Throws(IOException::class)
    override fun saveConfig()
    {
        val jsonArray = JsonArray()
        accounts.map(WrappedAccountSerializer::toJson).forEach(jsonArray::add)

        val writer = file.bufferedWriter()
        writer.write(FileManager.PRETTY_GSON.toJson(jsonArray) + System.lineSeparator())
        writer.close()
    }

    /**
     * Add cracked account to config
     *
     * @param name of account
     */
    fun addCrackedAccount(name: String?)
    {
        val crackedAccount = CrackedAccount()
        crackedAccount.name = name ?: return
        if (isAccountExists(crackedAccount)) return
        accounts.add(crackedAccount.wrapped)
    }

    /**
     * Add account to config
     *
     * @param name     of account
     * @param password of password
     */
    fun addMojangAccount(name: String?, password: String?)
    {
        val mojangAccount = MojangAccount()
        mojangAccount.name = name ?: return
        mojangAccount.password = password ?: return
        if (!isAccountExists(mojangAccount)) accounts.add(mojangAccount.wrapped)
    }

    /**
     * Add cracked account to config
     *
     * @param account
     * The account
     */
    fun addAccount(account: MinecraftAccount)
    {
        if (!isAccountExists(account)) accounts.add(account.wrapped)
    }

    // /**
    //  * Remove account from config
    //  *
    //  * @param selectedSlot
    //  * of the account
    //  */
    // fun removeAccount(selectedSlot: Int)
    // {
    // 	accounts.removeAt(selectedSlot)
    // }

    /**
     * Removed an account from the config
     *
     * @param account
     * the account
     */
    fun removeAccount(account: MinecraftAccount?)
    {
        accounts.remove(account)
    }

    /**
     * Check if the account is already added
     *
     * @param  name
     * of account
     * @return      if the account exists
     */
    fun isAccountExists(account: MinecraftAccount): Boolean = accounts.any { it.typeName == account.typeName && it.name.equals(account.name, ignoreCase = true) }

    /**
     * Clear all minecraft accounts from alt array
     */
    private fun clearAccounts()
    {
        accounts.clear()
    }
}
