/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import net.ccbluex.liquidbounce.LiquidBounce.commandManager
import net.ccbluex.liquidbounce.event.EventManager.registerListener
import net.ccbluex.liquidbounce.event.EventManager.unregisterListener
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.KeyEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.module.modules.combat.*
import net.ccbluex.liquidbounce.features.module.modules.exploit.*
import net.ccbluex.liquidbounce.features.module.modules.`fun`.Derp
import net.ccbluex.liquidbounce.features.module.modules.`fun`.SkinDerp
import net.ccbluex.liquidbounce.features.module.modules.misc.*
import net.ccbluex.liquidbounce.features.module.modules.movement.*
import net.ccbluex.liquidbounce.features.module.modules.player.*
import net.ccbluex.liquidbounce.features.module.modules.render.*
import net.ccbluex.liquidbounce.features.module.modules.world.*
import net.ccbluex.liquidbounce.features.module.modules.world.Timer
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import java.util.*


class ModuleManager : Listenable {

    val modules = TreeSet<Module> { module1, module2 -> module1.name.compareTo(module2.name) }
    private val moduleClassMap = hashMapOf<Class<*>, Module>()

    init {
        registerListener(this)
    }

    /**
     * Register all modules
     */
    fun registerModules() {
        LOGGER.info("[ModuleManager] Loading modules...")

        // Register modules which need to be instanced (Java classes)
        registerModules(
            AutoArmor::class.java,
            Sprint::class.java,
            Teleport::class.java,
            Phase::class.java,
            TeleportHit::class.java,
            Ignite::class.java,
            ItemTeleport::class.java
        )

        // Register modules which have already been instanced (Kotlin objects)
        registerModules(
            AutoBow,
            AutoLeave,
            AutoPot,
            AutoSoup,
            AutoWeapon,
            BowAimbot,
            Criticals,
            KillAura,
            Trigger,
            Velocity,
            Fly,
            HighJump,
            InventoryMove,
            NoSlow,
            LiquidWalk,
            SafeWalk,
            WallClimb,
            Strafe,
            Teams,
            NoRotateSet,
            ChestStealer,
            Scaffold,
            CivBreak,
            Tower,
            FastBreak,
            FastPlace,
            ESP,
            Speed,
            Tracers,
            NameTags,
            FastUse,
            Fullbright,
            ItemESP,
            StorageESP,
            Projectiles,
            NoClip,
            Nuker,
            PingSpoof,
            FastClimb,
            Step,
            AutoRespawn,
            AutoTool,
            NoWeb,
            Spammer,
            IceSpeed,
            Zoot,
            Regen,
            NoFall,
            Blink,
            NoHurtCam,
            Ghost,
            MidClick,
            XRay,
            Timer,
            Sneak,
            SkinDerp,
            GhostHand,
            AutoWalk,
            AutoBreak,
            FreeCam,
            Aimbot,
            Eagle,
            HitBox,
            AntiCactus,
            Plugins,
            AntiHunger,
            ConsoleSpammer,
            LongJump,
            Parkour,
            LadderJump,
            FastBow,
            MultiActions,
            AirJump,
            AutoClicker,
            NoBob,
            BlockOverlay,
            NoFriends,
            BlockESP,
            Chams,
            Clip,
            ServerCrasher,
            NoFOV,
            FastStairs,
            Derp,
            ReverseStep,
            TNTBlock,
            InventoryCleaner,
            TrueSight,
            LiquidChat,
            AntiBlind,
            NoSwing,
            BedGodMode,
            BugUp,
            Breadcrumbs,
            AbortBreaking,
            PotionSaver,
            CameraClip,
            WaterSpeed,
            SlimeJump,
            MoreCarry,
            NoPitchLimit,
            Kick,
            Liquids,
            AtAllProvider,
            AirLadder,
            GodMode,
            ForceUnicodeChat,
            BufferSpeed,
            SuperKnockback,
            ProphuntESP,
            AutoFish,
            Damage,
            Freeze,
            KeepContainer,
            VehicleOneHit,
            Reach,
            Rotations,
            NoJumpDelay,
            BlockWalk,
            AntiAFK,
            PerfectHorseJump,
            HUD,
            TNTESP,
            ComponentOnHover,
            KeepAlive,
            ResourcePackSpoof,
            NoSlowBreak,
            PortalMenu,
            AutoRod,
            AttackEffects,
            NoBooks,
            AutoAccount,
            NoScoreboard,
            Fucker,
            ChestAura,
            AntiBot,
            Animations,
            Backtrack,
            ClickGUI,
            NameProtect
        )

        LOGGER.info("[ModuleManager] Loaded ${modules.size} modules.")
    }

    /**
     * Register [module]
     */
    fun registerModule(module: Module) {
        modules += module
        moduleClassMap[module.javaClass] = module

        generateCommand(module)
        registerListener(module)
    }

    /**
     * Register [moduleClass] with new instance
     */
    private fun registerModule(moduleClass: Class<out Module>) {
        try {
            registerModule(moduleClass.newInstance())
        } catch (e: Throwable) {
            LOGGER.error("Failed to load module: ${moduleClass.name} (${e.javaClass.name}: ${e.message})")
        }
    }

    /**
     * Register a list of modules
     */
    @SafeVarargs
    fun registerModules(vararg modules: Class<out Module>) = modules.forEach(this::registerModule)


    /**
     * Register a list of modules
     */
    @SafeVarargs
    fun registerModules(vararg modules: Module) = modules.forEach(this::registerModule)

    /**
     * Unregister module
     */
    fun unregisterModule(module: Module) {
        modules.remove(module)
        moduleClassMap.remove(module::class.java)
        unregisterListener(module)
    }

    /**
     * Generate command for [module]
     */
    internal fun generateCommand(module: Module) {
        val values = module.values

        if (values.isEmpty())
            return

        commandManager.registerCommand(ModuleCommand(module, values))
    }

    /**
     * Get module by [moduleClass]
     */
    fun getModule(moduleClass: Class<*>) = moduleClassMap[moduleClass]!!

    operator fun get(clazz: Class<*>) = getModule(clazz)

    /**
     * Get module by [moduleName]
     */
    fun getModule(moduleName: String?) = modules.find { it.name.equals(moduleName, ignoreCase = true) }

    operator fun get(name: String) = getModule(name)

    /**
     * Module related events
     */

    /**
     * Handle incoming key presses
     */
    @EventTarget
    private fun onKey(event: KeyEvent) = modules.forEach { if (it.keyBind == event.key) it.toggle() }

    override fun handleEvents() = true
}
