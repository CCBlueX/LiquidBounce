package net.ccbluex.liquidbounce.utils.render;

import net.ccbluex.liquidbounce.utils.lzma.LZMA.Decoder;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.DynamicTexture;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

public class AnimationLoader implements Runnable {
    private final DynamicTexture[] textures;

    private final byte[] data;
    private final int width;
    private final int height;
    private final int imageCount;
    private final AtomicInteger decoderIndex = new AtomicInteger();
    private final AtomicInteger rendererIndex = new AtomicInteger();
    private final Thread thread;
    private final Object _rendererLock = new Object();
    private final Object _decoderLock = new Object();
    private int dataIndex = 2 * 3;
    private int bitIndex = 0;

    public AnimationLoader(InputStream inputStream, int cachedImages) throws IOException {
        this.thread = new Thread(this);
        this.textures = new DynamicTexture[cachedImages];
        this.data = Decoder.decode(inputStream);

        this.width = (data[0] & 0xFF) | (data[1] << 8);
        this.height = (data[2] & 0xFF) | (data[3] << 8);
        this.imageCount = (data[4] & 0xFF) | (data[5] << 8);

        for (int i = 0; i < cachedImages; i++) {
            this.textures[i] = new DynamicTexture(width, height);
        }
    }

    public void startThread() {
        thread.start();
    }


    public void stopAndCleanUp() throws InterruptedException {
        thread.interrupt();
        thread.join();

        this.cleanUp();
    }

    private void cleanUp() {
        for (DynamicTexture texture : textures) {
            if (texture != null)
                texture.deleteGlTexture();
        }

        Arrays.fill(textures, null);
    }

    /**
     * Returns the next frame in the animation
     *
     * @return The Texture of the current frame
     */
    public AbstractTexture nextFrame() throws InterruptedException {
        // Wait for the thread, if it can't keep up
        while (this.decoderIndex.get() <= this.rendererIndex.get()) {
            synchronized (_rendererLock) {
                _rendererLock.wait(50);
            }
        }

        int andIncrement = this.rendererIndex.getAndIncrement();

        int index = andIncrement % textures.length;

        // Notify the decoder thread, that a new frame was rendered
        synchronized (_decoderLock) {
            _decoderLock.notifyAll();
        }

        DynamicTexture texture = textures[index];

        texture.updateDynamicTexture();

        return texture;
    }

    @Override
    public void run() {
        while (!Thread.interrupted() && this.imageCount > this.decoderIndex.get()) {
            DynamicTexture texture = textures[decoderIndex.get() % textures.length];
            int[] outputArray = texture.getTextureData();

            // Decoding
            for (int i = 0; i < outputArray.length; i++) {
                outputArray[i] = (this.data[dataIndex] & (1 << bitIndex)) != 0 ? 0xFFFFFFFF : 0;

                if (++bitIndex >= 8) {
                    dataIndex++;
                    bitIndex = 0;
                }
            }

            this.decoderIndex.incrementAndGet(); // ++this.decoderIndex

            // If the renderer thread is waiting for the decoder to keep up, wake it up
            synchronized (_rendererLock) {
                _rendererLock.notifyAll();
            }

            // Wait for the render thread to keep up
            try {
                while (this.decoderIndex.get() >= this.rendererIndex.get() + textures.length - 1) {
                    synchronized (_decoderLock) {
                        _decoderLock.wait(50);
                    }
                }
            } catch (InterruptedException e) {
                break;
            }

        }
    }

    @Override
    protected void finalize() throws Throwable {
        this.stopAndCleanUp();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getImageCount() {
        return imageCount;
    }
}
