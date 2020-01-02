package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.KeyEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import org.reflections.Reflections

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @author CCBlueX, DasDirt
 * @game Minecraft
 */
@SideOnly(Side.CLIENT)
object ModuleManager : Listenable {

    private val modules = mutableListOf<Module>()
    private val moduleClassMap = hashMapOf<Class<*>, Module>()

    init {
        LiquidBounce.CLIENT.eventManager.registerListener(this)
    }

    /**
     * Register all modules of liquidbounce
     */
    fun registerModules() {
        ClientUtils.getLogger().info("[ModuleManager] Loading modules...")
        for (aClass in Reflections(ModuleManager::class.java.getPackage().toString().replace("package ", "")).getTypesAnnotatedWith(ModuleInfo::class.java)) {
            registerModule(aClass.newInstance() as Module)
        }
        ClientUtils.getLogger().info("[ModuleManager] Loaded ${modules.size} modules.")
    }

    /**
     * Register [module]
     */
    fun registerModule(module: Module) {
        modules.add(module)
        moduleClassMap[module.javaClass] = module

        generateCommand(module)
        LiquidBounce.CLIENT.eventManager.registerListener(module)
    }

    /**
     * Unregister module
     */
    fun unregisterModule(module: Module?) {
        modules.remove(module)
        LiquidBounce.CLIENT.eventManager.unregisterListener(module!!)
    }

    /**
     * Generate command for [module]
     */
    private fun generateCommand(module: Module) {
        val values = module.values

        if (values.isEmpty())
            return

        LiquidBounce.CLIENT.commandManager.registerCommand(ModuleCommand(module, values))
    }

    /**
     * Sort all modules by name
     */
    fun sortModules() = modules.sortBy { it.name }

    /**
     * Legacy stuff
     *
     * TODO: Remove later when everything is translated to Kotlin
     */

    /**
     * Get module by [moduleClass]
     */
    @JvmStatic
    fun getModule(moduleClass: Class<*>): Module? {
        return moduleClassMap[moduleClass]
    }

    /**
     * Get module by [moduleName]
     */
    @JvmStatic
    fun getModule(moduleName: String?): Module? {
        for (module in modules)
            if (module.name.equals(moduleName, ignoreCase = true))
                return module

        return null
    }

    /**
     * Get all modules
     */
    @JvmStatic
    fun getModules() = modules

    /**
     * Module related events
     */

    /**
     * Handle incoming key presses
     */
    @EventTarget
    private fun onKey(event: KeyEvent) {
        for (module in modules)
            if (module.keyBind == event.key)
                module.toggle()
    }

    override fun handleEvents() = true

    operator fun get(clazz: Class<*>) = getModule(clazz)
}