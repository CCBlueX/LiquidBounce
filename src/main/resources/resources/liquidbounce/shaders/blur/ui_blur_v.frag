#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D texture0;
uniform sampler2D overlay;
layout(std140) uniform BlurData {
    float alphaBlendMin;
    float alphaBlendMax;
};

layout(std140) uniform BlurKernelData {
    vec4 weightVecs[23];
    int kernelRadius;
};

float getWeight(int idx) {
    vec4 v = weightVecs[idx / 4];
    int comp = idx - (idx / 4) * 4;
    if (comp == 0) return v.x;
    if (comp == 1) return v.y;
    if (comp == 2) return v.z;
    return v.w;
}

void main() {
    vec4 overlayColor = texture(overlay, texCoord);

    // Almost transparent -> skip blur
    if (overlayColor.a <= 0.01) {
        fragColor = vec4(0.0);
        return;
    }

    float a = overlayColor.a;
    float range = alphaBlendMax - alphaBlendMin;
    float opacity = range > 0.0
        ? clamp((a - alphaBlendMin) / range, 0.0, 1.0)
        : 1.0;
    opacity = clamp(opacity, 0.1, 1.0);

    vec2 texelSize = vec2(1.0) / textureSize(texture0, 0).xy;

    vec4 blurred = vec4(0.0);

    for (int i = 0; i < kernelRadius * 2 + 1; i++) {
        float offset = float(i - kernelRadius);
        blurred += texture(texture0, texCoord + vec2(0.0, texelSize.y * offset)) * getWeight(i);
    }

    vec3 blurResult = blurred.rgb;
    fragColor = vec4(blurResult, opacity);
}
