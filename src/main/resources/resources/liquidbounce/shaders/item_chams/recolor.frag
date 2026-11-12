#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

uniform sampler2D capture;
uniform sampler2D depthTex;

layout(std140) uniform ItemChamsEffectData {
    float time;
    int flags;

    vec4 flowColor;
    float flowSpeed;
    float flowScale;
    float flowSharpness;
    float flowIntensity;
    float flowDirX;
    float flowDirY;

    vec4 trailColor;
    float trailWeight;
    float trailDecay;
    float trailDriftX;
    float trailDriftY;

    vec4 glowColor;
    float glowRadius;
    float glowThickness;
    float glowIntensity;

    vec4 scanColor;
    float scanSpacing;
    float scanThickness;
    float scanSpeed;
    float scanIntensity;
    float scanDirX;
    float scanDirY;

    float chromaStrength;
    int chromaMode;

    float hueShift;
    float hueMix;
    float saturation;
    float brightness;
    float pulseSpeed;
    float pulseAmount;
};

const float COVERAGE_EPSILON = 0.002;
const float DEPTH_EPSILON = 1e-8;

vec3 rgbToHsv(vec3 color) {
    vec4 k = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(color.bg, k.wz), vec4(color.gb, k.xy), step(color.b, color.g));
    vec4 q = mix(vec4(p.xyw, color.r), vec4(color.r, p.yzx), step(p.x, color.r));
    float d = q.x - min(q.w, q.y);
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + 1e-10)), d / (q.x + 1e-10), q.x);
}

vec3 hsvToRgb(vec3 hsv) {
    vec4 k = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(vec3(hsv.x) + k.xyz) * 6.0 - k.www);
    return hsv.z * mix(k.xxx, clamp(p - k.xxx, 0.0, 1.0), hsv.y);
}

void main() {
    vec4 source = textureLod(capture, texCoord, 0.0);
    float depth = textureLod(depthTex, texCoord, 0.0).r;
    float coverage = max(step(DEPTH_EPSILON, depth), step(COVERAGE_EPSILON, source.a));

    if (coverage <= 0.0) {
        fragColor = vec4(0.0);
        return;
    }

    vec3 hsv = rgbToHsv(source.rgb);
    hsv.x = fract(hsv.x + hueShift);
    hsv.y = clamp(hsv.y * saturation, 0.0, 1.0);
    hsv.z = clamp(hsv.z * brightness * (1.0 + pulseAmount * sin(time * pulseSpeed)), 0.0, 1.0);

    fragColor = vec4(hsvToRgb(hsv), coverage * hueMix);
}
