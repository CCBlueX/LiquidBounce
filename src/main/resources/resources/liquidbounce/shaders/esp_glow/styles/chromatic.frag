#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:esp_glow/glow.glsl>

#define WIDTH Params0.x
#define SPLIT Params0.y
#define SPEED Params0.z
#define TINT Params0.w
#define GLOW_RADIUS Params1.x
#define FILL Params1.y

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

ivec2 size;

vec4 fieldAt(vec2 pixel) {
    return texelFetch(InSampler, clamp(ivec2(pixel), ivec2(0), size - 1), 0);
}

float lineAt(vec2 pixel) {
    float dist = fieldDistance(fieldAt(pixel));
    if (dist <= 0.0 || dist >= NONE) {
        return 0.0;
    }

    float edge = edgeOf(dist);
    float t = max(edge - WIDTH, 0.0) / max(GLOW_RADIUS, 0.001);
    return max(band(edge, WIDTH, 1.0), 0.5 * exp(-3.0 * t * t));
}

void main() {
    size = textureSize(InSampler, 0);
    vec2 pixel = texCoord * vec2(size);
    vec2 offset = vec2(cos(Time * SPEED), sin(Time * SPEED)) * SPLIT;

    vec4 center = fieldAt(pixel);
    float centerDistance = fieldDistance(center);
    if (centerDistance == 0.0) {
        fragColor = glow(center.rgb, FILL);
        return;
    }

    vec3 split = vec3(lineAt(pixel + offset), lineAt(pixel), lineAt(pixel - offset));
    float alpha = max(split.r, max(split.g, split.b));
    vec3 tint = mix(vec3(1.0), centerDistance < NONE ? center.rgb : vec3(1.0), TINT);
    fragColor = glow(split * tint / max(alpha, 0.0001), alpha);
}
