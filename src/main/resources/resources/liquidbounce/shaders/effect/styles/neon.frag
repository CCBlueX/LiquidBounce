#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:effect/effect.glsl>

// Accent: core color
#define CORE_WIDTH Params0.x
#define BLOOM_RADIUS Params0.y
#define BLOOM_INTENSITY Params0.z
#define FILL Params0.w
#define CORE_MIX Params1.x
#define PULSE_SPEED Params1.y
#define PULSE_AMOUNT Params1.z

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    vec4 field = texelFetch(InSampler, pixelAt(texCoord, textureSize(InSampler, 0)), 0);
    float dist = fieldDistance(field);
    if (dist >= NONE) {
        fragColor = vec4(0.0);
        return;
    }

    float pulse = 1.0 - PULSE_AMOUNT * (0.5 + 0.5 * sin(Time * PULSE_SPEED * TAU));
    if (dist == 0.0) {
        fragColor = glow(fieldColor(field), FILL * pulse * fieldStrength(field));
        return;
    }

    float edge = edgeOf(dist);
    float core = band(edge, CORE_WIDTH, 1.0);
    float bloom = clamp(BLOOM_INTENSITY * pulse * exp(-2.5 * edge / BLOOM_RADIUS), 0.0, 1.0) * (1.0 - core);

    float alpha = core + bloom;
    vec3 base = fieldColor(field);
    vec3 color = (mix(base, Accent.rgb, CORE_MIX) * core + base * bloom) / max(alpha, 0.0001);
    fragColor = glow(color, alpha * fieldStrength(field));
}
