#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:esp_glow/glow.glsl>

// Range: how far the dots reach
#define CELL Params0.x
#define DOT_SCALE Params0.y
#define FILL Params0.z
#define EDGE_WIDTH Params0.w

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

// Halftone screens sit at 45 degrees
const mat2 SCREEN = mat2(0.70710678, -0.70710678, 0.70710678, 0.70710678);

void main() {
    ivec2 size = textureSize(InSampler, 0);
    vec2 pixel = texCoord * vec2(size);

    vec2 screen = SCREEN * pixel;
    vec2 cellCenter = (floor(screen / CELL) + 0.5) * CELL;
    vec4 cell = texelFetch(InSampler, clamp(ivec2(transpose(SCREEN) * cellCenter), ivec2(0), size - 1), 0);
    vec4 own = texelFetch(InSampler, pixelAt(texCoord, size), 0);
    float cellDistance = fieldDistance(cell);
    float ownDistance = fieldDistance(own);

    float strength = 0.0;
    if (cellDistance == 0.0) {
        strength = FILL;
    } else if (cellDistance < NONE) {
        float k = 1.0 - clamp(edgeOf(cellDistance) / Range, 0.0, 1.0);
        strength = k * k;
    }

    float radius = 0.5 * CELL * DOT_SCALE * sqrt(strength);
    float dotCoverage = 1.0 - smoothstep(radius - 0.6, radius + 0.6, length(screen - cellCenter));
    bool outside = ownDistance > 0.0 && ownDistance < NONE;
    float line = outside && EDGE_WIDTH > 0.0 ? band(edgeOf(ownDistance), EDGE_WIDTH, 1.0) : 0.0;

    float alpha = max(dotCoverage * step(0.001, strength), line);
    fragColor = glow(cellDistance < NONE ? cell.rgb : own.rgb, alpha);
}
