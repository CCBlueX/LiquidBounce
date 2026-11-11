#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:effect/effect.glsl>

// Range: how far the rings travel
#define SPACING Params0.x
#define RING_WIDTH Params0.y
#define SPEED Params0.z
#define EDGE_WIDTH Params0.w
#define FILL Params1.x

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 field = texelFetch(InSampler, pixelAt(texCoord, textureSize(InSampler, 0)), 0);
    float dist = fieldDistance(field);

    float alpha = 0.0;
    if (dist == 0.0) {
        alpha = FILL;
    } else if (dist < NONE) {
        float edge = edgeOf(dist);
        float toRing = abs(fract((edge - Time * SPEED) / SPACING) - 0.5) * SPACING;
        float ring = 1.0 - smoothstep(RING_WIDTH * 0.5 - 0.5, RING_WIDTH * 0.5 + 0.5, toRing);
        float fade = 1.0 - clamp(edge / Range, 0.0, 1.0);
        float line = EDGE_WIDTH > 0.0 ? band(edge, EDGE_WIDTH, 1.0) : 0.0;
        alpha = max(line, ring * fade * fade);
    }

    fragColor = glow(fieldColor(field), alpha * fieldStrength(field));
}
