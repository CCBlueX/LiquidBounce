package net.ccbluex.liquidbounce.web.persistant

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.Configurable

object PersistentLocalStorage : Configurable("storage") {

    var map by value("map", mutableMapOf<String, String>())

    init {
        ConfigSystem.root(this)
    }

    fun setItem(name: String, value: Boolean) {
        setItem(name, value.toString())
    }

    fun setItem(name: String, value: Int) {
        setItem(name, value.toString())
    }

    fun setItem(name: String, value: String) {
        map[name] = value
    }

    fun getItem(name: String): String? = map[name]

    fun removeItem(name: String) {
        map.remove(name)
    }

    fun clear() {
        map.clear()
    }

}
