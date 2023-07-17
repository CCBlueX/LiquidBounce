/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.script.api.global

import jdk.nashorn.api.scripting.JSObject
import jdk.nashorn.api.scripting.ScriptObjectMirror
import jdk.nashorn.api.scripting.ScriptUtils
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.FontRenderer

/**
 * Object used by the script API to provide an idiomatic way of creating module values.
 */
object Setting {

    /**
     * Creates a boolean value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [BoolValue]
     */
    @JvmStatic
    fun boolean(settingInfo: JSObject): BoolValue {
        val name = settingInfo["name"] as String
        val default = settingInfo["default"] as Boolean

        val isSupportedCallback = settingInfo["isSupported"] as? ScriptObjectMirror
        val onChangeCallback = settingInfo["onChange"] as? ScriptObjectMirror
        val onChangedCallback = settingInfo["onChanged"] as? ScriptObjectMirror

        return object : BoolValue(name, default) {
            override fun isSupported() = isSupportedCallback?.call(null) as? Boolean ?: true

            override fun onChange(oldValue: Boolean, newValue: Boolean) =
                onChangeCallback?.call(null, oldValue, newValue) as? Boolean ?: newValue

            override fun onChanged(oldValue: Boolean, newValue: Boolean) {
                onChangedCallback?.call(null, oldValue, newValue)
            }
        }
    }

    /**
     * Creates an integer value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [IntegerValue]
     */
    @JvmStatic
    fun integer(settingInfo: JSObject): IntegerValue {
        val name = settingInfo["name"] as String
        val default = settingInfo["default"]!!.toInt()
        val min = settingInfo["min"]!!.toInt()
        val max = settingInfo["max"]!!.toInt()

        val isSupportedCallback = settingInfo["isSupported"] as? ScriptObjectMirror
        val onChangeCallback = settingInfo["onChange"] as? ScriptObjectMirror
        val onChangedCallback = settingInfo["onChanged"] as? ScriptObjectMirror

        return object : IntegerValue(name, default, min..max) {
            override fun isSupported() = isSupportedCallback?.call(null) as? Boolean ?: true

            override fun onChange(oldValue: Int, newValue: Int) =
                onChangeCallback?.call(null, oldValue, newValue)?.toInt() ?: newValue

            override fun onChanged(oldValue: Int, newValue: Int) {
                onChangedCallback?.call(null, oldValue, newValue)
            }
        }
    }

    /**
     * Creates a float value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [FloatValue]
     */
    @JvmStatic
    fun float(settingInfo: JSObject): FloatValue {
        val name = settingInfo["name"] as String
        val default = settingInfo["default"]!!.toFloat()
        val min = settingInfo["min"]!!.toFloat()
        val max = settingInfo["max"]!!.toFloat()

        val isSupportedCallback = settingInfo["isSupported"] as? ScriptObjectMirror
        val onChangeCallback = settingInfo["onChange"] as? ScriptObjectMirror
        val onChangedCallback = settingInfo["onChanged"] as? ScriptObjectMirror

        return object : FloatValue(name, default, min..max) {
            override fun isSupported() = isSupportedCallback?.call(null) as? Boolean ?: true

            override fun onChange(oldValue: Float, newValue: Float) =
                onChangeCallback?.call(null, oldValue, newValue)?.toFloat() ?: newValue

            override fun onChanged(oldValue: Float, newValue: Float) {
                onChangedCallback?.call(null, oldValue, newValue)
            }
        }
    }

    /**
     * Creates a text value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [TextValue]
     */
    @JvmStatic
    fun text(settingInfo: JSObject): TextValue {
        val name = settingInfo["name"] as String
        val default = settingInfo["default"] as String

        val isSupportedCallback = settingInfo["isSupported"] as? ScriptObjectMirror
        val onChangeCallback = settingInfo["onChange"] as? ScriptObjectMirror
        val onChangedCallback = settingInfo["onChanged"] as? ScriptObjectMirror

        return object : TextValue(name, default) {
            override fun isSupported() = isSupportedCallback?.call(null) as? Boolean ?: true

            override fun onChange(oldValue: String, newValue: String) =
                onChangeCallback?.call(null, oldValue, newValue) as? String ?: newValue

            override fun onChanged(oldValue: String, newValue: String) {
                onChangedCallback?.call(null, oldValue, newValue)
            }
        }
    }

    /**
     * Creates a block value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [BlockValue]
     */
    @JvmStatic
    fun block(settingInfo: JSObject): BlockValue {
        val name = settingInfo["name"] as String
        val default = settingInfo["default"]!!.toInt()

        val isSupportedCallback = settingInfo["isSupported"] as? ScriptObjectMirror
        val onChangeCallback = settingInfo["onChange"] as? ScriptObjectMirror
        val onChangedCallback = settingInfo["onChanged"] as? ScriptObjectMirror

        return object : BlockValue(name, default) {
            override fun isSupported() = isSupportedCallback?.call(null) as? Boolean ?: true

            override fun onChange(oldValue: Int, newValue: Int) =
                onChangeCallback?.call(null, oldValue, newValue)?.toInt() ?: newValue

            override fun onChanged(oldValue: Int, newValue: Int) {
                onChangedCallback?.call(null, oldValue, newValue)
            }
        }
    }

    /**
     * Creates a list value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [ListValue]
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun list(settingInfo: JSObject): ListValue {
        val name = settingInfo["name"] as String
        val values = ScriptUtils.convert(settingInfo["values"], Array<String>::class.java) as Array<String>
        val default = settingInfo["default"] as String

        val isSupportedCallback = settingInfo["isSupported"] as? ScriptObjectMirror
        val onChangeCallback = settingInfo["onChange"] as? ScriptObjectMirror
        val onChangedCallback = settingInfo["onChanged"] as? ScriptObjectMirror

        return object : ListValue(name, values, default) {
            override fun isSupported() = isSupportedCallback?.call(null) as? Boolean ?: true

            override fun onChange(oldValue: String, newValue: String) =
                onChangeCallback?.call(null, oldValue, newValue) as? String ?: newValue

            override fun onChanged(oldValue: String, newValue: String) {
                onChangedCallback?.call(null, oldValue, newValue)
            }
        }
    }

    /**
     * Creates a font value.
     * @param settingInfo JavaScript object containing information about the value.
     * @return An instance of [FontValue]
     */
    @JvmStatic
    fun font(settingInfo: JSObject): FontValue {
        val name = settingInfo["name"] as String
        val default = settingInfo["default"] as? FontRenderer ?: Fonts.minecraftFont

        val isSupportedCallback = settingInfo["isSupported"] as? ScriptObjectMirror
        val onChangeCallback = settingInfo["onChange"] as? ScriptObjectMirror
        val onChangedCallback = settingInfo["onChanged"] as? ScriptObjectMirror

        return object : FontValue(name, default) {
            override fun isSupported() = isSupportedCallback?.call(null) as? Boolean ?: true

            override fun onChange(oldValue: FontRenderer, newValue: FontRenderer): FontRenderer =
                onChangeCallback?.call(null, oldValue, newValue) as? FontRenderer ?: newValue

            override fun onChanged(oldValue: FontRenderer, newValue: FontRenderer) {
                onChangedCallback?.call(null, oldValue, newValue)
            }
        }
    }
    
}

private fun Any.toInt() = (this as Number).toInt()
private fun Any.toFloat() = (this as Number).toFloat()

private operator fun JSObject.get(key: String) =
    if (this.hasMember(key)) this.getMember(key) else null
