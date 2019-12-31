package net.ccbluex.liquidbounce.script.api

import jdk.nashorn.api.scripting.ScriptObjectMirror
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.script.api.module.AdaptedModule
import net.ccbluex.liquidbounce.script.api.module.ScriptModule

/**
 * A script api class for module support
 *
 * @author CCBlueX
 */
object ModuleManager {

    /**
     * Register a java script based module
     *
     * @param scriptObjectMirror Java Script function for module (like a module class)
     */
    @JvmStatic
    fun registerModule(scriptObjectMirror: ScriptObjectMirror): Module {
        val module = ScriptModule(scriptObjectMirror)
        LiquidBounce.CLIENT.moduleManager.registerModule(module)
        return module
    }

    /**
     * Unregister a module
     *
     * @param module Instance of target module
     */
    @JvmStatic
    fun unregisterModule(module : Module) {
        unregisterModule(module, true)
    }

    /**
     * Unregister a module
     *
     * @param scriptObjectMirror Java Script function for module (like a module class)
     */
    @JvmStatic
    fun unregisterModule(scriptObjectMirror : ScriptObjectMirror) {
        val module = ScriptModule(scriptObjectMirror)

        unregisterModule(ModuleManager.getModule(module.name)!!, true)
    }

    /**
     * Unregister a module
     *
     * @param module Instance of target module
     * @param autoDisable Automatically disable module when unregistering module
     */
    @JvmStatic
    fun unregisterModule(module : Module, autoDisable : Boolean) {
        if(autoDisable && module.state) module.state = false

        LiquidBounce.CLIENT.moduleManager.unregisterModule(module)
    }

    /**
     * Search for module name and return then a adapted module
     *
     * @param moduleName Name of module
     */
    @JvmStatic
    fun getModule(moduleName: String) = AdaptedModule(ModuleManager.getModule(moduleName)!!)

    /**
     * @return a list of all modules
     */
    @JvmStatic
    fun getModules() = ModuleManager.getModules().map { AdaptedModule(it) }
}