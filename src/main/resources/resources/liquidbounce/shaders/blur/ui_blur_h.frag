#version 330 core

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D texture0;
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
    vec2 texelSize = vec2(1.0) / textureSize(texture0, 0).xy;

    vec4 result = vec4(0.0);

    for (int i = 0; i < kernelRadius * 2 + 1; i++) {
        float offset = float(i - kernelRadius);
        result += texture(texture0, texCoord + vec2(texelSize.x * offset, 0.0)) * getWeight(i);
    }

    fragColor = result;
}
