package net.ccbluex.liquidbounce.features.module.modules.client

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientLanguageChangedEvent
import net.ccbluex.liquidbounce.event.events.GameRenderEvent
import net.ccbluex.liquidbounce.event.events.HudLayoutEditorValueChangeEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.VirtualDisplayScreen
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.backend.browser.Browser
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.client.gui.screen.Screen
import org.lwjgl.glfw.GLFW

object ModuleHudEditor :
    ClientModule("HudLayoutEditor", Category.CLIENT, bind = GLFW.GLFW_KEY_RIGHT_ALT, disableActivation = true) {

    override val running = true



    @Suppress("unused")
    private val StartEditor by boolean("StartEditor", false).onChanged { cache ->
        RenderSystem.recordRenderCall {
            if (cache) {
                createView()
            } else {
                close()
            }

            if (mc.currentScreen is VirtualDisplayScreen || mc.currentScreen is ClickScreen) {
                onEnabled()
            }
        }
    }


    object Snapping : ToggleableConfigurable(this, "Snapping", true) {

        @Suppress("unused")
        private val gridSize by int("GridSize", 10, 1..100, "px").onChanged {
            EventManager.callEvent(HudLayoutEditorValueChangeEvent(ModuleHudEditor))
        }

        init {
            inner.find { it.name == "Enabled" }?.onChanged {
                EventManager.callEvent(HudLayoutEditorValueChangeEvent(ModuleHudEditor))
            }
        }
    }

    private var clickGuiBrowser: Browser? = null
    private const val WORLD_CHANGE_SECONDS_UNTIL_RELOAD = 5

    init {
        tree(Snapping)
    }

    override fun onEnabled() {
        // Pretty sure we are not in a game, so we can't open the clickgui
        if (!inGame) {
            return
        }

        mc.setScreen(
            if (clickGuiBrowser == null) {
                VirtualDisplayScreen(VirtualScreenType.LAYOUT_EDITOR)
            } else {
                ClickScreen()
            }
        )
        super.onEnabled()
    }

    private fun createView() {
        if (clickGuiBrowser != null) {
            return
        }

        clickGuiBrowser = ThemeManager.openInputAwareImmediate(
            VirtualScreenType.LAYOUT_EDITOR,
            true,
            priority = 20,
            settings = IntegrationListener.browserSettings
        ) {
            mc.currentScreen is ClickScreen
        }
    }

    private fun close() {
        clickGuiBrowser?.close()
        clickGuiBrowser = null
    }

    fun reload(restart: Boolean = false) {
        if (restart) {
            close()
            createView()
            return
        }
        clickGuiBrowser?.reload()
    }

    @Suppress("unused")
    private val gameRenderHandler = handler<GameRenderEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING) {
        clickGuiBrowser?.visible = mc.currentScreen is ClickScreen
        }

    @Suppress("unused")
    private val worldChangeHandler = sequenceHandler<WorldChangeEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
    ) { event ->
        if (event.world == null) {
            return@sequenceHandler
        }

        waitSeconds(WORLD_CHANGE_SECONDS_UNTIL_RELOAD)
        if (mc.currentScreen !is ClickScreen) {
            reload()
        }
    }

    @Suppress("unused")
    private val clientLanguageChangedHandler = handler<ClientLanguageChangedEvent> {
        if (mc.currentScreen !is ClickScreen) {
            reload()
        }
    }

    /**
     * An empty screen that acts as hint when to draw the clickgui
     */
    class ClickScreen : Screen("HUDEditor".asText()) {

        override fun close() {
            mc.mouse.lockCursor()
            super.close()
        }

        override fun shouldPause(): Boolean {
            // preventing game pause
            return false
        }
    }

}
