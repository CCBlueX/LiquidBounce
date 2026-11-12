#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:effect/effect.glsl>

#define RADIUS Params0.x
#define FALLOFF Params0.y
#define INTENSITY Params0.z
#define FILL Params0.w

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
        float k = clamp(1.0 - edgeOf(dist) / RADIUS, 0.0, 1.0);
        alpha = INTENSITY * pow(k, FALLOFF);
    }

    fragColor = glow(fieldColor(field), alpha * fieldStrength(field));
}
