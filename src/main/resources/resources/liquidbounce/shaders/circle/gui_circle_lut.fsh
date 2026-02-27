#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 vUv;
flat in ivec2 vUv2;

out vec4 fragColor;

const float TWO_PI = 6.28318530718;
const float INNER_RATIO_SCALE = 32767.0;

void main() {
    vec2 p = vUv * 2.0 - 1.0;
    float dist = length(p);
    float aa = max(fwidth(dist), 1e-4);

    float innerRatio = clamp(float(vUv2.y) / INNER_RATIO_SCALE, 0.0, 1.0);
    // Match rounded_rect AA strategy: one-sided smoothing inside the shape boundary.
    float dOuter = dist - 1.0;
    float outerAlpha = smoothstep(0.0, -aa, dOuter);

    float innerAlpha = 1.0;
    if (innerRatio > 0.0) {
        float dInner = innerRatio - dist;
        innerAlpha = smoothstep(0.0, -aa, dInner);
    }
    float alpha = outerAlpha * innerAlpha;

    if (alpha <= 0.001) {
        discard;
    }

    float angle = atan(p.x, p.y);
    float angle01 = fract(angle / TWO_PI + 1.0);
    vec2 lutSize = vec2(textureSize(Sampler0, 0));
    float row = (float(vUv2.x) + 0.5) / lutSize.y;

    vec4 lutColor = texture(Sampler0, vec2(angle01, row));
    vec4 color = lutColor * vec4(1.0, 1.0, 1.0, alpha) * ColorModulator;

    if (color.a <= 0.001) {
        discard;
    }

    fragColor = color;
}
