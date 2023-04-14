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
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.utils.toLowerCamelCase
import net.ccbluex.liquidbounce.value.Value
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation
import org.lwjgl.input.Keyboard

// TODO: Remove @JvmOverloads when all modules are ported to kotlin.
open class Module @JvmOverloads constructor(

    val name: String,
    val category: ModuleCategory,
    val forcedDescription: String = "",
    keyBind: Int = Keyboard.KEY_NONE,
    val defaultInArray: Boolean = true, // Used in HideCommand to reset modules visibility.
    private val canEnable: Boolean = true,

    ) : MinecraftInstance(), Listenable {

    // Module information
    var keyBind = keyBind
        set(keyBind) {
            field = keyBind

            saveConfig(modulesConfig)
        }
    var inArray = defaultInArray
        set(value) {
            field = value

            saveConfig(modulesConfig)
        }

    val description: String
        get() = forcedDescription.ifBlank { translation("module.${name.toLowerCamelCase()}.description") }

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
                addNotification(Notification(if (value) translation("notification.moduleEnabled", name) else translation("notification.moduleDisabled", name)))
            }

            // Call on enabled or disabled
            if (value) {
                onEnable()

                if (canEnable)
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

    val tagName
        get() = "$name${if (tag == null) "" else " §7$tag"}"

    val colorlessTagName
        get() = "$name${if (tag == null) "" else " " + stripColor(tag!!)}"

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

    // Get value using: module[valueName]
    operator fun get(valueName: String) = getValue(valueName)

    /**
     * Get all values of module
     */
    open val values: List<Value<*>>
        get() = javaClass.declaredFields.map { valueField ->
            valueField.isAccessible = true
            valueField[this]
        }.filterIsInstance<Value<*>>()

    /**
     * Events should be handled when module is enabled
     */
    override fun handleEvents() = state
}