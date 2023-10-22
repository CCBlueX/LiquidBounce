package net.ccbluex.liquidbounce.web

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.screen.WebScreen
import net.janrupf.ujr.api.UltralightConfigBuilder
import net.janrupf.ujr.api.UltralightPlatform
import net.janrupf.ujr.api.UltralightRenderer
import net.janrupf.ujr.core.UltralightJavaReborn
import net.janrupf.ujr.core.platform.PlatformEnvironment
import net.janrupf.ujr.example.glfw.bridge.FilesystemBridge
import net.janrupf.ujr.example.glfw.bridge.GlfwClipboardBridge
import net.janrupf.ujr.example.glfw.bridge.LoggerBridge
import net.janrupf.ujr.example.glfw.surface.GlfwSurfaceFactory
import net.janrupf.ujr.example.glfw.web.WebWindow
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.TitleScreen
import java.io.File


object WebController : Listenable, AutoCloseable {

    private var windows = mutableSetOf<WebWindow>()
    private var ujr: UltralightJavaReborn

    init {
        // Load the platform and create the Ultralight Java Reborn instance
        val environment = PlatformEnvironment.load()
        logger.info("Platform environment has been loaded!")
        ujr = UltralightJavaReborn(environment)
        ujr.activate()
        logger.info("Ultralight Java Reborn has been activated!")

        // Activate global bridge instances for Ultralight
        val platform = UltralightPlatform.instance()
        platform.usePlatformFontLoader()
        platform.filesystem = FilesystemBridge()
        platform.clipboard = GlfwClipboardBridge()
        platform.logger = LoggerBridge()

        // Note the usage of the GlfwSurfaceFactory here.
        // This is required to make Ultralight Java Reborn work custom surfaces.
        // Technically, we could also use the default surface factory, but that would
        // require us to deal with bitmaps and then upload them to OpenGLES.
        // Using a custom surface factory allows us to directly use pixel buffer objects.
        platform.setSurfaceFactory(GlfwSurfaceFactory())

        //TODO: change
        platform.setConfig(
            UltralightConfigBuilder().cachePath(System.getProperty("java.io.tmpdir")
                + File.separator + "ujr-example-glfw")
                .resourcePathPrefix(FilesystemBridge.RESOURCE_PREFIX)
                .build()
        )
    }

    val screenTest = handler<ScreenEvent> {
        if (it.screen is TitleScreen) {
            it.cancelEvent()

            val screen = WebScreen(createWindow(LayerDistribution.SCREEN_LAYER).also { window ->
                window.view.loadURL("http://localhost:5173")
            })
            mc.setScreen(screen)
        }
    }

    /**
     * Cleans up the Ultralight Java Reborn instance. This should be called before the application exits.
     * After this the whole web controller becomes unusable!!!
     *
     * @see UltralightJavaReborn.cleanup
     */
    fun terminate() {
        logger.debug("Cleaning up Ultralight Java Reborn...")
        ujr.cleanup()
    }

    /**
     * Creates a new window with the given width, height and layer.
     */
    fun createWindow(layer: LayerDistribution)
        = WebWindow({ mc.window.framebufferWidth }, { mc.window.framebufferHeight }, layer).also { windows += it }

    /**
     * Updates a few things, such as the window size and the renderer.
     */
    fun update() {
        for (window in windows) {
            window.resizeIfNeeded()
        }
        UltralightRenderer.getOrCreate().update()
    }

    /**
     * Render all active views to their respective render-targets/surfaces.
     * It is being called once per frame on the top of the render loop.
     */
    fun render() {
        UltralightRenderer.getOrCreate().render()
    }

    /**
     * Shortcut to render the windows on the in-game layer to the framebuffer.
     */
    fun renderInGame(context: DrawContext?) = renderToFramebuffer(context, LayerDistribution.IN_GAME_LAYER)

    /**
     * Shortcut to render the windows on the splash layer to the framebuffer.
     */
    fun renderOverlay(context: DrawContext?) = renderToFramebuffer(context, LayerDistribution.SPLASH_LAYER)

    /**
     * Shortcut to render the window on the given screen to the framebuffer.
     */
    fun renderScreen(context: DrawContext?, webScreen: WebScreen) = renderToFramebuffer(context, webScreen.window)

    /**
     * Renders the windows on the given layer to the framebuffer.
     */
    private fun renderToFramebuffer(drawContext: DrawContext?, layer: LayerDistribution) {
        for (window in windows) {
            if (window.layer === layer) {
                window.renderToFramebuffer(drawContext, mc.window.scaledWidth, mc.window.scaledHeight)
            }
        }
    }

    /**
     * Renders the given window to the framebuffer.
     */
    private fun renderToFramebuffer(drawContext: DrawContext?, window: WebWindow) {
        window.renderToFramebuffer(drawContext, mc.window.scaledWidth, mc.window.scaledHeight)
    }

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the try-with-resources statement.
     */
    override fun close() {
        terminate()
    }

    /**
     * The tracked cursor position from the last [onCursorPosition] event.
     */
    private var cursorPosition: Pair<Double, Double> = Pair(0.0, 0.0)

    /**
     * Handles the cursor position and forwards it to the windows.
     * The window will then handle the cursor position if it takes input.
     */
    val onCursorPosition = handler<MouseCursorEvent> { event ->
        val xPosition = event.x
        val yPosition = event.y

        for (window in windows) {
            window.onCursorPos(xPosition, yPosition)
        }
        cursorPosition = Pair(xPosition, yPosition)
    }

    /**
     * Handles the mouse button and forwards it to the windows.
     * The window will then handle the mouse button if it takes input.
     */
    val onMouseButton = handler<MouseButtonEvent> { event ->
        val button = event.button
        val action = event.action
        val mods = event.mods

        for (window in windows) {
            window.onMouseButton(button, action, mods)
        }
    }

    /**
     * Handles the mouse scroll and forwards it to the windows.
     * The window will then handle the mouse scroll if it takes input.
     */
    val onMouseScroll = handler<MouseScrollEvent> { event ->
        val xOffset = event.horizontal
        val yOffset = event.vertical

        for (window in windows) {
            window.onScroll(xOffset, yOffset)
        }
    }

    /**
     * Handles the key press and forwards it to the windows.
     * The window will then handle the key press if it takes input.
     */
    val onKeyPress = handler<KeyboardKeyEvent> { event ->
        val key = event.keyCode
        val scancode = event.scancode
        val action = event.action
        val mods = event.mods

        for (window in windows) {
            window.onKey(key, scancode, action, mods)
        }
    }

    /**
     * Handles the key char input and forwards it to the windows.
     * The window will then handle the key char if it takes input.
     */
    val onChar = handler<KeyboardCharEvent> { event ->
        val codepoint = event.codepoint

        for (window in windows) {
            window.onChar(codepoint)
        }
    }

}
