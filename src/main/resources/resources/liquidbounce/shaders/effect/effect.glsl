#include <minecraft:globals.glsl>

layout(std140) uniform EffectData {
    vec4 Params0;
    vec4 Params1;
    vec4 Accent;
    float Range;
    float Opacity;
};

// Seconds of game time, wrapping with the day
#define Time (GameTime * 1200.0)

// Distance of pixels farther than Range from every silhouette
const float NONE = 1024.0;

// The field target keeps sqrt(distance / FIELD_LIMIT), which leaves sub-pixel steps near the silhouettes in 8 bits
const float FIELD_LIMIT = 128.0;

// A field texel holds the nearest silhouette pixel: its color as 5-6-5 bits in rg, its alpha in b and the distance
// to it in a

const float TAU = 6.28318530718;

ivec2 pixelAt(vec2 uv, ivec2 size) {
    return clamp(ivec2(uv * vec2(size)), ivec2(0), size - 1);
}

vec3 packSilhouette(vec4 silhouette) {
    // Translucent geometry is blended into the target, which leaves its color premultiplied
    ivec3 color = ivec3(clamp(silhouette.rgb / silhouette.a, 0.0, 1.0) * vec3(31.0, 63.0, 31.0) + 0.5);
    return vec3(vec2((color.r << 3) | (color.g >> 3), ((color.g & 7) << 5) | color.b) / 255.0, silhouette.a);
}

vec3 fieldColor(vec4 texel) {
    ivec2 bits = ivec2(texel.rg * 255.0 + 0.5);
    return vec3(bits.x >> 3, ((bits.x & 7) << 3) | (bits.y >> 5), bits.y & 31) / vec3(31.0, 63.0, 31.0);
}

// Alpha of the nearest silhouette pixel, which geometry fades itself out with
float fieldStrength(vec4 texel) {
    return texel.b;
}

vec4 encodeField(vec3 silhouette, float dist) {
    return dist >= NONE ? vec4(0.0, 0.0, 0.0, 1.0) : vec4(silhouette, min(sqrt(dist / FIELD_LIMIT), 254.0 / 255.0));
}

float fieldDistance(vec4 texel) {
    return texel.a >= 1.0 ? NONE : texel.a * texel.a * FIELD_LIMIT;
}

// Field distances run between pixel centers, the silhouette edge lies half a pixel off the covered ones
float edgeOf(float dist) {
    return dist - 0.5;
}

// Coverage of a line from the silhouette edge out to width
float band(float edge, float width, float softness) {
    float half_soft = max(softness, 0.05) * 0.5;
    return 1.0 - smoothstep(width - half_soft, width + half_soft, edge);
}

// The game blends the outline target onto the scene with straight alpha
vec4 glow(vec3 color, float alpha) {
    return vec4(color, clamp(alpha * Opacity, 0.0, 1.0));
}

vec3 hsv2rgb(vec3 c) {
    vec3 p = abs(fract(c.xxx + vec3(1.0, 2.0 / 3.0, 1.0 / 3.0)) * 6.0 - 3.0);
    return c.z * mix(vec3(1.0), clamp(p - 1.0, 0.0, 1.0), c.y);
}

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
        mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x),
        u.y
    );
}

float hash13(vec3 p) {
    p = fract(p * 0.1031);
    p += dot(p, p.zyx + 31.32);
    return fract((p.x + p.y) * p.z);
}

float valueNoise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(
            mix(hash13(i), hash13(i + vec3(1.0, 0.0, 0.0)), u.x),
            mix(hash13(i + vec3(0.0, 1.0, 0.0)), hash13(i + vec3(1.0, 1.0, 0.0)), u.x),
            u.y
        ),
        mix(
            mix(hash13(i + vec3(0.0, 0.0, 1.0)), hash13(i + vec3(1.0, 0.0, 1.0)), u.x),
            mix(hash13(i + vec3(0.0, 1.0, 1.0)), hash13(i + vec3(1.0, 1.0, 1.0)), u.x),
            u.y
        ),
        u.z
    );
}

float fbm(vec3 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * valueNoise(p);
        p = p * 2.03 + vec3(17.0, 9.0, 13.0);
        amplitude *= 0.5;
    }
    return value;
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    for (int i = 0; i < 4; i++) {
        value += amplitude * valueNoise(p);
        p = p * 2.03 + vec2(17.0, 9.0);
        amplitude *= 0.5;
    }
    return value;
}
