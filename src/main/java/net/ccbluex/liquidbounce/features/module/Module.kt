/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.LiquidBounce.isStarting
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.file.FileManager.modulesConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.ui.client.hud.HUD.addNotification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Arraylist
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.toLowerCamelCase
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.value.Value
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard

// TODO: Remove @JvmOverloads when all modules are ported to kotlin.
open class Module @JvmOverloads constructor(

    val name: String,
    val category: ModuleCategory,
    defaultKeyBind: Int = Keyboard.KEY_NONE,
    val defaultInArray: Boolean = true, // Used in HideCommand to reset modules visibility.
    private val canBeEnabled: Boolean = true,
    private val forcedDescription: String? = null,
    // Adds spaces between lowercase and uppercase letters (KillAura -> Kill Aura)
    val spacedName: String = name.split("(?<=[a-z])(?=[A-Z])".toRegex()).joinToString(separator = " ")

) : MinecraftInstance(), Listenable {

    // Module information

    // Get normal or spaced name
    fun getName(spaced: Boolean = Arraylist.spacedModules) = if (spaced) spacedName else name

    var keyBind = defaultKeyBind
        set(keyBind) {
            field = keyBind

            saveConfig(modulesConfig)
        }

    var inArray = defaultInArray
        set(value) {
            field = value

            saveConfig(modulesConfig)
        }

    val description
        get() = forcedDescription ?: translation("module.${name.toLowerCamelCase()}.description")

    var slideStep = 0F

    // Current state of module
    var state = false
        set(value) {
            if (field == value)
                return

            // Call toggle
            onToggle(value)

            // Play sound and add notification
            if (!isStarting) {
                mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("random.click"), 1F))
                addNotification(Notification(translation("notification.module" + if (value) "Enabled" else "Disabled", getName())))
            }

            // Call on enabled or disabled
            if (value) {
                onEnable()

                if (canBeEnabled)
                    field = true
            } else {
                onDisable()
                field = false
            }

            // Save module state
            saveConfig(modulesConfig)
        }


    // HUD
    val hue = nextFloat()
    var slide = 0F

    // Tag
    open val tag: String?
        get() = null

    /**
     * Toggle module
     */
    fun toggle() {
        state = !state
    }

    /**
     * Called when module toggled
     */
    open fun onToggle(state: Boolean) {}

    /**
     * Called when module enabled
     */
    open fun onEnable() {}

    /**
     * Called when module disabled
     */
    open fun onDisable() {}

    /**
     * Get value by [valueName]
     */
    open fun getValue(valueName: String) = values.find { it.name.equals(valueName, ignoreCase = true) }

    /**
     * Get value via `module[valueName]`
     */
    operator fun get(valueName: String) = getValue(valueName)

    /**
     * Get all values of module with unique names
     */
    open val values
        get() = javaClass.declaredFields.map { valueField ->
            valueField.isAccessible = true
            valueField[this]
        }.filterIsInstance<Value<*>>().distinctBy { it.name }

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = state
}