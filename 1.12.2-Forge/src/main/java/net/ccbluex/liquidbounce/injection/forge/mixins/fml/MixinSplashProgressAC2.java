package net.ccbluex.liquidbounce.injection.forge.mixins.fml;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.injection.utils.EasingObject;
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer;
import net.ccbluex.liquidbounce.utils.AnimationLoader;
import net.ccbluex.liquidbounce.utils.LazySVGRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.common.ProgressManager;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import static org.lwjgl.opengl.GL11.*;

@Mixin(targets = "net/minecraftforge/fml/client/SplashProgress$2")
public abstract class MixinSplashProgressAC2 {
    @Shadow
    @Final
    private int barHeight;
    @Shadow
    private long framecount;

    private final EasingObject[] easingObjects = new EasingObject[3];

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL11;glDisable(I)V", ordinal = 0, shift = At.Shift.AFTER), cancellable = true, require = 1)
    private void run(CallbackInfo ci) {
        glClear(GL_COLOR_BUFFER_BIT);

        // matrix setup
        int w = Display.getWidth();
        int h = Display.getHeight();

        setupMVP(w, h);

        AWTFontRenderer fontRenderer = new AWTFontRenderer(new Font("Consolas", Font.BOLD, 32), 32, 127, true);

        LazySVGRenderer logo;

        try {
            logo = new LazySVGRenderer("/assets/minecraft/liquidbounce/logo.svg");
            // Prerender texture
            logo.getTexture(Display.getWidth() / 2);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        AnimationLoader animationLoader;

        try (InputStream inputStream = LiquidBounce.class.getResourceAsStream("/assets/minecraft/liquidbounce/water_animation.lzma")) {
            animationLoader = new AnimationLoader(inputStream, 6);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        animationLoader.startThread();

        glEnable(GL_TEXTURE_2D);


        //<editor-fold desc="Draw Intro">
        int frames = animationLoader.getImageCount();

        for (int i = 0; i < frames; i++) {
            glClearColor(1.0f, 1.0f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            if (w != (w = Display.getWidth()) || h != (h = Display.getHeight())) {
                setupMVP(w, h);
            }

            int glTextureId;

            try {
                glTextureId = animationLoader.nextFrame().getGlTextureId();
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }

            glBindTexture(GL_TEXTURE_2D, glTextureId);

            glColor4f(0.0f, 111.0f / 255.0f, 1.0f, 1.0f);
            glBegin(GL_QUADS);

            glTexCoord2f(0f, 0f);
            glVertex2d(0, 0);
            glTexCoord2f(0f, 1f);
            glVertex2d(0, h);
            glTexCoord2f(1f, 1f);
            glVertex2d(w, h);
            glTexCoord2f(1f, 0f);
            glVertex2d(w, 0);

            glEnd();

            glBindTexture(GL_TEXTURE_2D, logo.getTexture(w / 2).getGlTextureId());

            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            glBegin(GL_QUADS);


            int x = w / 2 - logo.getCurrentWidth() / 2;
            int y = h / 2 - logo.getCurrentHeight() / 2;

            glTexCoord2f(0f, 0f);
            glVertex2d(x, y);
            glTexCoord2f(0f, 1f);
            glVertex2d(x, y + logo.getCurrentHeight());
            glTexCoord2f(1f, 1f);
            glVertex2d(x + logo.getCurrentWidth(), y + logo.getCurrentHeight());
            glTexCoord2f(1f, 0f);
            glVertex2d(x + logo.getCurrentWidth(), y);

            glEnd();

            Display.update();
            Display.sync(60);
        }

        try {
            animationLoader.stopAndCleanUp();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        //</editor-fold>


//        glDisable(GL_TEXTURE_2D);

        easingObjects[0] = new EasingObject();
        easingObjects[1] = new EasingObject();
        easingObjects[2] = new EasingObject();

        while (!MixinSplashProgress.isDone()) {
            framecount++;
            ProgressManager.ProgressBar first = null, penult = null, last = null;
            Iterator<ProgressManager.ProgressBar> i = ProgressManager.barIterator();
            while (i.hasNext()) {
                if (first == null) first = i.next();
                else {
                    penult = last;
                    last = i.next();
                }
            }

            glClearColor(0.0f, 111.0f / 255.0f, 1.0f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT);

            if (w != (w = Display.getWidth()) || h != (h = Display.getHeight())) {
                setupMVP(w, h);
            }

            glBindTexture(GL_TEXTURE_2D, logo.getTexture(w / 2).getGlTextureId());

            glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            glBegin(GL_QUADS);


            int x = w / 2 - logo.getCurrentWidth() / 2;
            int y = h / 2 - logo.getCurrentHeight() / 2;

            glTexCoord2f(0f, 0f);
            glVertex2d(x, y);
            glTexCoord2f(0f, 1f);
            glVertex2d(x, y + logo.getCurrentHeight());
            glTexCoord2f(1f, 1f);
            glVertex2d(x + logo.getCurrentWidth(), y + logo.getCurrentHeight());
            glTexCoord2f(1f, 0f);
            glVertex2d(x + logo.getCurrentWidth(), y);

            glEnd();

            glDisable(GL_TEXTURE_2D);

            float barWidth = w / 2.0f;
            float barHeight = 20;
            float barOffset = 55.0f;

            // bars
            if (first != null) {
                glPushMatrix();
                glTranslatef(x, y + logo.getCurrentHeight() + ((h / 622.0f) * 40), 0);

                drawBar(first, easingObjects[0], fontRenderer, barWidth, barHeight);
                if (penult != null) {
                    glTranslatef(0, barOffset, 0);
                    drawBar(penult, easingObjects[1], fontRenderer, barWidth, barHeight);
                }
                if (last != null) {
                    glTranslatef(0, barOffset, 0);
                    drawBar(last, easingObjects[2], fontRenderer, barWidth, barHeight);
                }
                glPopMatrix();
            }

            glEnable(GL_TEXTURE_2D);

            // We use mutex to indicate safely to the main thread that we're taking the display global lock
            // So the main thread can skip processing messages while we're updating.
            // There are system setups where this call can pause for a while, because the GL implementation
            // is trying to impose a framerate or other thing is occurring. Without the mutex, the main
            // thread would delay waiting for the same global display lock
            MixinSplashProgress.getMutex().acquireUninterruptibly();

            Display.update();

            MixinSplashProgress.getMutex().release();

            if (MixinSplashProgress.isPaused()) {
                clearGL();
                setGL();
            }

            Display.sync(60);
        }

        fontRenderer.delete();

        clearGL();

        ci.cancel();
    }

    private void drawBar(ProgressManager.ProgressBar b, EasingObject easingObject, AWTFontRenderer fontRenderer, float barWidth, float barHeight) {
        glPushMatrix();
        // title - message
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glScalef(2, 2, 1);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_ALPHA_TEST);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);

        fontRenderer.drawString(b.getTitle() + " - " + b.getMessage(), -3, -3, 0xFFFFFFFF);
        glDisable(GL_TEXTURE_2D);
        glPopMatrix();
        // border
        glPushMatrix();
        glTranslatef(0, fontRenderer.getHeight(), 0);
//        setColor(barBorderColor);

        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        drawBox(barWidth, barHeight);
        // interior
//        setColor(barBackgroundColor);

        glColor4f(0.0f, 111.0f / 255.0f, 1.0f, 1.0f);
        glTranslatef(1, 1, 0);
        drawBox(barWidth - 2, barHeight - 2);
        // slidy part
//        setColor(barColor);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);

        drawBox(Math.min(easingObject.update((barWidth - 2) * (b.getStep() + 1) / (b.getSteps() + 1)), barWidth - 2), barHeight - 2); // Step can sometimes be 0.
        // progress text
//        String progress = b.getTitle() + " - " + b.getMessage();
//        glTranslatef(((float) barWidth - 2) / 2 - fontRenderer.getStringWidth(progress), 2, 0);
//        setColor(fontColor);
        glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        glScalef(2, 2, 1);
        glEnable(GL_TEXTURE_2D);
//        fontRenderer.drawString(progress, 0, -2, 0xFFFFFFFF);
        glPopMatrix();
    }

    private void drawBox(float w, float h) {
        glBegin(GL_QUADS);
        glVertex2f(0, 0);
        glVertex2f(0, h);
        glVertex2f(w, h);
        glVertex2f(w, 0);
        glEnd();
    }

    private void setupMVP(int w, int h) {
        glViewport(0, 0, w, h);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, w, h, 0, -1, 1);
        glMatrixMode(GL_MODELVIEW);
        glLoadIdentity();
    }

    @Shadow
    protected abstract void setGL();

    @Shadow
    protected abstract void clearGL();

}
