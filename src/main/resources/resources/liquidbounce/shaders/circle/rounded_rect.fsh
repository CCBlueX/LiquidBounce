#version 330

/* #moj_import <minecraft:dynamictransforms.glsl> */
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform u_RoundedRect {
    /** 0..1 (radius in UV) */
    vec2 CornerRadius;
};

in vec4 vColor;
in vec2 vUv;

out vec4 fragColor;

// Default (1,1)
// UV range
vec2 HalfSize = vec2(1.0);

// SDF：d<0 inside
float sdRoundBoxAniso(vec2 p, vec2 b, vec2 r) {
    vec2 rr = max(r, vec2(1e-6));

    vec2 pn = p / rr;
    vec2 bn = b / rr;

    vec2 q = abs(pn) - (bn - vec2(1.0));
    float dn = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - 1.0;

    return dn * min(rr.x, rr.y);
}

void main() {
    vec4 color = vColor * ColorModulator;
    if (color.a < 0.001) discard;

    // -1..1, 0 -> center
    vec2 p = vUv * 2.0 - 1.0;

    vec2 b = HalfSize;
    vec2 r = min(CornerRadius, b);

    float d = sdRoundBoxAniso(p, b, r);

    float aa = fwidth(d);
    float alpha = smoothstep(0.0, -aa, d);

    color.a *= alpha;
    if (color.a < 0.001) discard;

    fragColor = color;
}
