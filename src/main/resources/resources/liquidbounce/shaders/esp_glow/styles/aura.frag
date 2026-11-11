#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:esp_glow/glow.glsl>

#define RADIUS Params0.x
#define HEIGHT Params0.y
#define SWAY Params0.z
#define SCALE Params0.w
#define SPEED Params1.x
#define INTENSITY Params1.y
#define HEAT Params1.z
#define FILL Params1.w

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    ivec2 size = textureSize(InSampler, 0);
    vec2 pixel = texCoord * vec2(size);

    vec4 own = texelFetch(InSampler, pixelAt(texCoord, size), 0);
    if (fieldDistance(own) == 0.0) {
        fragColor = glow(own.rgb, FILL);
        return;
    }

    // Noise stretched upwards and drifting up. Every pixel takes the glow from below it, as far down as the noise
    // says, so the glow rises off the silhouette in tongues.
    vec2 q = vec2(pixel.x / SCALE, pixel.y / (SCALE * 2.0) - Time * SPEED);
    float lift = smoothstep(0.25, 0.75, fbm(q));
    float sway = fbm(q + vec2(5.2, 1.3)) * 2.0 - 1.0;
    vec2 offset = vec2(sway * SWAY, -lift * HEIGHT);

    vec4 field = texelFetch(InSampler, clamp(ivec2(pixel + offset), ivec2(0), size - 1), 0);
    float dist = fieldDistance(field);
    if (dist >= NONE) {
        fragColor = vec4(0.0);
        return;
    }

    float k = 1.0 - clamp(edgeOf(dist) / RADIUS, 0.0, 1.0);
    fragColor = glow(mix(field.rgb, vec3(1.0), HEAT * k * k * k), INTENSITY * k * k);
}
