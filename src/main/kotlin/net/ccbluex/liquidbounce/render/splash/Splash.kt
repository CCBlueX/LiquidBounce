/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.render.splash

import com.mojang.blaze3d.systems.RenderSystem
import kotlinx.coroutines.runBlocking
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.injection.mixins.minecraft.gui.MixinLoadingOverlayAccessor
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.render.withPush
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.math.approach
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundSource
import net.minecraft.util.Util
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * Plays the active theme's splash screen through the loading overlay, the browser choice and the startup
 * tasks into the menu.
 *
 * The splash is read off the render thread from the active theme. Themes load long after the first loading
 * overlay, so the folder of the active theme is kept for the next start. Until the splash is ready only the
 * background shows; the loading itself never waits for it. A theme without a splash, or with one that breaks,
 * gets Minecraft's own loading screen.
 */
object Splash {

    private const val MAX_FRAME_MILLIS = 100L
    private const val PROGRESS_RATE = 6f
    private const val SMOOTH_FRAMES = 8
    private const val SMOOTH_FRAME_MILLIS = 40L
    private const val MAX_HOLD_MILLIS = 4000L

    /** A splash that takes longer than this to draw for [SLOW_FRAMES] frames in a row is dropped. */
    private const val SLOW_FRAME_NANOS = 20_000_000L
    private const val SLOW_FRAMES = 30

    private val INCLUDED_THEME = ResourceSplashFiles("/resources/liquidbounce/themes/liquidbounce/")

    private class Loaded(val bundle: SplashBundle, val sounds: SplashSoundPlayer) : AutoCloseable {
        override fun close() {
            bundle.close()
            sounds.close()
        }
    }

    private class Active(val loaded: Loaded) : AutoCloseable {
        val textures: Map<String, SplashTexture>
        val renderer: LottieRenderer
        val playSound: (String) -> Unit = { name ->
            loaded.sounds.play(name, Minecraft.getInstance().options.getFinalSoundSourceVolume(SoundSource.MASTER))
        }

        init {
            val created = LinkedHashMap<String, SplashTexture>()
            try {
                loaded.bundle.images.forEach { (id, image) -> created[id] = SplashTexture(id, image) }
            } catch (e: Exception) {
                created.values.forEach(SplashTexture::close)
                throw e
            }
            textures = created
            renderer = LottieRenderer(loaded.bundle.animation, created.mapValues { (_, texture) -> texture.setup })
            // The textures hold the images now
            loaded.bundle.close()
        }

        /** Draws [frame] contained in the screen and centered. */
        fun draw(graphics: GuiGraphicsExtractor, frame: Float, progress: Float, alpha: Float) {
            val animation = loaded.bundle.animation
            val width = graphics.guiWidth().toFloat()
            val height = graphics.guiHeight().toFloat()
            val scale = minOf(width / animation.width, height / animation.height)
            graphics.pose().withPush {
                translate((width - animation.width * scale) / 2f, (height - animation.height * scale) / 2f)
                scale(scale)
                renderer.progress = progress
                renderer.render(graphics, frame, alpha)
            }
        }

        /** Closes once the GPU is done with frames that may still draw it. */
        override fun close() = RenderSystem.queueFencedTask {
            textures.values.forEach(SplashTexture::close)
            loaded.close()
        }
    }

    private var loading: CompletableFuture<Loaded?>? = null
    private var active: Active? = null
    @Volatile
    private var stale = false

    /** The active theme's files once themes are loaded; before that, the saved theme folder is used. */
    @Volatile
    private var themeFiles: SplashFiles? = null
    private var slowFrames = 0

    private var timeline: SplashTimeline? = null
    private var requested: SplashTimeline.Phase? = null
    private var hurried = false
    private var lastFrame = -1L

    /** When the timeline was created, and how many frames in a row came in time since. */
    private var createdAt = -1L
    private var smoothFrames = 0

    private var progress = 0f

    private val settings get() = active?.loaded?.bundle?.settings

    /** The phase on screen, null before the splash is ready. */
    val phase get() = timeline?.phase

    /** True once it is clear there is no splash to draw, so Minecraft's own loading screen shows. */
    @JvmStatic
    val isVanilla get() = active == null && loading?.let { it.isDone && it.getNow(null) == null } == true

    val background
        get() = settings?.background ?: when {
            isVanilla -> MixinLoadingOverlayAccessor.getBrandBackground().asInt
            else -> SplashSettings.DEFAULT_BACKGROUND
        }
    val textColor get() = settings?.text ?: -1
    val accentColor get() = settings?.accent ?: SplashSettings.DEFAULT_ACCENT

    /** GUI x of the `content` layer's anchor after the last [render]; NaN without one. */
    val contentX get() = active?.renderer?.content?.x ?: Float.NaN

    /** GUI y of the `content` layer's anchor after the last [render]; NaN without one. */
    val contentY get() = active?.renderer?.content?.y ?: Float.NaN

    private val gameDirectory get() = Minecraft.getInstance().gameDirectory
    private val savedTheme get() = File(gameDirectory, "LiquidBounce/splash-theme.txt")

    /**
     * Starts reading the splash in the background.
     */
    fun prepare() {
        if (loading == null) {
            loading = CompletableFuture.supplyAsync(::load, Util.backgroundExecutor()).exceptionally {
                logger.error("Failed to read the splash screen", it)
                null
            }
        }
    }

    /**
     * Starts over with the intro, for a new loading overlay.
     */
    fun begin() {
        if (stale) {
            stale = false
            release()
        }
        prepare()

        timeline = null
        requested = null
        hurried = false
        progress = 0f
        SplashProgress.startup = LiquidBounce.taskManager?.isCompleted != true
    }

    /**
     * True once the splash has played into [phase], so the client's text can come in; also without a splash.
     */
    fun isSettled(phase: SplashTimeline.Phase): Boolean {
        val timeline = timeline ?: return active == null && loading?.isDone != false
        return timeline.isSettled(phase)
    }

    fun request(phase: SplashTimeline.Phase) {
        requested = phase
        timeline?.request(phase)
    }

    fun hurry() {
        hurried = true
        timeline?.hurry()
    }

    /**
     * Takes the splash of [theme] from the next [begin] on, and from the next start until themes are loaded.
     */
    fun useTheme(theme: Theme?) {
        val folder = theme?.folder
        themeFiles = when {
            theme == null || theme.origin == Theme.Origin.RESOURCE -> INCLUDED_THEME
            folder != null -> DirectorySplashFiles(folder)
            else -> SplashFiles { path -> runBlocking { runCatching { theme.readFile(path) }.getOrNull() } }
        }

        runCatching {
            if (folder != null) {
                savedTheme.writeText(folder.toRelativeString(gameDirectory))
            } else {
                savedTheme.delete()
            }
        }.onFailure { logger.error("Failed to save the splash screen's theme", it) }
        stale = true
    }

    /**
     * Draws the current frame over the whole screen.
     *
     * @param progress the loading progress, or null to keep the last one
     * @param alpha fades the whole splash
     */
    fun render(graphics: GuiGraphicsExtractor, progress: Float?, alpha: Float) {
        val active = activate() ?: return
        val now = Util.getMillis()
        val timeline = timeline ?: SplashTimeline(active.loaded.bundle.animation).also {
            timeline = it
            lastFrame = now
            createdAt = now
            smoothFrames = 0
            requested?.let(it::request)
            if (hurried) it.hurry()
        }

        val delta = now - lastFrame
        lastFrame = now
        // The first frames of a start stall for seconds while Minecraft sets up; the intro would stutter and its
        // sounds come late, so it holds its first frame until frames come smoothly
        if (smoothFrames < SMOOTH_FRAMES) {
            smoothFrames = if (delta <= SMOOTH_FRAME_MILLIS) smoothFrames + 1 else 0
        }
        val smooth = smoothFrames >= SMOOTH_FRAMES || now - createdAt >= MAX_HOLD_MILLIS
        val millis = if (smooth) delta.coerceIn(0L, MAX_FRAME_MILLIS).toFloat() else 0f
        // The bar only ever grows during one splash; SplashProgress leaves room for what follows loading
        if (progress != null) {
            this.progress = this.progress.approach(maxOf(progress, this.progress), millis, PROGRESS_RATE)
        }

        val started = System.nanoTime()
        try {
            timeline.advance(millis, active.playSound)
            active.draw(graphics, timeline.drawFrame, this.progress, alpha)
        } catch (e: Exception) {
            fail("Failed to draw the splash screen", e)
            return
        }

        slowFrames = if (System.nanoTime() - started > SLOW_FRAME_NANOS) slowFrames + 1 else 0
        if (slowFrames >= SLOW_FRAMES) {
            fail("The splash screen draws too slowly", null)
        }
    }

    private fun activate(): Active? {
        active?.let { return it }
        // getNow is null while reading too
        val loaded = loading?.getNow(null) ?: return null
        return try {
            Active(loaded).also { active = it }
        } catch (e: Exception) {
            fail("Failed to set up the splash screen", e)
            null
        }
    }

    /**
     * Lets go of the splash in use or on its way, closing what it holds.
     */
    private fun release() {
        val active = active
        if (active != null) {
            active.close()
        } else {
            loading?.thenAccept { loaded -> loaded?.close() }
        }
        this.active = null
        loading = null
        timeline = null
    }

    /**
     * Drops a broken splash for Minecraft's own loading screen, until the theme changes.
     */
    private fun fail(message: String, error: Throwable?) {
        logger.error(message, error)
        release()
        slowFrames = 0
        loading = CompletableFuture.completedFuture(null)
    }

    private fun load(): Loaded? {
        val files = themeFiles ?: savedTheme.takeIf(File::isFile)?.readText()?.trim()
            ?.let { path -> DirectorySplashFiles(File(gameDirectory, path)) }
            ?: INCLUDED_THEME
        val bundle = loadBundle(files) ?: return null

        return Loaded(bundle, SplashSoundPlayer(bundle.sounds))
    }

}

private fun loadBundle(files: SplashFiles): SplashBundle? = runCatching {
    SplashBundle.load(files)?.also { bundle ->
        if (bundle.animation.unsupported.isNotEmpty()) {
            logger.warn("The splash screen uses what is not supported: ${bundle.animation.unsupported}")
        }
    }
}.onFailure {
    logger.error("Failed to read the splash screen", it)
}.getOrNull()
