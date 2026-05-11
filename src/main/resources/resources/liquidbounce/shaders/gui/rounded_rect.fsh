#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 vUv;
in vec4 vColor;
flat in ivec2 vSize;
flat in ivec2 vParameters;
flat in float vStrokeWidth;

out vec4 fragColor;

float sdRoundBox(vec2 p, vec2 halfSize, float radius) {
    float r = min(radius, min(halfSize.x, halfSize.y));
    vec2 q = abs(p) - halfSize + vec2(r);
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - r;
}

void main() {
    vec2 size = max(vec2(vSize), vec2(1.0));
    vec2 halfSize = size * 0.5;
    vec2 p = (vUv - vec2(0.5)) * size;
    float radius = max(float(vParameters.x), 0.0);

    float d = sdRoundBox(p, halfSize, radius);
    float aa = max(fwidth(d), 1e-4);
    float alpha;

    if (vStrokeWidth > 0.0) {
        float edge = smoothstep(0.0, -aa, d);
        float inner = smoothstep(-vStrokeWidth - aa, -vStrokeWidth + aa, d);
        alpha = edge * inner;
    } else {
        alpha = smoothstep(0.0, -aa, d);
    }

    vec4 color = vColor * ColorModulator;
    color.a *= alpha;
    if (color.a <= 0.001) {
        discard;
    }

    fragColor = color;
}
