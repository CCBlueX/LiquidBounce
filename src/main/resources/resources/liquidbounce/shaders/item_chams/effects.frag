#version 330
#extension GL_ARB_separate_shader_objects : require

layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;

uniform sampler2D capture;
uniform sampler2D depthTex;
uniform sampler2D trail;

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

const int FLAG_FLOW = 1;
const int FLAG_TRAIL = 2;
const int FLAG_GLOW = 4;
const int FLAG_CHROMATIC = 8;
const int FLAG_SCANLINES = 16;

const int CHROMA_GHOST = 1;
const float COVERAGE_EPSILON = 0.002;
const float DEPTH_EPSILON = 1e-8;

float coverageAt(vec2 uv) {
    float depth = textureLod(depthTex, uv, 0.0).r;
    float alpha = textureLod(capture, uv, 0.0).a;
    return max(step(DEPTH_EPSILON, depth), step(COVERAGE_EPSILON, alpha));
}

void main() {
    vec2 uv = texCoord;
    vec2 texel = 1.0 / vec2(textureSize(capture, 0));
    vec4 source = textureLod(capture, uv, 0.0);
    float coverage = coverageAt(uv);
    vec2 flowDir = vec2(flowDirX, flowDirY);
    vec2 scanDir = vec2(scanDirX, scanDirY);
    vec3 color = vec3(0.0);

    if ((flags & FLAG_FLOW) != 0 && coverage > 0.0) {
        float phase = dot(uv, flowDir) * flowScale - time * flowSpeed;
        float band = pow(max(sin(phase * 3.14159265), 0.0), max(1.0, flowSharpness));
        color += flowColor.rgb * (band * coverage * flowIntensity);
    }

    if ((flags & FLAG_TRAIL) != 0) {
        vec4 history = textureLod(trail, uv, 0.0);
        color += trailColor.rgb * history.rgb * history.a * trailWeight;
    }

    if ((flags & FLAG_GLOW) != 0) {
        float ring = 0.0;
        int rings = int(clamp(glowRadius, 1.0, 32.0));
        for (int i = 1; i <= rings; i++) {
            float radius = float(i);
            float weight = max(0.0, 1.0 - abs(radius - glowRadius) / max(0.2, glowThickness));
            if (weight <= 0.0) {
                continue;
            }

            for (int j = 0; j < 8; j++) {
                float angle = float(j) * 0.7853981634;
                vec2 offset = vec2(cos(angle), sin(angle)) * radius;
                ring += coverageAt(uv + offset * texel) * weight;
            }
        }

        color += glowColor.rgb * (ring * glowIntensity * (1.0 - coverage));
    }

    if ((flags & FLAG_CHROMATIC) != 0) {
        vec2 offset = normalize(flowDir + vec2(0.0001, 0.0001)) * chromaStrength * texel;

        if (chromaMode == CHROMA_GHOST) {
            color += textureLod(capture, uv - offset, 0.0).rgb * 0.5;
        } else {
            vec3 shifted = vec3(
                textureLod(capture, uv + offset, 0.0).r,
                source.g,
                textureLod(capture, uv - offset, 0.0).b
            );
            color += max(shifted - source.rgb, vec3(0.0)) * 2.0;
        }
    }

    if ((flags & FLAG_SCANLINES) != 0 && coverage > 0.0) {
        float phase = dot(uv * vec2(textureSize(capture, 0)), scanDir) / max(1.0, scanSpacing) - time * scanSpeed;
        float fraction = fract(phase);
        float line = 1.0 - smoothstep(0.0, max(0.01, scanThickness), min(fraction, 1.0 - fraction));
        color += scanColor.rgb * (line * coverage * scanIntensity);
    }

    fragColor = vec4(color, 0.0);
}
