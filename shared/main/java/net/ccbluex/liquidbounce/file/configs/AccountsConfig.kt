/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.file.configs

import com.google.gson.*
import net.ccbluex.liquidbounce.file.FileConfig
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount.AltServiceType
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import java.io.File
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

/**
 * Constructor of config
 *
 * @param file
 * of config
 */
class AccountsConfig(file: File) : FileConfig(file)
{
	val accounts: MutableList<MinecraftAccount> = ArrayList()

	// TODO: private val lock = ReentrantReadWriteLock()

	private val slashPattern: Pattern = Pattern.compile("/", Pattern.LITERAL)

	/**
	 * Load config from file
	 *
	 * @throws IOException
	 */
	@Throws(IOException::class)
	override fun loadConfig()
	{
		clearAccounts()
		try
		{
			val jsonElement = JsonParser().parse(MiscUtils.createBufferedFileReader(file))
			if (jsonElement is JsonNull) return
			val jsonArray = jsonElement.asJsonArray
			for (accountElement in jsonArray)
			{
				val accountObject = accountElement.asJsonObject
				val type: JsonElement? = accountObject["type"]
				val name: JsonElement = accountObject["name"]
				val password: JsonElement? = accountObject["password"]
				val inGameName: JsonElement? = accountObject["inGameName"]
				val bannedServers: JsonElement? = accountObject["bannedServers"]

				val account: MinecraftAccount = if (inGameName?.isJsonNull != false && password?.isJsonNull == false)
				{
					val serviceType = type?.let { AltServiceType.getById(it.asString) } ?: AltServiceType.MOJANG
					MinecraftAccount(serviceType, name.asString, password.asString)
				}
				else if (type?.isJsonNull != false) MinecraftAccount(AltServiceType.MOJANG, name.asString)
				else
				{
					val serviceType = AltServiceType.getById(type.asString) ?: AltServiceType.MOJANG
					if ((inGameName?.isJsonNull != false) && (password?.isJsonNull != false)) MinecraftAccount(serviceType, name.asString) else MinecraftAccount(serviceType, name.asString, password?.asString, inGameName?.asString)
				}

				if (bannedServers?.isJsonNull == false) account.bannedServers = bannedServers.asJsonArray.mapTo(ArrayList()) { it.asString }

				// lock.readLock().lock()
				addAccount(account)
				// lock.readLock().unlock()
			}
		}
		catch (ex: JsonSyntaxException)
		{
			loadWithOldFormat(ex)
		}
		catch (ex: IllegalStateException)
		{
			loadWithOldFormat(ex)
		}
	}

	private fun loadWithOldFormat(ex: Exception)
	{
		// When the JSON Parse fail, the client try to load and update the old config
		logger.info("[FileManager] Failed to load Accounts config with new format. Try to load old Accounts config...", ex)

		val accountList = Gson().fromJson<List<*>>(MiscUtils.createBufferedFileReader(file), MutableList::class.java) ?: return

		accounts.clear()

		accountList.map { it.toString().split(slashPattern, 2) }.filter(List<String>::isNotEmpty).forEach { info ->
			val information = info[0].split(":")
			val serviceType = AltServiceType.getById(information[0]) ?: AltServiceType.MOJANG

			val acc: MinecraftAccount = when (information.size)
			{
				1 -> MinecraftAccount(AltServiceType.MOJANG, information[0])
				2 -> MinecraftAccount(serviceType, information[1])
				3 -> MinecraftAccount(serviceType, information[1], information[2])
				else -> MinecraftAccount(serviceType, information[1], information[2], information[3])
			}

			if (info.size > 1) acc.bannedServers = deserializeOldBannedServers(info[1])

			accounts.add(acc)
		}

		logger.info("[FileManager] Loaded old Accounts config...")

		// Save the accounts into a new valid JSON file
		saveConfig()
		logger.info("[FileManager] Saved Accounts to new config...")
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

		// lock.writeLock().lock()
		for (minecraftAccount in accounts)
		{
			val accountObject = JsonObject()
			accountObject.addProperty("type", minecraftAccount.serviceType.id)
			accountObject.addProperty("name", minecraftAccount.name)
			accountObject.addProperty("password", minecraftAccount.password)
			accountObject.addProperty("inGameName", minecraftAccount.accountName)

			val bannedServers = minecraftAccount.bannedServers
			if (bannedServers.isNotEmpty())
			{
				val arr = JsonArray()
				bannedServers.forEach(arr::add)
				accountObject.add("bannedServers", arr)
			}

			jsonArray.add(accountObject)
		}
		// lock.writeLock().lock()

		val writer = MiscUtils.createBufferedFileWriter(file)
		writer.write(FileManager.PRETTY_GSON.toJson(jsonArray) + System.lineSeparator())
		writer.close()
	}

	/**
	 * Add cracked account to config
	 *
	 * @param account
	 * The account
	 */
	fun addAccount(account: MinecraftAccount)
	{
		if (isAccountExists(account.name)) return
		accounts.add(account)
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
	fun isAccountExists(name: String): Boolean = accounts.any { name.equals(it.name, ignoreCase = true) }

	/**
	 * Clear all minecraft accounts from alt array
	 */
	private fun clearAccounts()
	{
		accounts.clear()
	}

	companion object
	{
		fun deserializeOldBannedServers(str: String): MutableList<String> = str.split(";").toMutableList()
	}
}
