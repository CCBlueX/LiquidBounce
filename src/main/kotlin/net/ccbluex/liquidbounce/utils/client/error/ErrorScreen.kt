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
package net.ccbluex.liquidbounce.utils.client.error

import com.mojang.blaze3d.Blaze3D
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.splash.Splash
import net.ccbluex.liquidbounce.render.splash.SplashPhaseScreen
import net.ccbluex.liquidbounce.render.splash.SplashTimeline
import net.ccbluex.liquidbounce.render.splash.wrapText
import net.ccbluex.liquidbounce.utils.client.browseUrl
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.multiplayer.ClientLevel
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

/**
 * Shows a fatal error in the look of the splash screen instead of the game. Reporting it uploads the log to
 * logs.liquidbounce.net first and opens the bug report on GitHub filled in.
 */
class ErrorScreen private constructor(
    private val report: ErrorReport,
    private val quickFix: String?,
    private val needToReport: Boolean,
) : SplashPhaseScreen(
    translation("liquidbounce.splash.error.title", LiquidBounce.CLIENT_NAME),
    SplashTimeline.Phase.ERROR,
) {

    private enum class Action { REPORT, UPLOAD, FOLDER, QUIT }

    private val actions = if (needToReport) Action.entries else Action.entries - Action.REPORT
    private val drawn = CountDownLatch(1)

    /** Set while the report waits for the log upload. */
    private var reporting = false
    private var copied = false

    private var lines = emptyList<String>()
    private var labels = emptyList<String>()
    private var labelsState = -1

    override fun init() {
        // An error in game starts the splash over, quickly
        if (Splash.phase == SplashTimeline.Phase.OUTRO) {
            Splash.begin()
            Splash.hurry()
        }
        super.init()

        // The error itself always fits; the hint and the quick fix give way
        val maxWidth = width * 0.8f
        val hint = translation(
            if (needToReport) "liquidbounce.splash.error.hintReport" else "liquidbounce.splash.error.hint"
        ).string
        val error = translation("liquidbounce.splash.error.message", report.summary).string
        val errorLines = wrapText(error, maxWidth, MESSAGE_SCALE).take(MAX_ERROR_LINES)
        val introLines = wrapText(listOfNotNull(hint, quickFix).joinToString("\n\n"), maxWidth, MESSAGE_SCALE)
            .take(MAX_LINES - errorLines.size - 1)
        lines = introLines + "" + errorLines
    }

    override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTick: Float) {
        // Client events stop while this screen is up, the one that loads new glyphs too
        if (FontManager.isLoaded) {
            FontManager.glyphManager.updateDynamicGlyphs()
        }
        extractSplash(graphics, null)
        drawn.countDown()
        followUpload()

        val alpha = ((age - 200f) / 400f).coerceIn(0f, 1f)
        val blockHeight = TITLE_HEIGHT + (lines.size + 2) * MESSAGE_HEIGHT + GAP + actions.size * ACTION_HEIGHT
        var y = contentTop.coerceAtMost(height - blockHeight - MARGIN).coerceAtLeast(MARGIN)

        extractCenteredText(graphics, title.string, y, alpha)
        y += TITLE_HEIGHT
        for (line in lines) {
            extractCenteredText(graphics, line, y, alpha * 0.6f, MESSAGE_SCALE)
            y += MESSAGE_HEIGHT
        }
        y += MESSAGE_HEIGHT
        extractCenteredText(graphics, report.environment, y, alpha * 0.35f, MESSAGE_SCALE)
        extractOptions(graphics, labels(), y + MESSAGE_HEIGHT + GAP, alpha, mouseX, mouseY)
    }

    /** The actions' labels, built again only when the report or the upload moves on. */
    private fun labels(): List<String> {
        val log = report.log
        val logState = when {
            log == null -> 0
            !log.isDone -> 1
            log.isCompletedExceptionally -> 2
            else -> 3
        }
        val state = logState * 2 + if (reporting) 1 else 0
        if (state != labelsState) {
            labelsState = state
            labels = actions.map(::label)
        }
        return labels
    }

    private fun label(action: Action): String {
        val log = report.log
        val key = when (action) {
            Action.REPORT -> if (reporting) "uploading" else "report"
            Action.UPLOAD -> when {
                log == null -> "upload"
                !log.isDone -> "uploading"
                log.isCompletedExceptionally -> "uploadFailed"
                else -> return translation(
                    "liquidbounce.splash.error.openLog",
                    log.join().removePrefix("https://"),
                ).string
            }

            Action.FOLDER -> "openFolder"
            Action.QUIT -> "quit"
        }
        return translation("liquidbounce.splash.error.$key").string
    }

    /**
     * Copies the uploaded log's link and opens the report that waited for it.
     */
    private fun followUpload() {
        val log = report.log?.takeIf { it.isDone } ?: return
        val url = if (log.isCompletedExceptionally) null else log.join()
        if (url != null && !copied) {
            copied = true
            mc.keyboardHandler.clipboard = url
        }

        if (reporting) {
            reporting = false
            if (url == null) {
                openLogsFolder()
            }
            browseUrl(report.issueUrl(url))
        }
    }

    override fun pickOption(index: Int) {
        when (actions[index]) {
            Action.REPORT -> {
                // The report always comes with the log
                reporting = true
                report.uploadLog()
            }

            Action.UPLOAD -> {
                val log = report.log
                if (log != null && log.isDone && !log.isCompletedExceptionally) {
                    browseUrl(log.join())
                } else {
                    copied = false
                    report.uploadLog()
                }
            }

            Action.FOLDER -> openLogsFolder()
            Action.QUIT -> exitProcess(1)
        }
    }

    private fun openLogsFolder() = Blaze3D.openPath(mc.gameDirectory.toPath().resolve("logs"))

    override fun isPauseScreen() = true

    /**
     * Thrown on the render thread to leave whatever ran into the error; the game loop catches it in
     * [recover] and goes on with the error screen.
     */
    private object Shown : Error("The error screen is shown", null, false, false)

    companion object {

        private const val TITLE_HEIGHT = 16f
        private const val MESSAGE_HEIGHT = 9f
        private const val MESSAGE_SCALE = 0.85f
        private const val ACTION_HEIGHT = 14f
        private const val GAP = 10f
        private const val MARGIN = 8f
        private const val MAX_LINES = 14
        private const val MAX_ERROR_LINES = 4
        private const val DRAW_TIMEOUT_SECONDS = 10L

        @Volatile
        private var current: ErrorScreen? = null

        @Volatile
        private var fallback: (() -> Nothing)? = null

        @Volatile
        private var loopRunning = false

        @JvmStatic
        val isShown get() = current != null

        /**
         * True when [screen] must not replace the error screen.
         */
        @JvmStatic
        fun keeps(screen: Any?) = current.let { it != null && it !== screen }

        /**
         * Shows the error instead of the game and never returns, unless the game loop is not running yet
         * or the screen does not come up in time.
         *
         * @param fallback reports the error without the screen, if drawing it fails
         */
        internal fun takeOver(
            report: ErrorReport,
            quickFix: String?,
            needToReport: Boolean,
            fallback: () -> Nothing,
        ) {
            if (!loopRunning) {
                return
            }

            val screen = synchronized(this) {
                current ?: ErrorScreen(report, quickFix, needToReport).also {
                    this.fallback = fallback
                    current = it
                }
            }

            if (mc.isSameThread) {
                show(screen)
                throw Shown
            }

            mc.execute { show(screen) }
            if (!screen.drawn.await(DRAW_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                return
            }
            while (true) {
                Thread.sleep(Long.MAX_VALUE)
            }
        }

        /** Puts the screen up; if even that fails, the error goes to the system dialog. */
        private fun show(screen: ErrorScreen) {
            try {
                if (mc.gui.screen() !== screen) {
                    mc.gui.setOverlay(null)
                    mc.gui.setScreen(screen)
                    // Queued, so it never runs in the middle of the frame that failed
                    mc.schedule { leaveWorld(screen) }
                }
            } catch (e: Exception) {
                logger.error("Failed to show the error screen", e)
                fallback?.invoke()
            }
        }

        /**
         * Disconnects from the server, or saves and closes the singleplayer world, so the game does not go on
         * behind the screen.
         */
        private fun leaveWorld(screen: ErrorScreen) {
            val level = mc.level ?: return
            try {
                level.disconnect(ClientLevel.DEFAULT_QUIT_MESSAGE)
                // Keeps the server's resource packs, so no reload starts behind the screen
                mc.disconnect(screen, true)
            } catch (e: Exception) {
                logger.error("Failed to leave the world behind the error screen", e)
            }
        }

        /**
         * Called by the game loop with what a frame threw. Returns when the error screen goes on.
         */
        @JvmStatic
        fun recover(throwable: Throwable) {
            // Minecraft wraps what a screen or tick throws in a crash report
            if (generateSequence(throwable, Throwable::cause).any { it === Shown }) {
                releaseSurface()
                return
            }

            val fallback = fallback ?: throw throwable
            logger.error("The game crashed behind the error screen", throwable)
            fallback()
        }

        /**
         * Minecraft skips every frame while the window surface of one that was left early is acquired.
         */
        private fun releaseSurface() {
            val surface = mc.windowSurface()
            val target = mc.gameRenderer.mainRenderTarget().colorTextureView
            if (!surface.isAcquired || target == null) {
                return
            }

            val encoder = RenderSystem.getDevice().createCommandEncoder()
            surface.blitFromTexture(encoder, target)
            encoder.submit()
            surface.present()
        }

        @JvmStatic
        fun markLoopRunning() {
            loopRunning = true
        }

    }

}
