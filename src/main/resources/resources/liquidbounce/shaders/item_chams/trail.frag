#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

uniform sampler2D capture;
uniform sampler2D depthTex;
uniform sampler2D history;

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

void main() {
    vec2 uv = texCoord;
    vec2 texel = 1.0 / vec2(textureSize(capture, 0));
    vec4 source = textureLod(capture, uv, 0.0);
    float depth = textureLod(depthTex, uv, 0.0).r;
    float coverage = max(step(DEPTH_EPSILON, depth), step(COVERAGE_EPSILON, source.a));

    vec2 drift = vec2(trailDriftX, trailDriftY) * texel;
    vec4 previous = textureLod(history, uv + drift, 0.0);

    fragColor = max(previous * trailDecay, vec4(source.rgb, coverage));
}
