package net.janrupf.ujr.example.glfw.web;

import net.ccbluex.liquidbounce.web.Layer;
import net.janrupf.ujr.api.UltralightConfigBuilder;
import net.janrupf.ujr.api.UltralightPlatform;
import net.janrupf.ujr.api.UltralightRenderer;
import net.janrupf.ujr.api.javascript.JSClass;
import net.janrupf.ujr.api.javascript.JSGlobalContext;
import net.janrupf.ujr.api.javascript.JSObject;
import net.janrupf.ujr.api.javascript.JavaScriptValueException;
import net.janrupf.ujr.core.UltralightJavaReborn;
import net.janrupf.ujr.core.platform.PlatformEnvironment;
import net.janrupf.ujr.example.glfw.CustomJavaScriptClass;
import net.janrupf.ujr.example.glfw.bridge.FilesystemBridge;
import net.janrupf.ujr.example.glfw.bridge.GlfwClipboardBridge;
import net.janrupf.ujr.example.glfw.bridge.LoggerBridge;
import net.janrupf.ujr.example.glfw.surface.GlfwSurfaceFactory;
import net.minecraft.client.gui.DrawContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

import static net.ccbluex.liquidbounce.utils.client.ClientUtilsKt.getMc;

public class WebController implements AutoCloseable {

    private static final Logger LOGGER = LogManager.getLogger(WebController.class);

    private final Set<WebWindow> windows;
    private final UltralightJavaReborn ujr;

    public WebController() {
        this.windows = new HashSet<>();

        // Load the platform and create the Ultralight Java Reborn instance
        PlatformEnvironment environment = PlatformEnvironment.load();
        LOGGER.info("Platform environment has been loaded!");

        this.ujr = new UltralightJavaReborn(environment);
        this.ujr.activate();
        LOGGER.info("Ultralight Java Reborn has been activated!");

        // Activate global bridge instances for Ultralight
        UltralightPlatform platform = UltralightPlatform.instance();
        platform.usePlatformFontLoader();
        platform.setFilesystem(new FilesystemBridge());
        platform.setClipboard(new GlfwClipboardBridge());
        platform.setLogger(new LoggerBridge());

        // Note the usage of the GlfwSurfaceFactory here.
        // This is required to make Ultralight Java Reborn work custom surfaces.
        // Technically, we could also use the default surface factory, but that would
        // require us to deal with bitmaps and then upload them to OpenGLES.
        // Using a custom surface factory allows us to directly use pixel buffer objects.
        platform.setSurfaceFactory(new GlfwSurfaceFactory());

        //TODO: change
        platform.setConfig(new UltralightConfigBuilder().cachePath(System.getProperty("java.io.tmpdir") + File.separator + "ujr-example-glfw").resourcePathPrefix(FilesystemBridge.RESOURCE_PREFIX).build());

        JSGlobalContext context = new JSGlobalContext((JSClass) null);
        context.setName("TestContext");

        try {
            // Create the custom object and make it available on the global object
            JSObject customObject = context.makeObject(CustomJavaScriptClass.getJavaScriptClass());
            context.getGlobalObject().setProperty("customObject", customObject);

            context.evaluateScript("customObject.test = 42;\n" + "\n" + "new customObject();\n" + "customObject();\n" + "\n" + "delete customObject.test;\n" + "delete customObject;\n", null, null, 1);
        } catch (JavaScriptValueException e) {
            LOGGER.error("Failed to run demo JavaScript code", e);
        }
    }

    public void terminate() {
        LOGGER.debug("Cleaning up Ultralight Java Reborn...");
        ujr.cleanup();
    }

    public WebWindow createWindow(Supplier<Integer> width, Supplier<Integer> height, Layer layer) {
        WebWindow window = new WebWindow(width, height, layer);
        this.windows.add(window);

        return window;
    }

    public void update() {
        for (WebWindow window : this.windows) {
            window.resizeIfNeeded();
        }

        UltralightRenderer.getOrCreate().update();
    }

    public void render() {
        UltralightRenderer.getOrCreate().render();
    }

    public void renderToFramebuffer(DrawContext drawContext, Layer layer) {
        for (final WebWindow window : this.windows) {
            if (window.getLayer() == layer) {
                window.renderToFramebuffer(drawContext, getMc().getWindow().getScaledWidth(), getMc().getWindow().getScaledHeight());
            }
        }
    }

    public void renderToFramebuffer(DrawContext drawContext, WebWindow window) {
        window.renderToFramebuffer(drawContext, getMc().getWindow().getScaledWidth(), getMc().getWindow().getScaledHeight());
    }

    @Override
    public void close() {
        terminate();
    }

    public Set<WebWindow> getWindows() {
        return windows;
    }
}
