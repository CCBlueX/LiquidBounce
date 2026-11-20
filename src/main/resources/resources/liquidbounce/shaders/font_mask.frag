#version 330
#extension GL_ARB_separate_shader_objects : require

#include <minecraft:dynamictransforms.glsl>

// Widest filter in atlas texels. The texels it reads then lie within 3 texels of the glyph quad,
// which the atlas padding (GlyphPage.DEFAULT_PADDING) keeps clear of the next glyph.
#define MAX_FOOTPRINT 6.0

uniform sampler2D Sampler0;

layout(location = 0) in vec2 texCoord0;
layout(location = 1) in vec4 vertexColor;

layout(location = 0) out vec4 fragColor;

// Glyphs are rasterized larger than they are drawn, so a pixel spans several atlas texels. Average
// every texel under the pixel, weighted by how much of it the pixel covers, which is what a
// rasterizer at the drawn size computes. Below one texel per pixel this is plain bilinear.
float coverage() {
    vec2 atlasSize = vec2(textureSize(Sampler0, 0));
    vec2 dx = dFdx(texCoord0) * atlasSize;
    vec2 dy = dFdy(texCoord0) * atlasSize;
    vec2 footprint = clamp(abs(dx) + abs(dy), vec2(1.0), vec2(MAX_FOOTPRINT));

    vec2 center = texCoord0 * atlasSize;
    vec2 lo = center - footprint * 0.5;
    vec2 hi = center + footprint * 0.5;
    ivec2 first = ivec2(floor(lo));
    ivec2 last = ivec2(ceil(hi)) - 1;
    ivec2 maxTexel = ivec2(atlasSize) - 1;

    float sum = 0.0;
    for (int y = first.y; y <= last.y; y++) {
        float row = 0.0;
        for (int x = first.x; x <= last.x; x++) {
            float weight = min(hi.x, float(x + 1)) - max(lo.x, float(x));
            row += weight * texelFetch(Sampler0, clamp(ivec2(x, y), ivec2(0), maxTexel), 0).r;
        }
        sum += row * (min(hi.y, float(y + 1)) - max(lo.y, float(y)));
    }

    return sum / (footprint.x * footprint.y);
}

#ifdef TEXT_CONTRAST
float srgbToLinear(float c) {
    return c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4);
}

float linearToSrgb(float c) {
    return c <= 0.0031308 ? c * 12.92 : 1.055 * pow(c, 1.0 / 2.4) - 0.055;
}

// Skia's mask gamma (SkMaskGamma.cpp): the coverage that, blended as usual, gives what blending in
// linear light would against a background guessed as the opposite luminance of the text, plus a
// contrast boost that fades out for bright text.
float maskGamma(float coverage, vec3 color) {
    // SkComputeLuminance, cut to the 3 bits Skia keys its tables by. With 7 steps src never equals dst.
    float src = floor(dot(round(color * 255.0), vec3(54.0, 183.0, 19.0)) / 8192.0) / 7.0;
    float dst = 1.0 - src;
    float linDst = srgbToLinear(dst);

    float srca = coverage + (1.0 - coverage) * TEXT_CONTRAST * linDst * coverage;
    float blended = linearToSrgb(srgbToLinear(src) * srca + (1.0 - srca) * linDst);
    return (blended - dst) / (src - dst);
}
#endif

void main() {
    float alpha = coverage();
#ifdef TEXT_CONTRAST
    alpha = maskGamma(alpha, vertexColor.rgb);
#endif
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * alpha);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
