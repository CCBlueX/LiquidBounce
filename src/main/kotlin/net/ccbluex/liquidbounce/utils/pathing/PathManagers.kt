package net.ccbluex.liquidbounce.utils.pathing

import net.ccbluex.liquidbounce.features.module.modules.`fun`.ModuleSuicide

object PathManagers {

    lateinit var instance: BaritonePathManager
        private set

    fun baritoneExists(): Boolean {
        return try {
            Class.forName("baritone.api.BaritoneAPI")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }
    fun get(): BaritonePathManager = instance

    fun init() {
        if (baritoneExists()) {
            BaritoneUtils.IS_AVAILABLE = true
            instance = BaritonePathManager(ModuleSuicide)
        }
    }
}
