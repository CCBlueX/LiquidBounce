package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.notifications.Notification
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.render.ColorUtils.stripColor
import net.ccbluex.liquidbounce.value.Value
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.util.*

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
@SideOnly(Side.CLIENT)
open class Module : MinecraftInstance(), Listenable {
    private val canEnable = javaClass.getAnnotation(ModuleInfo::class.java).canEnable
    var name = javaClass.getAnnotation(ModuleInfo::class.java).name
    var description = javaClass.getAnnotation(ModuleInfo::class.java).description
    var category = javaClass.getAnnotation(ModuleInfo::class.java).category
    var keyBind = javaClass.getAnnotation(ModuleInfo::class.java).keyBind
        set(keyBind) {
            field = keyBind
            LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.modulesConfig)
        }
    var state = false
        set(value) {
            if (field == value) {
                return
            }
            onToggle(value)
            if (value) {
                onEnable()
                if (canEnable)
                    field = true
            } else {
                onDisable()
                field = false
            }
            LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.modulesConfig)
        }


    // HUD
    val hue = Math.random().toFloat()
    var slide = 0f

    val tagName: String
        get() = name + if (tag == null) "" else " §7$tag"

    val colorlessTagName: String
        get() = name + if (tag == null) "" else " " + stripColor(tag)

    open val tag: String?
        get() = null

    fun toggle() {
        state = !state
    }

    open fun onToggle(state: Boolean) {
        if (!LiquidBounce.CLIENT.isStarting && this.state != state) {
            mc.soundHandler.playSound(PositionedSoundRecord.create(ResourceLocation("random.click"), 1.0f))
            LiquidBounce.CLIENT.hud.addNotification(Notification((if (state) "Enabled " else "Disabled ") + name))
        }
    }

    open fun onEnable() {}
    open fun onDisable() {}

    fun onStarted() {}
    open fun showArray(): Boolean {
        return true
    }

    // Value
    open fun getValue(valueName: String?): Value<*>? {
        for (field in javaClass.declaredFields) {
            try {
                field.isAccessible = true
                val o = field[this]
                if (o is Value<*>) {
                    if (o.name.equals(valueName, ignoreCase = true)) {
                        return o
                    }
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
        return null
    }

    open val values: MutableList<Value<*>>
        get() {
            val values: MutableList<Value<*>> = ArrayList()
            for (_field in javaClass.declaredFields) {
                try {
                    _field.isAccessible = true
                    val o = _field[this]
                    if (o is Value<*>) {
                        values.add(o)
                    }
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
            return values
        }

    override fun handleEvents(): Boolean {
        return state
    }
}