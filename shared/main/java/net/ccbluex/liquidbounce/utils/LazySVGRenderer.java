package net.ccbluex.liquidbounce.utils;

import com.google.common.io.ByteStreams;
import net.minecraft.client.renderer.texture.DynamicTexture;
import org.apache.batik.anim.dom.SVGDOMImplementation;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.TranscodingHints;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.util.SVGConstants;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class LazySVGRenderer {
    private final byte[] svgFile;
    private DynamicTexture currentTexture = null;
    private int currentWidth;
    private int currentHeight;
    private volatile BufferedImage queued;
    private Thread currentThread = null;
    private float aspectRatio = 0.0f;

    public LazySVGRenderer(String resource) throws IOException {
        try (InputStream inputStream = LazySVGRenderer.class.getResourceAsStream(resource)) {
            svgFile = ByteStreams.toByteArray(inputStream);
        }
    }

    private void updateTexture(BufferedImage image) {
        if (currentTexture != null) {
            currentTexture.deleteGlTexture();
        }

        currentTexture = new DynamicTexture(image);
    }


    public DynamicTexture getTexture(int width) {
        if (currentTexture == null || currentWidth != width) {
            runRenderer(width);

            currentWidth = width;
            // Estimate the height
            currentHeight = (int) (aspectRatio * width);
        }

        if (queued != null) {
            updateTexture(queued);
            currentHeight = queued.getHeight();
            aspectRatio = currentHeight / (float) currentWidth;
            queued = null;
        }

        return currentTexture;
    }

    private void runRenderer(int width) {
        if (currentThread != null)
            currentThread.interrupt();

        try {
            if (currentTexture == null) {
                queued = renderSVG(width);
            } else {
                Thread currentThread = new Thread(() -> {
                    try {
                        queued = renderSVG(width);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

                this.currentThread = currentThread;

                currentThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private BufferedImage renderSVG(float width) throws IOException {
        final BufferedImage[] imagePointer = new BufferedImage[1];

        // Rendering hints can't be set programatically, so
        // we override defaults with a temporary stylesheet.
        // These defaults emphasize quality and precision, and
        // are more similar to the defaults of other SVG viewers.
        // SVG documents can still override these defaults.
        String css = "svg {" +
                "shape-rendering: geometricPrecision;" +
                "text-rendering:  geometricPrecision;" +
                "color-rendering: optimizeQuality;" +
                "image-rendering: optimizeQuality;" +
                "}";
        File cssFile = File.createTempFile("batik-default-override-", ".css");

        Files.write(cssFile.toPath(), css.getBytes(StandardCharsets.UTF_8));

        TranscodingHints transcoderHints = new TranscodingHints();
        transcoderHints.put(ImageTranscoder.KEY_XML_PARSER_VALIDATING, Boolean.FALSE);
        transcoderHints.put(ImageTranscoder.KEY_DOM_IMPLEMENTATION,
                SVGDOMImplementation.getDOMImplementation());
        transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT_NAMESPACE_URI,
                SVGConstants.SVG_NAMESPACE_URI);
        transcoderHints.put(ImageTranscoder.KEY_DOCUMENT_ELEMENT, "svg");
        transcoderHints.put(ImageTranscoder.KEY_USER_STYLESHEET_URI, cssFile.toURI().toString());
        transcoderHints.put(ImageTranscoder.KEY_WIDTH, width);

        try {

            TranscoderInput input = new TranscoderInput(new ByteArrayInputStream(svgFile));

            ImageTranscoder t = new ImageTranscoder() {

                @Override
                public BufferedImage createImage(int w, int h) {
                    return new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                }

                @Override
                public void writeImage(BufferedImage image, TranscoderOutput out) throws TranscoderException {
                    imagePointer[0] = image;
                }
            };
            t.setTranscodingHints(transcoderHints);
            t.transcode(input, null);
        } catch (TranscoderException ex) {
            // Requires Java 6
            ex.printStackTrace();
            throw new IOException("Couldn't convert ");
        } finally {
            cssFile.delete();
        }

        return imagePointer[0];
    }

    public void cleanUp() {
        if (this.currentTexture != null) {
            this.currentTexture.deleteGlTexture();
            this.currentTexture = null;
        }
    }

    @Override
    protected void finalize() throws Throwable {
        cleanUp();
    }

    public int getCurrentWidth() {
        return currentWidth;
    }

    public int getCurrentHeight() {
        return currentHeight;
    }
}
