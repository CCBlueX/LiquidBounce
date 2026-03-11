#version 330

/* #moj_import <minecraft:dynamictransforms.glsl> */
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform u_RoundedRect {
    /** 0..1 (radius in UV[-1,1] space) */
    vec2 CornerRadius;
    /** 0 = fill, > 0 = stroke width in pixels */
    float StrokeWidth;
};

in vec4 vColor;
in vec2 vUv;

out vec4 fragColor;

// quad local half-size in p-space (p is in [-1,1])
const vec2 HalfSize = vec2(1.0);

// d < 0 inside
float sdRoundBoxAniso(vec2 p, vec2 b, vec2 r) {
    vec2 rr = max(r, vec2(1e-6));

    vec2 pn = p / rr;
    vec2 bn = b / rr;

    vec2 q = abs(pn) - (bn - vec2(1.0));
    float dn = length(max(q, 0.0)) + min(max(q.x, q.y), 0.0) - 1.0;

    // scale back to p-space distance
    return dn * min(rr.x, rr.y);
}

void main() {
    vec4 color = vColor * ColorModulator;
    if (color.a < 0.001) discard;

    // p in [-1,1], center at 0
    vec2 p = vUv * 2.0 - 1.0;

    vec2 b = HalfSize;
    vec2 r = min(CornerRadius, b);

    float d = sdRoundBoxAniso(p, b, r);

    float alpha;
    if (StrokeWidth > 0.0) {
        float dd = fwidth(d);                  // d-space per pixel
        float w  = StrokeWidth * dd;           // full width in d-space
        float aa = fwidth(d);                  // AA width

        // 1) inside test near edge (d <= 0)
        float edgeIn = smoothstep(0.0, -aa, d);          // 1 inside, 0 outside

        // 2) cut off deeper than -w (keep only d >= -w)
        float band   = smoothstep(-w - aa, -w + aa, d);  // 0 deep inside, 1 near edge

        alpha = edgeIn * band;
    } else {
        float aa = fwidth(d);
        alpha = smoothstep(0.0, -aa, d);
    }

    color.a *= alpha;
    if (color.a < 0.001) discard;

    fragColor = color;
}
