#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:esp_glow/glow.glsl>

#define WIDTH Params0.x
#define GLOW_RADIUS Params0.y
#define GLOW_INTENSITY Params0.z
#define FILL Params0.w
#define SPEED Params1.x
#define SPREAD Params1.y
#define SATURATION Params1.z
#define ENTITY_MIX Params1.w

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

    vec2 pixel = texCoord * vec2(size);
    float hue = fract((pixel.x + pixel.y) / SPREAD - Time * SPEED);
    vec3 color = mix(hsv2rgb(vec3(hue, SATURATION, 1.0)), field.rgb, ENTITY_MIX);

    float alpha = FILL;
    if (dist > 0.0) {
        float edge = edgeOf(dist);
        float t = max(edge - WIDTH, 0.0) / max(GLOW_RADIUS, 0.001);
        alpha = max(band(edge, WIDTH, 1.0), GLOW_INTENSITY * exp(-3.0 * t * t));
    }

    fragColor = glow(color, alpha);
}
