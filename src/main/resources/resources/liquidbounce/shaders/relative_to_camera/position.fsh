#version 330

/* #moj_import <minecraft:dynamictransforms.glsl> */
layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in float alphaFactor;

out vec4 fragColor;

void main() {
    fragColor = vec4(ColorModulator.rgb, ColorModulator.a * alphaFactor);
}
