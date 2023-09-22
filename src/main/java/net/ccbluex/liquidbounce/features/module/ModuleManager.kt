/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.event.EventManager.registerListener
import net.ccbluex.liquidbounce.event.EventManager.unregisterListener
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.KeyEvent
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.command.CommandManager.registerCommand
import net.ccbluex.liquidbounce.features.module.modules.beta.CoroutineArmorer
import net.ccbluex.liquidbounce.features.module.modules.beta.CoroutineCleaner
import net.ccbluex.liquidbounce.features.module.modules.beta.CoroutineStealer
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
import net.ccbluex.liquidbounce.utils.ClientUtils.displayChatMessage
import java.util.*


object ModuleManager : Listenable {

    val modules = TreeSet<Module> { module1, module2 -> module1.name.compareTo(module2.name) }
    private val moduleClassMap = hashMapOf<Class<*>, Module>()

    private lateinit var inventoryWorker: Job

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
            Ignite::class.java,
            ItemTeleport::class.java,
            Phase::class.java,
            Teleport::class.java,
            TeleportHit::class.java
        )

        // Register modules which have already been instanced (Kotlin objects)
        registerModules(
            AbortBreaking,
            Aimbot,
            AirJump,
            AirLadder,
            Ambience,
            Animations,
            AntiAFK,
            AntiBlind,
            AntiBot,
            AntiCactus,
            AntiHunger,
            AntiFireball,
            AtAllProvider,
            AttackEffects,
            AutoAccount,
            AutoBow,
            AutoBreak,
            AutoClicker,
            AutoFish,
            AutoLeave,
            AutoPot,
            AutoRespawn,
            AutoRod,
            AutoSoup,
            AutoTool,
            AutoWalk,
            AutoWeapon,
            AvoidHazards,
            Backtrack,
            BedGodMode,
            Blink,
            BlockESP,
            BlockOverlay,
            BlockWalk,
            BowAimbot,
            Breadcrumbs,
            BufferSpeed,
            BugUp,
            CameraClip,
            Chams,
            ChestAura,
            ChestStealer,
            CivBreak,
            ClickGUI,
            Clip,
            ComponentOnHover,
            ConsoleSpammer,
            CoroutineArmorer,
            CoroutineCleaner,
            CoroutineStealer,
            Criticals,
            Damage,
            Derp,
            ESP,
            Eagle,
            FastBow,
            FastBreak,
            FastClimb,
            FastPlace,
            FastStairs,
            FastUse,
            Fly,
            ForceUnicodeChat,
            FreeCam,
            Freeze,
            Fucker,
            Fullbright,
            Ghost,
            GhostHand,
            GodMode,
            HUD,
            HighJump,
            HitBox,
            IceSpeed,
            InventoryCleaner,
            InventoryMove,
            ItemESP,
            KeepAlive,
            KeepContainer,
            KeyPearl,
            Kick,
            KillAura,
            LadderJump,
            LiquidChat,
            LiquidWalk,
            Liquids,
            LongJump,
            MidClick,
            MoreCarry,
            MultiActions,
            NameProtect,
            NameTags,
            NoBob,
            NoBooks,
            NoClip,
            NoFOV,
            NoFall,
            NoFluid,
            NoFriends,
            NoHurtCam,
            NoJumpDelay,
            NoPitchLimit,
            NoRotateSet,
            NoScoreboard,
            NoSlow,
            NoSlowBreak,
            NoSwing,
            NoWeb,
            Nuker,
            Parkour,
            PerfectHorseJump,
            PingSpoof,
            Plugins,
            PortalMenu,
            PotionSaver,
            PotionSpoof,
            Projectiles,
            ProphuntESP,
            Reach,
            Refill,
            Regen,
            ResourcePackSpoof,
            ReverseStep,
            Rotations,
            SafeWalk,
            Scaffold,
            ServerCrasher,
            SkinDerp,
            SlimeJump,
            Sneak,
            Spammer,
            Speed,
            Sprint,
            Step,
            StorageESP,
            Strafe,
            SuperKnockback,
            TNTBlock,
            TNTESP,
            Teams,
            Timer,
            Tower,
            Tracers,
            Trigger,
            TrueSight,
            VehicleOneHit,
            Velocity,
            WallClimb,
            WaterSpeed,
            XRay,
            Zoot
        )

        inventoryWorker = CoroutineScope(Dispatchers.Default).launch {
            while (isActive) {
                runCatching {
                    CoroutineArmorer.hasSearched = false

                    // Try to steal stuff from chests
                    CoroutineStealer.execute()

                    // Try to drop and equip armor
                    CoroutineArmorer.execute()

                    // Try to sort and clean inventory
                    CoroutineCleaner.execute()
                }.onFailure {
                    // TODO: Remove when stable
                    displayChatMessage("§cReworked coroutine inventory management has ran into an issue! Please report this: ${it.message}")

                    it.printStackTrace()
                }
            }
        }

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

        registerCommand(ModuleCommand(module, values))
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
