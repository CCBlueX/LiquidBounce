#version 330

/* #moj_import <minecraft:dynamictransforms.glsl> */
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

/* #moj_import <minecraft:projection.glsl> */
layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec4 vColor;
in vec2 vUv;

out vec4 fragColor;

float sdHeart(vec2 p) {
    p.x = abs(p.x);

    if (p.y + p.x > 1.0)
        return sqrt(dot(p - vec2(0.25, 0.75),
                        p - vec2(0.25, 0.75))) - sqrt(2.0)/4.0;

    return sqrt(min(
        dot(p - vec2(0.00, 1.00), p - vec2(0.00, 1.00)),
        dot(p - 0.5 * max(p.x + p.y, 0.0),
            p - 0.5 * max(p.x + p.y, 0.0))
        )) * sign(p.x - p.y);
}

void main() {
    vec4 color = vColor * ColorModulator;
    if (color.a < 0.001) discard;

    vec2 p = (vUv * 2.0 - 1.0) * vec2(1.08, 1.12);
    p.y -= 0.03;

    float d = sdHeart(p);

    float aa = max(fwidth(d), 1e-4) * 1.5;
    float alpha = smoothstep(0.0, -aa, d);

    color.a *= alpha;

    if (color.a < 0.001) discard;

    fragColor = color;
}
