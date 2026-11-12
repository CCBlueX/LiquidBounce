#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:effect/effect.glsl>

// Accent: stripe color
#define SPACING Params0.x
#define DIRECTION Params0.yz
#define SPEED Params0.w
#define STRIPES Params1.x
#define FILL Params1.y
#define EDGE_WIDTH Params1.z
#define FLICKER Params1.w

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    ivec2 size = textureSize(InSampler, 0);
    vec4 field = texelFetch(InSampler, pixelAt(texCoord, size), 0);
    float dist = fieldDistance(field);
    if (dist >= NONE) {
        fragColor = vec4(0.0);
        return;
    }

    float flicker = 1.0 - FLICKER * hash12(vec2(floor(Time * 12.0), 7.0));

    if (dist == 0.0) {
        vec2 pixel = texCoord * vec2(size);
        float wave = 0.5 + 0.5 * sin((dot(pixel, DIRECTION) / SPACING - Time * SPEED) * TAU);
        float stripe = pow(wave, 6.0) * STRIPES;
        fragColor = glow(mix(fieldColor(field), Accent.rgb, stripe), (FILL + stripe) * flicker * fieldStrength(field));
        return;
    }

    float edge = edgeOf(dist);
    float alpha = max(band(edge, EDGE_WIDTH, 1.0), 0.35 * exp(-edge / 2.0));
    fragColor = glow(fieldColor(field), alpha * flicker * fieldStrength(field));
}
