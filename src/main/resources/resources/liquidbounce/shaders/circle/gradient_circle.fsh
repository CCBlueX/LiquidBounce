#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 vUv;
flat in ivec2 vOuterPacked;
flat in ivec2 vInnerPacked;
in float vInnerRatio;

out vec4 fragColor;

vec4 unpackColor(ivec2 packed) {
    int rg = packed.x & 0xFFFF;
    int ba = packed.y & 0xFFFF;
    return vec4(
        float((rg >> 8) & 0xFF),
        float(rg & 0xFF),
        float((ba >> 8) & 0xFF),
        float(ba & 0xFF)
    ) / 255.0;
}

void main() {
    vec2 p = vUv * 2.0 - 1.0;
    float dist = length(p);
    float aa = max(fwidth(dist), 1e-4);

    float dOuter = dist - 1.0;
    float outerAlpha = smoothstep(0.0, -aa, dOuter);

    float innerRatio = clamp(vInnerRatio, 0.0, 1.0);
    float innerAlpha = 1.0;
    if (innerRatio > 0.0) {
        float dInner = innerRatio - dist;
        innerAlpha = smoothstep(0.0, -aa, dInner);
    }

    float coverage = outerAlpha * innerAlpha;
    if (coverage <= 0.001) {
        discard;
    }

    vec4 outerColor = unpackColor(vOuterPacked);
    vec4 innerColor = unpackColor(vInnerPacked);
    float t = clamp((dist - innerRatio) / max(1.0 - innerRatio, 1e-4), 0.0, 1.0);
    vec4 color = mix(innerColor, outerColor, t);
    color = vec4(color.rgb, color.a * coverage) * ColorModulator;

    if (color.a <= 0.001) {
        discard;
    }

    fragColor = color;
}
