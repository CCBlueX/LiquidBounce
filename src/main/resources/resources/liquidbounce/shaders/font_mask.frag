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

void main() {
    float alpha = coverage();
    vec4 color = vec4(vertexColor.rgb, vertexColor.a * alpha);
    if (color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
