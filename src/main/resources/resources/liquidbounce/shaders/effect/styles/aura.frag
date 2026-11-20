#version 330
#extension GL_ARB_separate_shader_objects : require

#include <liquidbounce:effect/effect.glsl>

#define RADIUS Params0.x
#define HEIGHT Params0.y
#define SWAY Params0.z
#define SCALE Params0.w
#define SPEED Params1.x
#define INTENSITY Params1.y
#define HEAT Params1.z
#define FILL Params1.w

// World axes of the camera, and in Lens.xy the tangents of half its horizontal and vertical field of view
layout(std140) uniform EffectView {
    vec4 Right;
    vec4 Up;
    vec4 Forward;
    vec4 Lens;
};

// Blocks after which the noise repeats, keeps its coordinates small
const int WRAP = 256;

// Screen heights a block of camera movement moves the noise, right for a silhouette about 4 blocks away
const float PARALLAX = 0.18;

uniform sampler2D InSampler;

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

void main() {
    ivec2 size = textureSize(InSampler, 0);
    vec2 pixel = texCoord * vec2(size);

    vec4 own = texelFetch(InSampler, pixelAt(texCoord, size), 0);
    if (fieldDistance(own) == 0.0) {
        fragColor = glow(fieldColor(own), FILL * fieldStrength(own));
        return;
    }

    // Noise stretched upwards and drifting up. Every pixel takes the glow from below it, as far down as the noise
    // says, so the glow rises off the silhouette in tongues. The noise lies in the world, on a sphere around the
    // camera that is one focal length wide in pixels, so turning and walking leave it where the silhouette is instead
    // of dragging it over the screen.
    vec2 ndc = texCoord * 2.0 - 1.0;
    vec3 ray = normalize(Forward.xyz + Right.xyz * (ndc.x * Lens.x) + Up.xyz * (ndc.y * Lens.y));
    vec3 camera = vec3(CameraBlockPos & (WRAP - 1)) - CameraOffset;
    vec3 q = (ray * (0.5 / Lens.y) + camera * PARALLAX) * float(size.y) / SCALE;
    q.y = q.y * 0.5 - Time * SPEED;
    float lift = smoothstep(0.25, 0.75, fbm(q));
    float sway = fbm(q + vec3(5.2, 1.3, 2.8)) * 2.0 - 1.0;
    vec2 offset = vec2(sway * SWAY, -lift * HEIGHT);

    vec4 field = texelFetch(InSampler, clamp(ivec2(pixel + offset), ivec2(0), size - 1), 0);
    float dist = fieldDistance(field);
    if (dist >= NONE) {
        fragColor = vec4(0.0);
        return;
    }

    float k = 1.0 - clamp(edgeOf(dist) / RADIUS, 0.0, 1.0);
    fragColor = glow(mix(fieldColor(field), vec3(1.0), HEAT * k * k * k), INTENSITY * k * k * fieldStrength(field));
}
