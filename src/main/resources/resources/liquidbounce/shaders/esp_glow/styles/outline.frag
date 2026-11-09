#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:esp_glow/glow.glsl>

#define WIDTH Params0.x
#define SOFTNESS Params0.y
#define GLOW_RADIUS Params0.z
#define GLOW_INTENSITY Params0.w
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
        float glowAlpha = 0.0;
        if (GLOW_RADIUS > 0.0) {
            float t = max(edge - WIDTH, 0.0) / GLOW_RADIUS;
            glowAlpha = GLOW_INTENSITY * exp(-3.0 * t * t);
        }
        alpha = max(band(edge, WIDTH, SOFTNESS), glowAlpha);
    }

    fragColor = glow(field.rgb, alpha);
}
