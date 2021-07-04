/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.injection.backend.Backend
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.value.Value
import kotlin.random.Random

open class Module : MinecraftInstance(), Listenable
{
	var isSupported: Boolean

	// Module information
	// TODO: Remove ModuleInfo and change to constructor (#Kotlin)
	var name: String
	var description: String
	var category: ModuleCategory
	var keyBind = mutableSetOf<Int>()
		set(keyBind)
		{
			field = keyBind

			if (!LiquidBounce.isStarting) FileManager.saveConfig(LiquidBounce.fileManager.modulesConfig)
		}
	var array = true
		set(array)
		{
			field = array

			if (!LiquidBounce.isStarting) FileManager.saveConfig(LiquidBounce.fileManager.modulesConfig)
		}
	private val canEnable: Boolean

	var slideStep = 0F

	init
	{
		val moduleInfo = javaClass.getAnnotation(ModuleInfo::class.java) ?: throw Exception("Module ${javaClass.simpleName} doesn't have ModuleInfo annotation")

		name = moduleInfo.name
		description = moduleInfo.description
		category = moduleInfo.category
		keyBind = moduleInfo.keyBind.toMutableSet()
		array = moduleInfo.array
		canEnable = moduleInfo.canEnable
		isSupported = Backend.REPRESENTED_BACKEND_VERSION in moduleInfo.supportedVersions
	}

	/**
	 * Current state of module
	 */
	var state = false
		set(value)
		{
			if (field == value) return

			// Call toggle
			onToggle(value)

			// Play sound and add notification
			if (!LiquidBounce.isStarting)
			{
				mc.soundHandler.playSound("random.click", 1.0F)
				LiquidBounce.hud.addNotification(Notification("Module Manager", "${if (value) "Enabled " else "Disabled "}$name", null))
			}

			// Call on enabled or disabled
			if (value)
			{
				try
				{
					onEnable()
				}
				catch (e: Exception)
				{
					ClientUtils.logger.error("Uncaught exception '$e' occurred while onEnable() in module $name", e)
				}

				if (canEnable) field = true
			}
			else
			{
				try
				{
					onDisable()
				}
				catch (e: Exception)
				{
					ClientUtils.logger.error("Uncaught exception '$e' occurred while onDisable() in module $name", e)
				}
				field = false
			}

			// Save module state
			FileManager.saveConfig(LiquidBounce.fileManager.modulesConfig)
		}

	/**
	 * HUD
	 */
	val hue = Random.nextFloat()
	var slide = 0F

	/**
	 * Tag
	 */
	open val tag: String?
		get() = null

	/**
	 * Toggle module
	 */
	fun toggle()
	{
		state = !state
	}

	/**
	 * Called when module toggled
	 */
	open fun onToggle(state: Boolean)
	{
	}

	/**
	 * Called when module enabled
	 */
	open fun onEnable()
	{
	}

	/**
	 * Called when module disabled
	 */
	open fun onDisable()
	{
	}

	/**
	 * Get module by [valueName]
	 */
	open fun getValue(valueName: String) = values.find { it.name.equals(valueName, ignoreCase = true) }

	/**
	 * Get all values of module
	 */
	open val values: List<Value<*>>
		get() = javaClass.declaredFields.asSequence().map { valueField ->
			valueField.isAccessible = true
			valueField[this]
		}.filterIsInstance<Value<*>>().filter(Value<*>::isSupported).toList()

	/**
	 * Events should be handled when module is enabled
	 */
	override fun handleEvents() = state
}
